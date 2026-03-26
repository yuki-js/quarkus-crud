# Chapter 5: Exercise Answers

## Exercise 5.1: Add a New Endpoint

You should have completed these steps:

### Step 1: Schema Added

In `openapi/components/schemas/user.yaml`, you added:

```yaml
UserStatusUpdateRequest:
  type: object
  properties:
    status:
      type: string
      enum: [ACTIVE, SUSPENDED]
```

### Step 2: Path Added

In `openapi/paths/users.yaml`, you added:

```yaml
/api/users/{userId}/status:
  patch:
    tags:
      - Users
    summary: Update user status
    operationId: updateUserStatus
    parameters:
      - name: userId
        in: path
        required: true
        schema:
          type: integer
          format: int64
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '../components/schemas/user.yaml#/UserStatusUpdateRequest'
    responses:
      '200':
        description: Status updated
      '403':
        description: Not authorized
      '404':
        description: User not found
```

### Step 3: Generate Code

You ran:
```bash
./gradlew compileOpenApi generateOpenApiModels
```

### Step 4: Implementation

The generated interface method looks like:

```java
@Path("/api")
public interface UsersApi {
    @PATCH
    @Path("/users/{userId}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response updateUserStatus(
        @PathParam("userId") Long userId,
        UserStatusUpdateRequest request
    );
}
```

You would implement it in `UsersApiImpl.java`:

```java
@Override
@Authenticated
@PATCH
@Path("/users/{userId}/status")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response updateUserStatus(
        @PathParam("userId") Long userId,
        UserStatusUpdateRequest request) {
    
    User user = authenticatedUser.get();
    
    // Authorization: Only admins or the user themselves
    if (!user.isAdmin() && !user.getId().equals(userId)) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new ErrorResponse("Not authorized"))
            .build();
    }
    
    try {
        userUseCase.updateUserStatus(userId, request.getStatus());
        return Response.ok().build();
    } catch (IllegalArgumentException e) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse(e.getMessage()))
            .build();
    }
}
```

---

## Common Mistakes

### Mistake 1: Forgetting to Run Gradle Tasks

After modifying YAML, you MUST run:
```bash
./gradlew compileOpenApi generateOpenApiModels
```

If you forget, you'll get compilation errors because the generated classes don't exist.

### Mistake 2: Wrong Ref Path

Make sure your `$ref` paths are correct:
```yaml
# Correct
$ref: '../components/schemas/user.yaml#/UserStatusUpdateRequest'

# Wrong (missing ../)
$ref: 'components/schemas/user.yaml#/UserStatusUpdateRequest'
```

### Mistake 3: Putting Implementation in Resource

Remember: Resource should be THIN. Don't put business logic there:

```java
// WRONG - business logic in resource
@Patch
public Response updateUserStatus(Long userId, Request req) {
    User user = userMapper.findById(userId);
    if (user == null) throw new IllegalArgumentException();
    if (!user.isAdmin()) throw new SecurityException();
    user.setStatus(req.getStatus());
    userMapper.update(user);
    return ok();
}

// RIGHT - delegation
@Patch
public Response updateUserStatus(Long userId, Request req) {
    return userUseCase.updateStatus(userId, req.getStatus());
}
```

---

## Hints

Still stuck?

1. **Hint 1**: Start with YAML only. Don't write any Java until you've generated the code.

2. **Hint 2**: If you get "cannot find symbol" errors, you probably forgot to run the Gradle tasks.

3. **Hint 3**: Look at existing implementations in `UsersApiImpl.java` for patterns to follow.