#!/bin/sh
# E2E Test Script for Quarkus CRUD API
# Tests health, auth, and CRUD operations using curl
# Uses BASE_URL environment variable (default: http://localhost:8080)

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
FAILED=0
TOTAL=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo "${RED}[ERROR]${NC} $1"
}

log_test() {
    echo "${YELLOW}[TEST]${NC} $1"
}

assert_status() {
    expected=$1
    actual=$2
    test_name=$3
    
    TOTAL=$((TOTAL + 1))
    if [ "$actual" -eq "$expected" ]; then
        log_info "✓ Test passed: $test_name (status: $actual)"
    else
        log_error "✗ Test failed: $test_name (expected: $expected, got: $actual)"
        FAILED=$((FAILED + 1))
    fi
}

assert_contains() {
    text=$1
    substring=$2
    test_name=$3
    
    TOTAL=$((TOTAL + 1))
    if echo "$text" | grep -q "$substring"; then
        log_info "✓ Test passed: $test_name"
    else
        log_error "✗ Test failed: $test_name (expected to find: $substring)"
        FAILED=$((FAILED + 1))
    fi
}

# Create temporary files for responses
TMP_DIR=$(mktemp -d)
trap "rm -rf $TMP_DIR" EXIT

log_info "Starting E2E tests against $BASE_URL"
log_info "Temporary directory: $TMP_DIR"

# Test 1: Health Check
log_test "Test 1: Health check endpoint"
status=$(curl -s -o "$TMP_DIR/health.json" -w "%{http_code}" "$BASE_URL/healthz")
assert_status 200 "$status" "Health check returns 200"
response=$(cat "$TMP_DIR/health.json")
assert_contains "$response" "UP" "Health check response contains UP"

# Test 2: Create Guest User
log_test "Test 2: Create guest user and get JWT token"
curl -s -D "$TMP_DIR/headers.txt" -o "$TMP_DIR/guest.json" "$BASE_URL/api/auth/guest" -X POST
status=$(grep "HTTP" "$TMP_DIR/headers.txt" | tail -1 | sed 's/.*HTTP\/[0-9.]\+ \([0-9]\+\).*/\1/')
assert_status 200 "$status" "Create guest user returns 200"

# Extract JWT token from Authorization header
TOKEN=$(grep -i "^authorization:" "$TMP_DIR/headers.txt" | sed 's/.*Bearer \(.*\)/\1/' | tr -d '\r\n ')
if [ -z "$TOKEN" ]; then
    log_error "Failed to extract JWT token from response headers"
    FAILED=$((FAILED + 1))
else
    log_info "JWT token extracted successfully: ${TOKEN:0:20}..."
fi

response=$(cat "$TMP_DIR/guest.json")
assert_contains "$response" "id" "Guest user response contains id"
assert_contains "$response" "createdAt" "Guest user response contains createdAt"

