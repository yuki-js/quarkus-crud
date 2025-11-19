#!/usr/bin/env ts-node
/**
 * E2E Test Runner for Quarkus CRUD API
 * Uses OpenAPI-generated TypeScript Axios client
 * Controlled by BASE_URL environment variable (default: http://localhost:8080)
 */

import axios, { AxiosError } from 'axios';
import {
  AuthenticationApi,
  RoomsApi,
  HealthApi,
  Configuration,
} from '../generated-client';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const config = new Configuration({
  basePath: BASE_URL,
});

let testsPassed = 0;
let testsFailed = 0;

function logInfo(message: string): void {
  console.log(`\x1b[32m[INFO]\x1b[0m ${message}`);
}

function logError(message: string): void {
  console.log(`\x1b[31m[ERROR]\x1b[0m ${message}`);
}

function logTest(message: string): void {
  console.log(`\x1b[33m[TEST]\x1b[0m ${message}`);
}

function assertTrue(condition: boolean, testName: string): void {
  if (condition) {
    logInfo(`✓ Test passed: ${testName}`);
    testsPassed++;
  } else {
    logError(`✗ Test failed: ${testName}`);
    testsFailed++;
  }
}

function assertEquals<T>(expected: T, actual: T, testName: string): void {
  if (expected === actual) {
    logInfo(`✓ Test passed: ${testName}`);
    testsPassed++;
  } else {
    logError(`✗ Test failed: ${testName} (expected: ${expected}, got: ${actual})`);
    testsFailed++;
  }
}

