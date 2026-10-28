# Project Setup

- Java 25 required
- Spring Boot 3.2
- Gradle for build and dependency management
- Thymeleaf template engine

# Build & Test

- `gradle assemble` - compile and package
- `gradle bootRun` - start dev server on port 3006
- `gradle test` - run unit tests
- `gradle checkstyleMain` - run checkstyle
- `./run.sh` - start dev server

# Code Style

- 4-space indentation
- One class per file, named to match the class
- Controller methods return template name strings
- Use `@GetMapping` annotations for route handlers

# Architecture

- `src/main/java/com/example/site/` - Java source root
- `src/main/java/.../SiteApplication.java` - Spring Boot entry point
- `src/main/java/.../controller/PageController.java` - Route handlers
- `src/main/resources/templates/` - Thymeleaf HTML templates
- `src/main/resources/static/css/style.css` - Global stylesheet
- `src/main/resources/application.properties` - App configuration
- `src/test/java/` - Test classes

# Gotchas

- Thymeleaf uses fragment-based layout: `th:replace="~{layout :: nav}"`
- Port is configured in `application.properties` (3006)
- The site brand is "RoboCare Health" (robotic healthcare company)
