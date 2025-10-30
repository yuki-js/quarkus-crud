#!/bin/bash

# Integration Test Script for Quarkus CRUD Application
# This script tests the entire application stack including:
# - PostgreSQL database
# - MyBatis mappers
# - Flyway migrations
# - REST API endpoints
# - Authentication flow
# - Room CRUD operations
# - Authorization checks

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
COOKIE_FILE="/tmp/quarkus-crud-cookies.txt"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Function to print test result
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASSED${NC}: $2"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC}: $2"
        ((TESTS_FAILED++))
    fi
    ((TESTS_RUN++))
}

# Function to test HTTP response
test_http() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local data=$4
    local description=$5
    local cookie_option=""
    
    if [ "$6" == "with_cookie" ]; then
        cookie_option="-b $COOKIE_FILE"
    fi
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -c $COOKIE_FILE $cookie_option -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -c $COOKIE_FILE $cookie_option -X $method "$BASE_URL$endpoint")
    fi
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$status_code" == "$expected_status" ]; then
        print_result 0 "$description"
        echo "$body"
    else
        print_result 1 "$description (expected $expected_status, got $status_code)"
        echo "$body"
    fi
    
    echo ""
}

echo "========================================="
echo "Quarkus CRUD Integration Test Suite"
echo "========================================="
echo "Base URL: $BASE_URL"
echo ""

# Clean up previous test data
rm -f $COOKIE_FILE

echo "--- Authentication Tests ---"
echo ""

# Test 1: Create guest user
echo "Test 1: Create guest user"
test_http POST "/api/auth/guest" 200 "" "Create guest user"

# Test 2: Get current user info
echo "Test 2: Get current user with cookie"
test_http GET "/api/auth/me" 200 "" "Get current user info" "with_cookie"

# Test 3: Get current user without cookie
rm -f $COOKIE_FILE
echo "Test 3: Get current user without authentication"
test_http GET "/api/auth/me" 401 "" "Get current user without cookie"

# Create a new guest user for room tests
echo "Creating new guest user for room tests..."
test_http POST "/api/auth/guest" 200 "" "Create guest user for room tests"

echo "--- Room CRUD Tests ---"
echo ""

# Test 4: Create room without authentication
rm -f $COOKIE_FILE
echo "Test 4: Create room without authentication"
test_http POST "/api/rooms" 401 '{"name":"Unauthorized Room","description":"Should fail"}' "Create room without auth"

# Create authenticated user
test_http POST "/api/auth/guest" 200 "" "Create authenticated user"

# Test 5: Create room
echo "Test 5: Create room with authentication"
room_response=$(curl -s -c $COOKIE_FILE -b $COOKIE_FILE -X POST "$BASE_URL/api/rooms" \
    -H "Content-Type: application/json" \
    -d '{"name":"Test Room","description":"Integration test room"}')
echo "$room_response"
room_id=$(echo "$room_response" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ -n "$room_id" ]; then
    print_result 0 "Create room and extract ID: $room_id"
else
    print_result 1 "Create room and extract ID"
fi
echo ""

# Test 6: Get all rooms
echo "Test 6: Get all rooms"
test_http GET "/api/rooms" 200 "" "Get all rooms"

# Test 7: Get room by ID
echo "Test 7: Get room by ID"
test_http GET "/api/rooms/$room_id" 200 "" "Get room by ID"

# Test 8: Get my rooms
echo "Test 8: Get my rooms"
test_http GET "/api/rooms/my" 200 "" "Get my rooms" "with_cookie"

# Test 9: Update room
echo "Test 9: Update room"
test_http PUT "/api/rooms/$room_id" 200 '{"name":"Updated Room","description":"Updated description"}' "Update room" "with_cookie"

# Test 10: Create another user and try to update first user's room
echo "Test 10: Authorization - try to update another user's room"
rm -f $COOKIE_FILE
test_http POST "/api/auth/guest" 200 "" "Create second user"
test_http PUT "/api/rooms/$room_id" 403 '{"name":"Hacked Room","description":"Should fail"}' "Try to update another user's room" "with_cookie"

# Test 11: Create another user and try to delete first user's room
echo "Test 11: Authorization - try to delete another user's room"
test_http DELETE "/api/rooms/$room_id" 403 "" "Try to delete another user's room" "with_cookie"

echo "--- Multi-Room Tests ---"
echo ""

# Create a new user for multi-room tests
rm -f $COOKIE_FILE
test_http POST "/api/auth/guest" 200 "" "Create user for multi-room tests"

# Test 12-14: Create multiple rooms
echo "Test 12: Create first room"
test_http POST "/api/rooms" 201 '{"name":"Living Room","description":"First room"}' "Create living room" "with_cookie"

echo "Test 13: Create second room"
test_http POST "/api/rooms" 201 '{"name":"Bedroom","description":"Second room"}' "Create bedroom" "with_cookie"

echo "Test 14: Create third room"
test_http POST "/api/rooms" 201 '{"name":"Kitchen","description":"Third room"}' "Create kitchen" "with_cookie"

# Test 15: Verify my rooms count
echo "Test 15: Verify user has multiple rooms"
my_rooms=$(curl -s -b $COOKIE_FILE "$BASE_URL/api/rooms/my")
echo "$my_rooms"
room_count=$(echo "$my_rooms" | grep -o '"id":[0-9]*' | wc -l)
if [ "$room_count" -ge 3 ]; then
    print_result 0 "User has $room_count rooms"
else
    print_result 1 "User should have at least 3 rooms, got $room_count"
fi
echo ""

echo "--- Database Integrity Tests ---"
echo ""

# Test 16: Test special characters in room name
echo "Test 16: Create room with special characters"
test_http POST "/api/rooms" 201 '{"name":"Room @#$% with & special chars","description":"Testing special chars"}' "Create room with special characters" "with_cookie"

# Test 17: Test null description
echo "Test 17: Update room with null description"
test_http PUT "/api/rooms/$room_id" 200 '{"name":"No Description Room","description":null}' "Update room with null description" "with_cookie"

echo ""
echo "========================================="
echo "Test Summary"
echo "========================================="
echo "Tests run: $TESTS_RUN"
echo -e "${GREEN}Tests passed: $TESTS_PASSED${NC}"
if [ $TESTS_FAILED -gt 0 ]; then
    echo -e "${RED}Tests failed: $TESTS_FAILED${NC}"
else
    echo -e "${GREEN}Tests failed: $TESTS_FAILED${NC}"
fi
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi
