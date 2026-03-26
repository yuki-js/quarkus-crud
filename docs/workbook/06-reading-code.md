# Chapter 6: Reading Code - The Underrated Skill

## Most Developers Can't Read Code

We're taught to write code. We're rarely taught to read it.

But in reality, you spend **more time reading code than writing it**:
- Debugging existing features
- Understanding how something works
- Adding new features to existing code
- Onboarding to a new project

This chapter teaches you how to efficiently read and understand a codebase.

## The Navigation Pattern

When you need to understand how something works, follow this pattern:

```
1. Find the ENTRY POINT (Resource layer)
2. Follow the CHAIN (UseCase → Service → Repository)
3. Understand the TERMINUS (Repository returns data)
```

## Step 1: Finding Entry Points

### How to Find a Specific Endpoint

If you want to understand `GET /api/users/{userId}`:

**Method 1: Search for the path**

```bash
grep -r "@Path.*users" src/
```

**Method 2: Look in resource files**

Look in `resource/` folder for files with `Api` suffix.

**Method 3: Check the generated API**

Look in `generated/api/` for interfaces like `UsersApi`.

### The Resource File Structure

Resource files typically:
- Have names ending with `ApiImpl.java` or `Controller.java`
- Are in the `resource/` package
- Implement generated interfaces
- Have `@Path`, `@GET`, `@POST` annotations

```java
@ApplicationScoped
@Path("/api")
public class UsersApiImpl implements UsersApi {
    // Entry point for /api/users/*
}
```

## Step 2: Following the Chain

Once you find the entry point, follow the chain:

```java
@GET
@Path("/users/{userId}")
public Response getUserById(@PathParam("userId") Long userId) {
    // This calls...
    return userService
        .findById(userId)  // → Service layer
        .map(user -> Response.ok(toUserPublicResponse(user)).build())
        .orElse(...);
}
```

So `getUserById` calls `userService.findById()`.

Let's look at the service:

```java
public Optional<User> findById(Long id) {
    // This calls...
    return userMapper.findById(id);  // → Repository layer
}
```

And the mapper:

```java
@Mapper
public interface UserMapper {
    Optional<User> findById(Long id);  // → Database
}
```

**Complete chain**: Resource → Service → Repository → Database

## Step 3: Understanding the Data Flow

### Request Flow

```
HTTP Request
    ↓
Resource (parse parameters)
    ↓
UseCase (authorization)
    ↓
Service (business logic)
    ↓
Repository (data access)
    ↓
Database
```

### Response Flow

```
Database
    ↓
Repository (result set mapping)
    ↓
Service (transformation)
    ↓
UseCase (optional processing)
    ↓
Resource (format response)
    ↓
HTTP Response
```

## Practical Exercise: Trace a Feature

Let's trace the "Create Event" feature to understand how it works.

### Step 1: Find the Entry Point

Search for event creation:

```bash
grep -r "events" src/main/java/app/aoki/quarkuscrud/resource/
```

Found: `EventsApiImpl.java`

Look for POST endpoint:

```java
@Override
@Authenticated
@POST
@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
public Response createEvent(EventCreateRequest eventCreateRequest) {
    // ...
}
```

### Step 2: Follow the Chain

The implementation delegates to something. Let's find out:

```java
public Response createEvent(EventCreateRequest eventCreateRequest) {
    User user = authenticatedUser.get();
    
    try {
        EventLiveEvent event = eventUseCase.createEvent(
            user.getId(),
            eventCreateRequest.getTitle(),
            eventCreateRequest.getDescription(),
            eventCreateRequest.getEventStatus(),
            eventCreateRequest.getMaxAttendees(),
            eventCreateRequest.getStartTime(),
            eventCreateRequest.getEndTime(),
            eventCreateRequest.getLocation()
        );
        return Response.ok(event).build();
    } catch (SecurityException e) {
        return Response.status(Response.Status.FORBIDDEN)
            .entity(new ErrorResponse(e.getMessage()))
            .build();
    } catch (IllegalArgumentException e) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse(e.getMessage()))
            .build();
    }
}
```

So it calls `eventUseCase.createEvent()`.

### Step 3: Look at the UseCase

```java
public EventLiveEvent createEvent(
        Long organizerId,
        String title,
        String description,
        EventStatus eventStatus,
        Integer maxAttendees,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String location) {
    
    // Authorization: Only active users can create events
    User organizer = userService.findById(organizerId)
        .orElseThrow(() -> new IllegalArgumentException("Organizer not found"));
    
    if (organizer.getAccountLifecycle() != AccountLifecycle.ACTIVE) {
        throw new SecurityException("Only active users can create events");
    }
    
    // Business logic
    Event event = new Event();
    event.setOrganizerId(organizerId);
    event.setTitle(title);
    event.setDescription(description);
    event.setEventStatus(eventStatus);
    event.setMaxAttendees(maxAttendees);
    event.setStartTime(startTime);
    event.setEndTime(endTime);
    event.setLocation(location);
    event.setCreatedAt(LocalDateTime.now());
    event.setUpdatedAt(LocalDateTime.now());
    
    eventMapper.insert(event);
    
    // Create invitation code
    String code = generateInvitationCode();
    // ... (more logic)
    
    return toEventLiveEvent(event);
}
```

### Step 4: Look at the Service/Repository

The UseCase calls `eventMapper.insert()`. This is in the Repository layer.

```java
@Mapper
public interface EventMapper {
    void insert(Event event);
    Optional<Event> findById(Long id);
    void update(Event event);
}
```

## Finding Business Rules

Business rules are often in UseCase classes. Search for:

```java
// Authorization rules
if (!userId.equals(requestingUserId)) {
    throw new SecurityException("...");
}

// Validation rules
if (value < 0) {
    throw new IllegalArgumentException("...");
}

// State transitions
if (currentStatus == DELETED) {
    throw new IllegalStateException("...");
}
```

## Reading Mapper XML

Mapper XML files contain the actual SQL. Find them in:

```
src/main/resources/mapper/
```

```xml
<mapper namespace="app.aoki.quarkuscrud.mapper.EventMapper">
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO events (
            organizer_id, title, description, event_status,
            max_attendees, start_time, end_time, location,
            created_at, updated_at
        ) VALUES (
            #{organizerId}, #{title}, #{description}, #{eventStatus},
            #{maxAttendees}, #{startTime}, #{endTime}, #{location},
            #{createdAt}, #{updatedAt}
        )
    </insert>
</mapper>
```

## Exercise 6.1: Trace the Code

**Task**: Understand how friendship creation works.

**Questions to answer**:
1. What's the endpoint path?
2. Which UseCase handles it?
3. What authorization checks are made?
4. What Service methods are called?
5. What database tables are affected?

**Hint**: Look in `FriendshipsApiImpl.java` and `FriendshipUseCase.java`.

## Key Takeaways

1. **Start at Resource layer** - entry points are there
2. **Follow the chain** - Resource → UseCase → Service → Repository
3. **Understand data flow** - both request and response
4. **Business rules are in UseCase** - search for SecurityException
5. **SQL is in Mapper XML** - check resources/mapper/

---

## What's Next?

Time to put it all together. The final chapter is a **workshop project** where you'll add a new feature using everything you've learned.

**[Next: Workshop Project](07-workshop-project.md)**