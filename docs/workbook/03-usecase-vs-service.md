# Chapter 3: UseCase vs Service - The Crucial Distinction

## The Most Confusing Part

This is where most developers get confused. "Both sound like business logic!" But they're actually quite different.

## The Simple Answer

| | **UseCase** | **Service** |
|---|---|---|
| **Japanese** | ユースケース (what) | サービス (how) |
| **English** | Use Case | Service |
| **Answer to** | "What are we doing?" | "How do we do it?" |
| **Contains** | Flow + Authorization | Technical steps |
| **Example** | "Validate user, then create, then send email" | "Insert into DB", "Hash password" |

## Real-World Analogy: Restaurant Order

When you order food at a restaurant:

**UseCase (what happens)**:
1. Waiter takes your order
2. Waiter checks if you're allowed to order (not banned)
3. Waiter sends order to kitchen
4. Kitchen prepares food
5. Waiter brings food to you
6. Waiter records the transaction

**Service (how it happens)**:
- The chef knows HOW to cook each dish
- The grill knows HOW to sear meat at 500°F
- The refrigerator knows HOW to keep food cold

The **UseCase** is about the flow and authorization. The **Service** is about the technical knowledge.

## Code Example: User Registration

Let's see the difference in a real scenario: **User Registration**.

### The "Wrong" Way (No Separation)

```java
@Service
public class BadUserService {
    
    public User registerUser(String email, String password) {
        // UseCase logic mixed with Service logic
        
        // Validation (should be in UseCase)
        if (email == null) throw new IllegalArgumentException("Email required");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email");
        
        // Authorization (should be in UseCase)
        // "Can this person register?" - THIS IS USECASE
        
        // Technical steps (correctly in Service)
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hashPassword(password)); // HOW
        user.setCreatedAt(LocalDateTime.now());
        
        // Database (correctly in Service)
        userMapper.insert(user); // HOW
        
        // External calls (should be in UseCase)
        sendWelcomeEmail(email); // "We need to send email AFTER user is created" - THIS IS FLOW
        
        return user;
    }
}
```

This service is doing too much. It contains authorization decisions, flow control, AND technical implementation.

### The Right Way

#### UseCase Layer: "What" and "Who"

```java
@ApplicationScoped
public class RegistrationUseCase {

    @Inject UserService userService;        // HOW
    @Inject EmailService emailService;      // HOW
    @Inject UserMapper userMapper;          // HOW

    /**
     * What: Register a new user
     * Who: Anyone with a valid email can register
     */
    public User registerUser(String email, String password) {
        // Step 1: Validate inputs (part of the flow)
        validateEmail(email);
        validatePassword(password);
        
        // Step 2: Check if email is already taken (authorization)
        if (emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Step 3: Create user (delegates to Service)
        User user = userService.createUser(email, password);
        
        // Step 4: Send welcome email (flow - AFTER user is created)
        emailService.sendWelcomeEmail(email);
        
        // Step 5: Return result
        return user;
    }
    
    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }
    
    private boolean emailExists(String email) {
        return userMapper.findByEmail(email).isPresent();
    }
}
```

#### Service Layer: "How"

```java
@ApplicationScoped
public class UserService {

    @Inject UserMapper userMapper;

    /**
     * HOW: Create a user entity with hashed password
     * This is a reusable technical operation
     */
    public User createUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hashPassword(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        
        userMapper.insert(user);
        return user;
    }
    
    /**
     * HOW: Hash a password
     * This is a technical detail that could be reused
     */
    private String hashPassword(String password) {
        // Technical implementation details
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
```

## Another Example: Event Registration

Here's a more complex example: **Registering for an Event**.

### UseCase (What + Who)