async function runE2ETests(): Promise<void> {
  logInfo(`Starting TypeScript E2E tests against ${BASE_URL}`);
  logInfo('=========================================');

  const authApi = new AuthenticationApi(config);
  const roomsApi = new RoomsApi(config);
  const healthApi = new HealthApi(config);

  let token1: string | null = null;
  let token2: string | null = null;
  let userId1: number | null = null;
  let roomId: number | null = null;

  try {
    // Test 1: Health Check
    logTest('Test 1: Health check endpoint');
    try {
      const healthResponse = await healthApi.getHealthStatus();
      assertEquals(200, healthResponse.status, 'Health check returns 200');
      assertTrue(
        healthResponse.data.status === 'UP',
        'Health check status is UP'
      );
    } catch (error) {
      logError(`Health check failed: ${error}`);
      testsFailed++;
    }

    // Test 2: Create Guest User 1
    logTest('Test 2: Create first guest user and extract JWT token');
    try {
      const guestResponse = await authApi.createGuestUser();
      assertEquals(200, guestResponse.status, 'Create guest user returns 200');
      assertTrue(
        guestResponse.data.id !== undefined,
        'Guest user has id'
      );
      assertTrue(
        guestResponse.data.createdAt !== undefined,
        'Guest user has createdAt'
      );

      // Extract token from Authorization header
      const authHeader = guestResponse.headers['authorization'];
      if (authHeader && typeof authHeader === 'string') {
        token1 = authHeader.replace('Bearer ', '').trim();
        userId1 = guestResponse.data.id;
        logInfo(`JWT token extracted: ${token1.substring(0, 20)}...`);
        logInfo(`User ID: ${userId1}`);
      } else {
        logError('Failed to extract JWT token from Authorization header');
        testsFailed++;
      }
    } catch (error) {
      logError(`Create guest user failed: ${error}`);
      testsFailed++;
    }

    // Test 3: Get Current User (with token)
    logTest('Test 3: Get current user with valid token');
    if (token1) {
      try {
        const configWithAuth = new Configuration({
          basePath: BASE_URL,
          accessToken: token1,
        });
        const authApiWithToken = new AuthenticationApi(configWithAuth);
        const meResponse = await authApiWithToken.getCurrentUser();
        assertEquals(200, meResponse.status, 'Get current user returns 200');
        assertEquals(
          userId1,
          meResponse.data.id,
          'Current user ID matches created user'
        );
      } catch (error) {
        logError(`Get current user failed: ${error}`);
        testsFailed++;
      }
    }

    // Test 4: Get Current User (without token - should fail)
    logTest('Test 4: Get current user without token (should fail with 401)');
    try {
      await authApi.getCurrentUser();
      logError('Expected 401 error but request succeeded');
      testsFailed++;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        logInfo('✓ Test passed: Get current user without token returns 401');
        testsPassed++;
      } else {
        logError(`Unexpected error: ${error}`);
        testsFailed++;
      }
    }

    // Test 5: List all rooms (no auth required)
    logTest('Test 5: List all rooms (no auth required)');
    try {
      const roomsResponse = await roomsApi.getAllRooms();
      assertEquals(200, roomsResponse.status, 'List all rooms returns 200');
      assertTrue(Array.isArray(roomsResponse.data), 'Rooms response is array');
    } catch (error) {
      logError(`List all rooms failed: ${error}`);
      testsFailed++;
    }

    // Test 6: Create a room (with auth)
    logTest('Test 6: Create a room with authentication');
    if (token1) {
      try {
        const configWithAuth = new Configuration({
          basePath: BASE_URL,
          accessToken: token1,
        });
        const roomsApiWithToken = new RoomsApi(configWithAuth);
        const createResponse = await roomsApiWithToken.createRoom({
          name: 'TS E2E Test Room',
          description: 'Created by TypeScript E2E test',
        });
        assertEquals(201, createResponse.status, 'Create room returns 201');
        assertTrue(
          createResponse.data.name === 'TS E2E Test Room',
          'Created room has correct name'
        );
        roomId = createResponse.data.id;
        logInfo(`Created room with ID: ${roomId}`);
      } catch (error) {
        logError(`Create room failed: ${error}`);
        testsFailed++;
      }
    }

    // Test 7: Create room without auth (should fail)
    logTest('Test 7: Create room without authentication (should fail with 401)');
    try {
      await roomsApi.createRoom({
        name: 'Unauthorized Room',
      });
      logError('Expected 401 error but request succeeded');
      testsFailed++;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        logInfo('✓ Test passed: Create room without auth returns 401');
        testsPassed++;
      } else {
        logError(`Unexpected error: ${error}`);
        testsFailed++;
      }
    }

    // Test 8: Get room by ID
    logTest('Test 8: Get room by ID');
    if (roomId) {
      try {
        const getRoomResponse = await roomsApi.getRoomById(roomId);
        assertEquals(200, getRoomResponse.status, 'Get room by ID returns 200');
        assertEquals(
          roomId,
          getRoomResponse.data.id,
          'Retrieved room has correct ID'
        );
        assertTrue(
          getRoomResponse.data.name === 'TS E2E Test Room',
          'Retrieved room has correct name'
        );
      } catch (error) {
        logError(`Get room by ID failed: ${error}`);
        testsFailed++;
      }
    }

    // Test 9: Update room
    logTest('Test 9: Update room');
    if (token1 && roomId) {
      try {
        const configWithAuth = new Configuration({
          basePath: BASE_URL,
          accessToken: token1,
        });
        const roomsApiWithToken = new RoomsApi(configWithAuth);
        const updateResponse = await roomsApiWithToken.updateRoom(roomId, {
          name: 'Updated TS E2E Room',
          description: 'Updated by TypeScript E2E test',
        });
        assertEquals(200, updateResponse.status, 'Update room returns 200');
        assertTrue(
          updateResponse.data.name === 'Updated TS E2E Room',
          'Updated room has new name'
        );
      } catch (error) {
        logError(`Update room failed: ${error}`);
        testsFailed++;
      }
    }

    // Test 10: Get my rooms
    logTest('Test 10: Get my rooms (authenticated)');
    if (token1) {
      try {
        const configWithAuth = new Configuration({
          basePath: BASE_URL,
          accessToken: token1,
        });
        const roomsApiWithToken = new RoomsApi(configWithAuth);
        const myRoomsResponse = await roomsApiWithToken.getMyRooms();
        assertEquals(200, myRoomsResponse.status, 'Get my rooms returns 200');
        assertTrue(
          Array.isArray(myRoomsResponse.data),
          'My rooms response is array'
        );
        assertTrue(
          myRoomsResponse.data.length > 0,
          'My rooms array is not empty'
        );
        const hasCreatedRoom = myRoomsResponse.data.some(
          (room) => room.id === roomId
        );
        assertTrue(hasCreatedRoom, 'My rooms includes the created room');
      } catch (error) {
        logError(`Get my rooms failed: ${error}`);
        testsFailed++;
      }
    }

    // Test 11: Get non-existent room (should fail)
    logTest('Test 11: Get non-existent room (should fail with 404)');
    try {
      await roomsApi.getRoomById(999999);
      logError('Expected 404 error but request succeeded');
      testsFailed++;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        logInfo('✓ Test passed: Get non-existent room returns 404');
        testsPassed++;
      } else {
        logError(`Unexpected error: ${error}`);
        testsFailed++;
      }
    }

    // Test 12: Update room without auth (should fail)
    logTest('Test 12: Update room without authentication (should fail with 401)');
    if (roomId) {
      try {
        await roomsApi.updateRoom(roomId, {
          name: 'Unauthorized Update',
        });
        logError('Expected 401 error but request succeeded');
        testsFailed++;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          logInfo('✓ Test passed: Update room without auth returns 401');
          testsPassed++;
        } else {
          logError(`Unexpected error: ${error}`);
          testsFailed++;
        }
      }
    }

    // Test 13: Create second user and try to update first user's room (should fail with 403)
    logTest(
      'Test 13: Create second user and try to update first users room (should fail with 403)'
    );
    try {
      const guest2Response = await authApi.createGuestUser();
      const authHeader2 = guest2Response.headers['authorization'];
      if (authHeader2 && typeof authHeader2 === 'string') {
        token2 = authHeader2.replace('Bearer ', '').trim();

        if (roomId) {
          const configWithAuth2 = new Configuration({
            basePath: BASE_URL,
            accessToken: token2,
          });
          const roomsApiWithToken2 = new RoomsApi(configWithAuth2);
          try {
            await roomsApiWithToken2.updateRoom(roomId, {
              name: 'Forbidden Update',
            });
            logError('Expected 403 error but request succeeded');
            testsFailed++;
          } catch (error) {
            if (axios.isAxiosError(error) && error.response?.status === 403) {
              logInfo('✓ Test passed: Update another users room returns 403');
              testsPassed++;
            } else {
              logError(`Unexpected error: ${error}`);
              testsFailed++;
            }
          }
        }
      }
    } catch (error) {
      logError(`Create second user or forbidden update test failed: ${error}`);
      testsFailed++;
    }

    // Test 14: Delete room
    logTest('Test 14: Delete room');
    if (token1 && roomId) {
      try {
        const configWithAuth = new Configuration({
          basePath: BASE_URL,
          accessToken: token1,
        });
        const roomsApiWithToken = new RoomsApi(configWithAuth);
        const deleteResponse = await roomsApiWithToken.deleteRoom(roomId);
        assertEquals(204, deleteResponse.status, 'Delete room returns 204');
      } catch (error) {
        logError(`Delete room failed: ${error}`);
        testsFailed++;
      }
    }

    // Test 15: Verify room is deleted
    logTest('Test 15: Verify room is deleted (should return 404)');
    if (roomId) {
      try {
        await roomsApi.getRoomById(roomId);
        logError('Expected 404 error but request succeeded');
        testsFailed++;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) {
          logInfo('✓ Test passed: Get deleted room returns 404');
          testsPassed++;
        } else {
          logError(`Unexpected error: ${error}`);
          testsFailed++;
        }
      }
    }

    // Test 16: Delete room without auth (should fail)
    logTest('Test 16: Delete room without authentication (should fail with 401)');
    if (token1) {
      try {
        // First create a room to delete
        const configWithAuth = new Configuration({
          basePath: BASE_URL,
          accessToken: token1,
        });
        const roomsApiWithToken = new RoomsApi(configWithAuth);
        const createForDeleteResponse = await roomsApiWithToken.createRoom({
          name: 'Room to delete',
        });
        const deleteRoomId = createForDeleteResponse.data.id;

        // Try to delete without auth
        try {
          await roomsApi.deleteRoom(deleteRoomId);
          logError('Expected 401 error but request succeeded');
          testsFailed++;
        } catch (error) {
          if (axios.isAxiosError(error) && error.response?.status === 401) {
            logInfo('✓ Test passed: Delete room without auth returns 401');
            testsPassed++;
          } else {
            logError(`Unexpected error: ${error}`);
            testsFailed++;
          }
        }

        // Clean up - delete with proper auth
        await roomsApiWithToken.deleteRoom(deleteRoomId);
      } catch (error) {
        logError(`Delete room test setup failed: ${error}`);
        testsFailed++;
      }
    }

    // Test 17: Test validation - create room with empty name (should fail)
    logTest('Test 17: Create room with empty name (should fail with 400)');
    if (token1) {
      try {
        const configWithAuth = new Configuration({
          basePath: BASE_URL,
          accessToken: token1,
        });
        const roomsApiWithToken = new RoomsApi(configWithAuth);
        await roomsApiWithToken.createRoom({
          name: '',
        });
        logError('Expected 400 error but request succeeded');
        testsFailed++;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 400) {
          logInfo('✓ Test passed: Create room with empty name returns 400');
          testsPassed++;
        } else {
          logError(`Unexpected error: ${error}`);
          testsFailed++;
        }
      }
    }

    // Summary
    console.log('');
    logInfo('=========================================');
    logInfo('TypeScript E2E Test Results');
    logInfo('=========================================');
    logInfo(`Total tests: ${testsPassed + testsFailed}`);
    logInfo(`Passed: ${testsPassed}`);
    logInfo(`Failed: ${testsFailed}`);
    logInfo('=========================================');

    if (testsFailed > 0) {
      logError('TypeScript E2E tests FAILED');
      process.exit(1);
    } else {
      logInfo('All TypeScript E2E tests PASSED ✓');
      process.exit(0);
    }
  } catch (error) {
    logError(`Unexpected error during E2E tests: ${error}`);
    process.exit(1);
  }
}

// Run the tests
runE2ETests().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
