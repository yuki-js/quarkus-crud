# JWT-Based Authentication API Documentation

This API provides secure JWT-based authentication for both guest users and registered users, with CRUD operations for rooms.

## Authentication Overview

The API uses **JWT (JSON Web Token)** with RSA-2048 signature for secure, stateless authentication. All authenticated endpoints require a Bearer token in the `Authorization` header.

### Security Features
- **Cryptographically signed JWT tokens** using RSA-2048 key pair
- **Secure password hashing** with SHA-256 and random salt
- **Role-based access control** (guest, user roles)
- **Token expiration** (24 hours by default)
- **Standards-compliant** Bearer token authentication (RFC 6750)

## Database Setup

The application uses PostgreSQL with Flyway migrations. To set up the database:

1. Start PostgreSQL:
```bash
docker run --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=quarkus_crud -p 5432:5432 -d postgres:latest
```

2. The application will automatically run migrations on startup.

## API Endpoints

### Authentication Endpoints

#### Create Guest User
Creates an anonymous guest user and returns a JWT token.

**Endpoint:** `POST /api/auth/guest`

**Response:**
```json
{
  "id": 1,
  "createdAt": "2025-10-29T18:00:00"
}
```

**Headers Returned:** 
- `Authorization: Bearer <JWT_TOKEN>`

**JWT Token Claims:**
- `sub`: User ID (subject)
- `upn`: User principal name (guest_{userId} for guest users)
- `iss`: Token issuer (https://quarkus-crud.example.com)
- `exp`: Expiration timestamp
- `groups`: User roles (["guest"] for guest users)

#### Get Current User
Retrieves the current user information from the JWT token.

**Endpoint:** `GET /api/auth/me`

**Headers Required:** 
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "id": 1,
  "createdAt": "2025-10-29T18:00:00"
}
```

**Note:** JWT tokens provide stateless authentication and include user identity in the token itself.

### Room Endpoints

#### Create Room
Creates a new room for the authenticated user.

**Endpoint:** `POST /api/rooms`

**Headers Required:** 
```
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "name": "My Room",
  "description": "A sample room"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "My Room",
  "description": "A sample room",
  "userId": 1,
  "createdAt": "2025-10-29T18:00:00"
}
```

#### Get All Rooms
Retrieves all rooms in the system.

**Endpoint:** `GET /api/rooms`

**Response:**
```json
[
  {
    "id": 1,
    "name": "My Room",
    "description": "A sample room",
    "userId": 1,
    "createdAt": "2025-10-29T18:00:00"
  }
]
```

#### Get Room by ID
Retrieves a specific room by ID.

**Endpoint:** `GET /api/rooms/{id}`

**Response:**
```json
{
  "id": 1,
  "name": "My Room",
  "description": "A sample room",
  "userId": 1,
  "createdAt": "2025-10-29T18:00:00"
}
```

#### Get My Rooms
Retrieves all rooms created by the authenticated user.

**Endpoint:** `GET /api/rooms/my`

**Headers Required:** 
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "My Room",
    "description": "A sample room",
    "userId": 1,
    "createdAt": "2025-10-29T18:00:00"
  }
]
```

#### Update Room
Updates an existing room (only the owner can update).

**Endpoint:** `PUT /api/rooms/{id}`

**Headers Required:** 
```
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "name": "Updated Room",
  "description": "Updated description"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Updated Room",
  "description": "Updated description",
  "userId": 1,
  "createdAt": "2025-10-29T18:00:00"
}
```

#### Delete Room
Deletes a room (only the owner can delete).

**Endpoint:** `DELETE /api/rooms/{id}`

**Headers Required:** 
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:** 204 No Content

### Live Endpoints

#### Subscribe to Live Room Updates (Server-Sent Events)
Subscribe to real-time room updates via Server-Sent Events. Receive notifications when rooms are created, updated, or deleted.

**Endpoint:** `GET /api/live/rooms`

**Headers Required:** None

**Response:** Server-Sent Events stream

**Event Format:**
```json
{
  "eventType": "ROOM_CREATED|ROOM_UPDATED|ROOM_DELETED",
  "roomId": 1,
  "name": "My Room",
  "description": "A sample room",
  "userId": 1,
  "timestamp": "2025-10-30T12:00:00"
}
```

**Note:** This endpoint uses Server-Sent Events (SSE) to push real-time updates to connected clients. The connection stays open and events are sent as they occur.

## Example Usage with curl

### 1. Create a guest user
```bash
curl -c cookies.txt -X POST http://localhost:8080/api/auth/guest
```

### 2. Get current user info
```bash
curl -b cookies.txt http://localhost:8080/api/auth/me
```

### 3. Create a room
```bash
curl -b cookies.txt -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Room","description":"My test room"}'
```

### 4. Get my rooms
```bash
curl -b cookies.txt http://localhost:8080/api/rooms/my
```

### 5. Get all rooms
```bash
curl http://localhost:8080/api/rooms
```

### 6. Subscribe to live room updates
```bash
curl -N http://localhost:8080/api/live/rooms
```

**Note:** The `-N` flag disables curl's output buffering to see events in real-time.

**Example usage with JavaScript:**
```javascript
const eventSource = new EventSource('http://localhost:8080/api/live/rooms');

eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log(`${data.eventType}: Room "${data.name}" (ID: ${data.roomId})`);
};

eventSource.onerror = (error) => {
    console.error('SSE connection error:', error);
};

// Close the connection when done
// eventSource.close();
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    guest_token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Rooms Table
```sql
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rooms_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## Running the Application

### Development Mode
```bash
./gradlew quarkusDev
```

### Build and Run
```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

## Configuration

Edit `src/main/resources/application.properties` to configure the database connection:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus_crud
```
