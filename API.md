# Guest User Authentication API Documentation

This API provides anonymous guest user authentication using cookies and CRUD operations for rooms.

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
Creates an anonymous guest user and sets a cookie with the guest token.

**Endpoint:** `POST /api/auth/guest`

**Response:**
```json
{
  "id": 1,
  "guestToken": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2025-10-29T18:00:00"
}
```

**Cookie Set:** `guest_token` (HttpOnly, Max-Age: 1 year)

#### Get Current User
Retrieves the current user information from the guest token cookie.

**Endpoint:** `GET /api/auth/me`

**Headers Required:** Cookie with `guest_token`

**Response:**
```json
{
  "id": 1,
  "guestToken": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2025-10-29T18:00:00"
}
```

### Room Endpoints

#### Create Room
Creates a new room for the authenticated user.

**Endpoint:** `POST /api/rooms`

**Headers Required:** Cookie with `guest_token`

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

**Headers Required:** Cookie with `guest_token`

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

**Headers Required:** Cookie with `guest_token`

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

**Headers Required:** Cookie with `guest_token`

**Response:** 204 No Content

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
