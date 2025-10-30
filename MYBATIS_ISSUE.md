# MyBatis Configuration Issue

## Problem

The application has a MyBatis configuration issue where the XML mapper files are not being properly loaded at runtime. This results in the error:

```
org.apache.ibatis.binding.BindingException: Invalid bound statement (not found): app.aoki.mapper.UserMapper.insert
```

This issue affects BOTH:
- Development mode (`./gradlew quarkusDev`)
- Test mode (`./gradlew test`)
- Production mode (`java -jar build/quarkus-app/quarkus-run.jar`)

## Root Cause

The issue is related to how `quarkus-mybatis` (version 2.4.1) integrates with Quarkus 3.28.5. The XML mapper files are correctly packaged in the JAR but are not being found by MyBatis at runtime.

## Evidence

1. **Mapper XMLs are in the JAR**:
   ```bash
   unzip -l build/quarkus-app/app/quarkus-template-0.0.1.jar | grep mapper
   # Shows: mapper/UserMapper.xml, mapper/RoomMapper.xml
   ```

2. **MyBatis config file exists**:
   ```bash
   unzip -l build/quarkus-app/app/quarkus-template-0.0.1.jar | grep mybatis
   # Shows: mybatis-config.xml
   ```

3. **Configuration looks correct**:
   - `mybatis-config.xml` properly references the mappers
   - `application.properties` has correct MyBatis settings
   - Mapper interfaces are properly annotated with `@Mapper`

## Potential Solutions

### Option 1: Use Annotation-Based Mappers (Recommended)

Instead of XML mappers, use MyBatis annotations directly on the mapper interfaces:

```java
@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO users (guest_token, created_at, updated_at) VALUES (#{guestToken}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Select("SELECT id, guest_token as guestToken, created_at as createdAt, updated_at as updatedAt FROM users WHERE id = #{id}")
    Optional<User> findById(@Param("id") Long id);
    
    // ... other methods
}
```

**Pros:**
- Works reliably with Quarkus
- No XML configuration needed
- Type-safe

**Cons:**
- Requires refactoring all mappers
- Complex SQL statements can be harder to read

### Option 2: Upgrade quarkus-mybatis

Check if a newer version of `quarkus-mybatis` is available that's compatible with Quarkus 3.28.5:

```gradle
implementation 'io.quarkiverse.mybatis:quarkus-mybatis:3.0.0' // Check latest version
```

### Option 3: Configure MyBatis Programmatically

Create a MyBatis configuration bean that programmatically loads the mappers:

```java
@ApplicationScoped
public class MyBatisConfig {
    
    @Produces
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        
        // Add mapper classes
        configuration.addMapper(UserMapper.class);
        configuration.addMapper(RoomMapper.class);
        
        // Load XML mappers from resources
        Resources.setDefaultClassLoader(MyBatisConfig.class.getClassLoader());
        try (Reader userMapperReader = Resources.getResourceAsReader("mapper/UserMapper.xml");
             Reader roomMapperReader = Resources.getResourceAsReader("mapper/RoomMapper.xml")) {
            XMLMapperBuilder userMapperBuilder = new XMLMapperBuilder(userMapperReader, configuration, 
                "mapper/UserMapper.xml", configuration.getSqlFragments());
            userMapperBuilder.parse();
            
            XMLMapperBuilder roomMapperBuilder = new XMLMapperBuilder(roomMapperReader, configuration, 
                "mapper/RoomMapper.xml", configuration.getSqlFragments());
            roomMapperBuilder.parse();
        }
        
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
```

### Option 4: Check Classpath Issues

The issue might be related to classloader hierarchy. Try explicitly specifying the mapper locations:

In `application.properties`:
```properties
mybatis.mapper-locations=classpath*:mapper/*.xml
mybatis.type-aliases-package=app.aoki.entity
mybatis.configuration.map-underscore-to-camel-case=true
```

##  Workaround for Testing

Until the MyBatis issue is fixed, the provided integration test script (`scripts/integration-test.sh`) can be used to test the API endpoints once the application is fixed and running.

## Next Steps

1. Try Option 1 (annotation-based mappers) as it's the most reliable with Quarkus
2. If annotations aren't preferred, investigate Option 2 (upgrade quarkus-mybatis)
3. Create a minimal reproduction case and report to quarkus-mybatis maintainers
4. Once fixed, run the comprehensive test suite provided in this PR

## Testing After Fix

Once the MyBatis issue is resolved:

1. Start the application:
   ```bash
   ./gradlew quarkusDev
   ```

2. Run the integration tests:
   ```bash
   ./scripts/integration-test.sh
   ```

3. Run unit tests:
   ```bash
   ./gradlew test
   ```

All tests should pass once MyBatis is properly configured.
