# API Documentation

## Base URL

- Local: `http://localhost:8080`
- Production: `https://api.example.com`

## Authentication

The API uses JWT tokens stored in HTTP-only cookies for authentication. Guest users are automatically created and authenticated.

### Cookie-based Authentication

All authenticated endpoints require a `guest_token` cookie, which is automatically set when creating a guest user.

## Endpoints

### Authentication

#### Create Guest User
```http
POST /api/auth/guest
```

Creates an anonymous guest user and sets an authentication cookie.

**Response**: `200 OK`
```json
{
  "id": 1,
  "createdAt": "2025-11-18T06:00:00"
}
```

**Headers**:
```
Set-Cookie: guest_token=<jwt-token>; Path=/; Max-Age=31536000; HttpOnly
```

#### Get Current User
```http
GET /api/auth/me
```

Retrieves information about the currently authenticated user.

**Authentication**: Required (Cookie)

**Response**: `200 OK`
```json
{
  "id": 1,
  "createdAt": "2025-11-18T06:00:00"
}
```

**Error Response**: `401 Unauthorized`
```json
{
  "error": "No guest token found"
}
```

### Rooms

#### List All Rooms
```http
GET /api/rooms
```

Retrieves all rooms in the system.

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "name": "My Room",
    "description": "A sample room",
    "userId": 1,
    "createdAt": "2025-11-18T06:00:00"
  }
]
```

#### Get Room by ID
```http
GET /api/rooms/{id}
```

Retrieves a specific room by ID.

**Parameters**:
- `id` (path, required): Room ID (integer)

**Response**: `200 OK`
```json
{
  "id": 1,
  "name": "My Room",
  "description": "A sample room",
  "userId": 1,
  "createdAt": "2025-11-18T06:00:00"
}
```

**Error Response**: `404 Not Found`
```json
{
  "error": "Room not found"
}
```

#### Create Room
```http
POST /api/rooms
Content-Type: application/json
```

Creates a new room for the authenticated user.

**Authentication**: Required (Cookie)

**Request Body**:
```json
{
  "name": "My Room",
  "description": "A sample room"
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "name": "My Room",
  "description": "A sample room",
  "userId": 1,
  "createdAt": "2025-11-18T06:00:00"
}
```

**Error Responses**:
- `401 Unauthorized`: Authentication required
- `400 Bad Request`: Invalid input

#### Update Room
```http
PUT /api/rooms/{id}
Content-Type: application/json
```

Updates an existing room. Only the room owner can update.

**Authentication**: Required (Cookie)

**Parameters**:
- `id` (path, required): Room ID (integer)

**Request Body**:
```json
{
  "name": "Updated Room",
  "description": "Updated description"
}
```

**Response**: `200 OK`
```json
{
  "id": 1,
  "name": "Updated Room",
  "description": "Updated description",
  "userId": 1,
  "createdAt": "2025-11-18T06:00:00"
}
```

**Error Responses**:
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Not the room owner
- `404 Not Found`: Room not found

#### Delete Room
```http
DELETE /api/rooms/{id}
```

Deletes a room. Only the room owner can delete.

**Authentication**: Required (Cookie)

**Parameters**:
- `id` (path, required): Room ID (integer)

**Response**: `204 No Content`

**Error Responses**:
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Not the room owner
- `404 Not Found`: Room not found

#### Get My Rooms
```http
GET /api/rooms/my
```

Retrieves all rooms created by the authenticated user.

**Authentication**: Required (Cookie)

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "name": "My Room",
    "description": "A sample room",
    "userId": 1,
    "createdAt": "2025-11-18T06:00:00"
  }
]
```

### Live Updates

#### Subscribe to Room Events
```http
GET /api/live/rooms
Accept: text/event-stream
```

Subscribes to real-time room updates via Server-Sent Events.

**Response**: `200 OK` (streaming)
```
data: {"eventType":"ROOM_CREATED","roomId":1,"name":"My Room","description":"A sample room","userId":1,"timestamp":"2025-11-18T06:00:00"}

data: {"eventType":"ROOM_UPDATED","roomId":1,"name":"Updated Room","description":"Updated","userId":1,"timestamp":"2025-11-18T06:01:00"}

data: {"eventType":"ROOM_DELETED","roomId":1,"name":"Updated Room","description":"Updated","userId":1,"timestamp":"2025-11-18T06:02:00"}
```

Event types:
- `ROOM_CREATED`: A new room was created
- `ROOM_UPDATED`: An existing room was updated
- `ROOM_DELETED`: A room was deleted

### Health

#### Health Check
```http
GET /healthz
```

Returns the health status of the application.

**Response**: `200 OK`

## Error Handling

All errors follow a consistent format:

```json
{
  "error": "Error message describing what went wrong"
}
```

Common HTTP status codes:
- `200 OK`: Successful request
- `201 Created`: Resource created successfully
- `204 No Content`: Successful deletion
- `400 Bad Request`: Invalid input
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

## OpenAPI Specification

The complete OpenAPI specification is available at:
- YAML: `/q/openapi`
- Swagger UI: `/q/swagger-ui`