# Test 3: Get Current User (with token)
log_test "Test 3: Get current user with valid token"
status=$(curl -s -o "$TMP_DIR/me.json" -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/auth/me")
assert_status 200 "$status" "Get current user returns 200"
response=$(cat "$TMP_DIR/me.json")
assert_contains "$response" "id" "Current user response contains id"

# Test 4: Get Current User (without token - should fail)
log_test "Test 4: Get current user without token (should fail with 401)"
status=$(curl -s -o "$TMP_DIR/me_fail.json" -w "%{http_code}" "$BASE_URL/api/auth/me")
assert_status 401 "$status" "Get current user without token returns 401"

# Test 5: List all rooms (no auth required)
log_test "Test 5: List all rooms (no auth required)"
status=$(curl -s -o "$TMP_DIR/rooms.json" -w "%{http_code}" "$BASE_URL/api/rooms")
assert_status 200 "$status" "List all rooms returns 200"

# Test 6: Create a room (with auth)
log_test "Test 6: Create a room with authentication"
status=$(curl -s -o "$TMP_DIR/room_created.json" -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"name":"E2E Test Room","description":"Created by E2E test"}' \
    "$BASE_URL/api/rooms")
assert_status 201 "$status" "Create room returns 201"
response=$(cat "$TMP_DIR/room_created.json")
assert_contains "$response" "E2E Test Room" "Created room has correct name"
assert_contains "$response" "id" "Created room has id"

# Extract room ID
ROOM_ID=$(echo "$response" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
if [ -z "$ROOM_ID" ]; then
    log_error "Failed to extract room ID from response"
    FAILED=$((FAILED + 1))
else
    log_info "Created room with ID: $ROOM_ID"
fi

# Test 7: Create room without auth (should fail)
log_test "Test 7: Create room without authentication (should fail with 401)"
status=$(curl -s -o "$TMP_DIR/room_fail.json" -w "%{http_code}" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"name":"Unauthorized Room"}' \
    "$BASE_URL/api/rooms")
assert_status 401 "$status" "Create room without auth returns 401"

# Test 8: Get room by ID
if [ -n "$ROOM_ID" ]; then
    log_test "Test 8: Get room by ID"
    status=$(curl -s -o "$TMP_DIR/room_get.json" -w "%{http_code}" "$BASE_URL/api/rooms/$ROOM_ID")
    assert_status 200 "$status" "Get room by ID returns 200"
    response=$(cat "$TMP_DIR/room_get.json")
    assert_contains "$response" "E2E Test Room" "Retrieved room has correct name"
fi

# Test 9: Update room
if [ -n "$ROOM_ID" ]; then
    log_test "Test 9: Update room"
    status=$(curl -s -o "$TMP_DIR/room_updated.json" -w "%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -X PUT \
        -d '{"name":"Updated E2E Room","description":"Updated by E2E test"}' \
        "$BASE_URL/api/rooms/$ROOM_ID")
    assert_status 200 "$status" "Update room returns 200"
    response=$(cat "$TMP_DIR/room_updated.json")
    assert_contains "$response" "Updated E2E Room" "Updated room has new name"
fi

# Test 10: Get my rooms
log_test "Test 10: Get my rooms (authenticated)"
status=$(curl -s -o "$TMP_DIR/my_rooms.json" -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/api/rooms/my")
assert_status 200 "$status" "Get my rooms returns 200"
response=$(cat "$TMP_DIR/my_rooms.json")
assert_contains "$response" "Updated E2E Room" "My rooms includes the created room"

# Test 11: Get non-existent room (should fail)
log_test "Test 11: Get non-existent room (should fail with 404)"
status=$(curl -s -o "$TMP_DIR/room_404.json" -w "%{http_code}" "$BASE_URL/api/rooms/999999")
assert_status 404 "$status" "Get non-existent room returns 404"

# Test 12: Update room without auth (should fail)
if [ -n "$ROOM_ID" ]; then
    log_test "Test 12: Update room without authentication (should fail with 401)"
    status=$(curl -s -o "$TMP_DIR/room_update_fail.json" -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -X PUT \
        -d '{"name":"Unauthorized Update"}' \
        "$BASE_URL/api/rooms/$ROOM_ID")
    assert_status 401 "$status" "Update room without auth returns 401"
fi

# Test 13: Create second user and try to update first user's room (should fail with 403)
log_test "Test 13: Create second user and try to update first user's room (should fail with 403)"
curl -s -D "$TMP_DIR/headers2.txt" -o "$TMP_DIR/guest2.json" "$BASE_URL/api/auth/guest" -X POST
TOKEN2=$(grep -i "^authorization:" "$TMP_DIR/headers2.txt" | sed 's/.*Bearer \(.*\)/\1/' | tr -d '\r\n ')
if [ -n "$TOKEN2" ] && [ -n "$ROOM_ID" ]; then
    status=$(curl -s -o "$TMP_DIR/room_forbidden.json" -w "%{http_code}" \
        -H "Authorization: Bearer $TOKEN2" \
        -H "Content-Type: application/json" \
        -X PUT \
        -d '{"name":"Forbidden Update"}' \
        "$BASE_URL/api/rooms/$ROOM_ID")
    assert_status 403 "$status" "Update another user's room returns 403"
fi

# Test 14: Delete room
if [ -n "$ROOM_ID" ]; then
    log_test "Test 14: Delete room"
    status=$(curl -s -o "$TMP_DIR/room_deleted.txt" -w "%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        -X DELETE \
        "$BASE_URL/api/rooms/$ROOM_ID")
    assert_status 204 "$status" "Delete room returns 204"
fi

# Test 15: Verify room is deleted
if [ -n "$ROOM_ID" ]; then
    log_test "Test 15: Verify room is deleted (should return 404)"
    status=$(curl -s -o "$TMP_DIR/room_deleted_check.json" -w "%{http_code}" "$BASE_URL/api/rooms/$ROOM_ID")
    assert_status 404 "$status" "Get deleted room returns 404"
fi

# Test 16: Delete room without auth (should fail)
# First create a room to delete
log_test "Test 16: Delete room without authentication (should fail with 401)"
status=$(curl -s -o "$TMP_DIR/room_for_delete.json" -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -X POST \
    -d '{"name":"Room to delete"}' \
    "$BASE_URL/api/rooms")
if [ "$status" -eq 201 ]; then
    response=$(cat "$TMP_DIR/room_for_delete.json")
    DELETE_ROOM_ID=$(echo "$response" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
    
    # Try to delete without auth
    status=$(curl -s -o "$TMP_DIR/room_delete_fail.json" -w "%{http_code}" \
        -X DELETE \
        "$BASE_URL/api/rooms/$DELETE_ROOM_ID")
    assert_status 401 "$status" "Delete room without auth returns 401"
    
    # Clean up - delete with proper auth
    curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        -X DELETE \
        "$BASE_URL/api/rooms/$DELETE_ROOM_ID"
fi

# Summary
echo ""
log_info "========================================="
log_info "E2E Test Results"
log_info "========================================="
log_info "Total tests: $TOTAL"
log_info "Passed: $((TOTAL - FAILED))"
log_info "Failed: $FAILED"
log_info "========================================="

if [ $FAILED -gt 0 ]; then
    log_error "E2E tests FAILED"
    exit 1
else
    log_info "All E2E tests PASSED ✓"
    exit 0
fi
