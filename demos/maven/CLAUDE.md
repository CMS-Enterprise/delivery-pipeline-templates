# Project Setup

- Java 17+ required
- Spring Boot 3.2
- Maven for build and dependency management
- Thymeleaf template engine

# Build & Test

- `mvn clean package` - compile and package
- `mvn spring-boot:run` - start dev server on port 3004
- `mvn test` - run unit tests
- `./run.sh` - build and start dev server

# Code Style

- 4-space indentation
- One class per file, named to match the class
- Controller methods return template name strings
- Use `@GetMapping` annotations for route handlers
- Model attributes set via `Model.addAttribute()`

# Architecture

- `src/main/java/com/example/site/` - Java source root
- `src/main/java/.../SiteApplication.java` - Spring Boot entry point
- `src/main/java/.../controller/PageController.java` - Route handlers
- `src/main/resources/templates/` - Thymeleaf HTML templates
- `src/main/resources/templates/layout.html` - Shared layout fragments (head, nav, footer)
- `src/main/resources/static/css/style.css` - Global stylesheet
- `src/main/resources/application.properties` - App configuration
- `src/test/java/` - Test classes

# Workflow

- Create a feature branch: `git checkout -b feature/your-feature-name`
- Run `mvn test` before committing
- Include the user prompt in commit messages
- Write tests alongside implementation

# Gotchas

- Thymeleaf uses fragment-based layout: `th:replace="~{layout :: nav}"`
- Template caching is disabled (`spring.thymeleaf.cache=false`) for dev hot-reload
- Port is configured in `application.properties` (currently 3004)
- The site brand is "RoboCare Health" (robotic healthcare company)
