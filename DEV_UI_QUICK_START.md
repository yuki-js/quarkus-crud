# Quarkus Dev UI - Quick Start Guide

## TL;DR - Get Started in 30 Seconds

```bash
# 1. Clone and enter the project
git clone https://github.com/yuki-js/quarkus-crud.git
cd quarkus-crud

# 2. Start development mode (that's it!)
./gradlew quarkusDev

# 3. Open your browser to the Dev UI
# http://localhost:8080/q/dev-ui
```

No PostgreSQL setup needed! Dev Services handles everything automatically.

## What Just Happened?

When you ran `./gradlew quarkusDev`, Quarkus automatically:

1. ✅ Started a PostgreSQL 15 container via Dev Services
2. ✅ Created the database and configured the connection
3. ✅ Ran all Flyway migrations to set up the schema
4. ✅ Started the application with live reload enabled
5. ✅ Enabled the Dev UI at http://localhost:8080/q/dev-ui

## Explore the Dev UI

Open http://localhost:8080/q/dev-ui in your browser and discover:

### 📊 Configuration
- View all active configuration properties
- Override settings at runtime (no restart needed)
- See which values come from defaults vs custom config

### 🐳 Dev Services
- See the PostgreSQL container status
- View container logs
- Get JDBC connection details
- Restart or stop containers

### 📝 Swagger UI
- Test all API endpoints interactively
- View request/response schemas
- Authenticate with JWT tokens
- Try different inputs and see results immediately

**Quick Test Flow:**
1. POST `/api/authentication/guest` → Get JWT token
2. Click "Authorize" → Paste token with "Bearer " prefix
3. POST `/api/rooms` → Create a room
4. GET `/api/rooms` → See your room

### 🗄️ Database Tools
- View datasource configuration
- See connection pool statistics
- Check active connections

### 🏥 Health Checks
- Monitor liveness, readiness, startup probes
- See individual health check status
- Debug health issues in real-time

### 🧪 Continuous Testing
- Enable continuous testing (tests run on code changes)
- View test results in real-time
- Filter by passed/failed/skipped
- See detailed failure messages

### ℹ️ Build Info
- View application version and build time
- See Git commit information
- Check Java and Quarkus versions
- List installed features

## Development Workflow

### Make Code Changes
1. Edit Java files, configuration, or resources
2. Save the file
3. Quarkus detects the change and reloads automatically (1-2 seconds)
4. Test your changes immediately

### Test API Endpoints
1. Go to Dev UI → SmallRye OpenAPI → Swagger UI
2. Expand the endpoint you want to test
3. Click "Try it out"
4. Modify the request body/parameters
5. Click "Execute"
6. See the response immediately

### Debug Database Issues
1. Go to Dev UI → Dev Services → PostgreSQL
2. Note the JDBC URL and container ID
3. Use psql to inspect:
   ```bash
   # Find the container name from Dev UI
   docker exec -it <container-name> psql -U quarkus -d default
   
   # Run SQL queries
   \dt                    # List tables
   SELECT * FROM users;   # Query data
   \d users              # Describe table
   ```

### Run Tests While Developing
```bash
# Start dev mode with continuous testing
./gradlew quarkusDev --tests
```

Or enable in Dev UI → Continuous Testing → Enable

Tests run automatically whenever you save a file!

## Keyboard Shortcuts (in Terminal)

While `./gradlew quarkusDev` is running:

- **r** - Force restart the application
- **h** - Show help (all available commands)
- **s** - Restart with cache clearing
- **w** - Open Dev UI in browser
- **Ctrl+C** - Stop dev mode

## Common Questions

### Q: Do I need to start PostgreSQL manually?
**A:** No! Dev Services starts it automatically in a Docker container.

### Q: What if the container port conflicts with my existing PostgreSQL?
**A:** Dev Services uses a random available port, not 5432. No conflicts!

### Q: Does the container persist between sessions?
**A:** Yes! With `reuse=true`, the same container is reused for faster startup.

### Q: How do I use my own PostgreSQL instead?
**A:** Disable Dev Services:
```bash
./gradlew quarkusDev -Dquarkus.devservices.enabled=false
```

### Q: Are production deployments affected?
**A:** No! Dev Services only runs in dev mode. Production uses normal configuration.

### Q: Can I use Dev UI with my existing tests?
**A:** Yes! Tests use the same Dev Services containers, ensuring consistency.

## Next Steps

1. Read the [comprehensive Dev UI guide](docs/dev-ui-guide.md)
2. Check out the [development guide](docs/development.md)
3. Explore the [API documentation](docs/api.md)
4. Run the [test suite](docs/testing.md)

## Tips & Tricks

- **Fast Iteration**: Use Swagger UI for quick API testing instead of curl
- **Configuration Experiments**: Override config in Dev UI before editing files
- **Debug Database**: Use Dev Services dashboard to access PostgreSQL logs
- **Continuous Testing**: Enable it to catch regressions immediately
- **Live Reload**: Most changes apply without restart (Java, config, resources)

## Troubleshooting

**Dev UI not loading?**
- Ensure you ran `./gradlew quarkusDev` (not `./gradlew build`)
- Check the application started (no errors in terminal)
- Try http://127.0.0.1:8080/q/dev-ui

**PostgreSQL container not starting?**
- Ensure Docker is running: `docker ps`
- Check Docker has enough resources (memory/disk)
- View logs in Dev UI → Dev Services → PostgreSQL

**Live reload not working?**
- Save your file (IDE auto-save should be enabled)
- Check terminal for compilation errors
- Try manual reload: press **r** in terminal

---

**Happy Coding! 🚀**

For detailed information, see [docs/dev-ui-guide.md](docs/dev-ui-guide.md)