```java
@ApplicationScoped
public class EventRegistrationUseCase {

    @Inject EventService eventService;
    @Inject UserService userService;
    @Inject EventAttendeeMapper attendeeMapper;

    /**
     * What: Register a user for an event
     * Who: Only if:
     *   - User is not already registered
     *   - User is not banned from this event
     *   - Event has available spots
     */
    public Attendee registerForEvent(Long userId, Long eventId) {
        // Step 1: Get entities
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Event event = eventService.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        
        // Step 2: Authorization checks (USECASE RESPONSIBILITY)
        if (isUserBanned(userId, eventId)) {
            throw new SecurityException("You are banned from this event");
        }
        
        if (isAlreadyRegistered(userId, eventId)) {
            throw new IllegalArgumentException("Already registered for this event");
        }
        
        if (!hasAvailableSpots(eventId)) {
            throw new IllegalStateException("Event is full");
        }
        
        // Step 3: Do the work (delegates to Service)
        Attendee attendee = eventService.addAttendee(userId, eventId);
        
        // Step 4: Post-registration actions (FLOW)
        eventService.sendConfirmationEmail(userId, eventId);
        
        return attendee;
    }
    
    // Authorization helpers (USECASE RESPONSIBILITY)
    private boolean isUserBanned(Long userId, Long eventId) {
        // Check if user is banned
        return false; // Simplified
    }
    
    private boolean isAlreadyRegistered(Long userId, Long eventId) {
        return attendeeMapper.findByUserAndEvent(userId, eventId).isPresent();
    }
    
    private boolean hasAvailableSpots(Long eventId) {
        Event event = eventService.findById(eventId).orElseThrow();
        long currentCount = attendeeMapper.countByEvent(eventId);
        return currentCount < event.getMaxAttendees();
    }
}
```

### Service (How)

```java
@ApplicationScoped
public class EventService {

    @Inject EventMapper eventMapper;
    @Inject EventAttendeeMapper attendeeMapper;
    @Inject EmailService emailService;

    /**
     * HOW: Find an event by ID
     */
    public Optional<Event> findById(Long eventId) {
        return eventMapper.findById(eventId);
    }

    /**
     * HOW: Add an attendee to an event
     * This is a technical operation
     */
    @Transactional
    public Attendee addAttendee(Long userId, Long eventId) {
        Attendee attendee = new Attendee();
        attendee.setUserId(userId);
        attendee.setEventId(eventId);
        attendee.setRegisteredAt(LocalDateTime.now());
        
        attendeeMapper.insert(attendee);
        
        // Update event attendee count
        eventMapper.incrementAttendeeCount(eventId);
        
        return attendee;
    }
    
    /**
     * HOW: Send confirmation email
     * This is a technical operation (could be reused elsewhere)
     */
    public void sendConfirmationEmail(Long userId, Long eventId) {
        User user = // get user
        Event event = // get event
        emailService.sendEmail(user.getEmail(), "Confirmation", 
            "You are registered for " + event.getTitle());
    }
}
```

## The Key Insight

> **If you're asking "can this user do X?" → UseCase**
> **If you're asking "how do I do X technically?" → Service**

## Exercise 3.1: Separate the Concerns

Here's a single method. Separate it into UseCase and Service:

```java
public Order createOrder(Long userId, Long productId, Integer quantity) {
    // Check user exists
    User user = userRepository.findById(userId);
    if (user == null) throw new IllegalArgumentException("User not found");
    
    // Check product exists
    Product product = productRepository.findById(productId);
    if (product == null) throw new IllegalArgumentException("Product not found");
    
    // Check user is active
    if (!user.isActive()) throw new SecurityException("User not active");
    
    // Check stock
    if (product.getStock() < quantity) {
        throw new IllegalStateException("Insufficient stock");
    }
    
    // Calculate total
    BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));
    
    // Create order
    Order order = new Order();
    order.setUserId(userId);
    order.setProductId(productId);
    order.setQuantity(quantity);
    order.setTotalPrice(total);
    
    // Save
    orderRepository.insert(order);
    
    // Update stock
    product.setStock(product.getStock() - quantity);
    productRepository.update(product);
    
    // Send notification
    notificationService.sendOrderCreatedEmail(user.getEmail(), order);
    
    return order;
}
```

**Your task**:
1. Which authorization checks belong in UseCase?
2. Which technical operations belong in Service?
3. What would the Service methods look like?
4. What would the UseCase method look like?

---

## Key Takeaways

1. **UseCase = What + Who** (flow and authorization)
2. **Service = How** (technical implementation)
3. **UseCase orchestrates, Service executes**
4. **Authorization decisions are ALWAYS in UseCase**
5. **If you ask "can they?" → UseCase. If you ask "how?" → Service.**
6. **Service methods should be reusable by multiple UseCases**

---

## What's Next?

Now you understand layers and the UseCase/Service distinction. Let's look at **where each type of code should live** - the file organization.

**[Next: File Organization](04-file-organization.md)**