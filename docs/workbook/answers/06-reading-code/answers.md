# Chapter 6: Exercise Answers

## Exercise 6.1: Trace the Code

### Questions and Answers

**1. What's the endpoint path?**

Based on `FriendshipsApiImpl.java`:

```
POST /api/friendships/{otherUserId}
GET /api/friendships/{otherUserId}
PUT /api/friendships/{otherUserId}
GET /api/me/friendships/received
POST /api/users/{userId}/friendship
```

**2. Which UseCase handles it?**

Looking at `FriendshipsApiImpl.java`:

```java
@Inject FriendshipUseCase friendshipUseCase;
```

The `FriendshipUseCase` handles friendship business logic.

**3. What authorization checks are made?**

In `FriendshipUseCase.java`:

```java
public Friendship getFriendshipByOtherUser(Long requestingUserId, Long otherUserId) {
    // Check if friendship exists
    Friendship friendship = friendshipMapper
        .findBySenderAndRecipient(requestingUserId, otherUserId)
        .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
    
    // Only sender can view the friendship details
    if (!friendship.getSenderId().equals(requestingUserId)) {
        throw new SecurityException("You can only view your own friendships");
    }
    
    return friendship;
}
```

Authorization rules from `FriendshipUseCase`:
- Only the sender can view friendship details
- Only sender can update friendship metadata
- Receiver can mark friendship as received

**4. What Service methods are called?**

Looking at `FriendshipUseCase`:

```java
@Inject FriendshipMapper friendshipMapper;
@Inject UserMapper userMapper;
```

The "services" are actually mappers in this template (MyBatis):

```java
friendshipMapper.findBySenderAndRecipient(senderId, recipientId);
friendshipMapper.findByRecipient(recipientId);
friendshipMapper.insert(friendship);
friendshipMapper.update(friendship);
userMapper.findById(userId);
```

**5. What database tables are affected?**

From the SQL in `FriendshipMapper.xml`:

```sql
CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    usermeta TEXT,
    sysmeta TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

Tables affected:
- `friendships` - main table for friendships
- `users` - referenced for validation (but not modified)

---

## How to Trace Any Feature

### Step 1: Find the Resource

Search in `resource/` folder for the API endpoint:

```bash
grep -r "friendships" src/main/java/app/aoki/quarkuscrud/resource/
```

### Step 2: Find the UseCase

Look at the Resource class for `@Inject` fields:

```java
@Inject FriendshipUseCase friendshipUseCase;
```

### Step 3: Follow the Chain

In the Resource method:

```java
public Response getFriendshipByOtherUser(Long otherUserId) {
    User user = authenticatedUser.get();
    return friendshipUseCase.getFriendshipByOtherUser(user.getId(), otherUserId);
}
```

### Step 4: Check Authorization

In the UseCase method:

```java
if (!friendship.getSenderId().equals(requestingUserId)) {
    throw new SecurityException("You can only view your own friendships");
}
```

### Step 5: Find Data Access

In the UseCase:

```java
friendshipMapper.findBySenderAndRecipient(requestingUserId, otherUserId);
```

### Step 6: Check SQL

In `src/main/resources/mapper/FriendshipMapper.xml`:

```xml
<select id="findBySenderAndRecipient" resultType="Friendship">
    SELECT * FROM friendships 
    WHERE sender_id = #{senderId} AND recipient_id = #{recipientId}
</select>
```

---

## Hints

Still stuck?

1. **Hint 1**: Start at the Resource layer. Search for `@Path` annotations with the feature name.

2. **Hint 2**: Use `grep` to find all files that mention the feature:
   ```bash
   grep -r "friendship" src/
   ```

3. **Hint 3**: Look for `@Inject` in the Resource to find which UseCase/Service it uses.

4. **Hint 4**: In UseCase, look for `SecurityException` for authorization logic.

5. **Hint 5**: In UseCase, look for `@Transactional` for data modification logic.