# File Summaries

This file contains summaries of important files in the repository.

## File Path: `pom.xml`

**Description:** This is a Maven Project Object Model (POM) file. It defines the project structure, dependencies, and build configuration for a Java project named "jinx". The project is a multi-module project, including modules like `jinx-service`, `jinx-starter`, `jinx-client`, `jinx-ui`, and `jinx-api`. It uses Spring Boot and Java 21. It also defines various dependencies like `aliyun-sdk-oss`, `lombok`, `openai4j`, `mysql-connector-j`, `mybatis-plus-boot-starter`, etc. The file also includes configurations for plugins like `maven-compiler-plugin` and `spring-boot-maven-plugin`.

## File Path: `README.md`

**Description:** This file serves as a guide for developers. It outlines constraints for writing controller layer code, such as using POST requests with request objects for parameters, limiting GET requests to two parameters, and placing business logic in the service layer. It references `specifications.md` for further logic guidelines. The README also details several common modules available in the project:
    *   **CommonRangeInterface:** For managing scope, collaboration, and task assignments across different business modules like courses, training, and learning maps.
    *   **User Management/Collaboration Permission Check:** Also part of `CommonRangeInterface`, it provides methods to check user rights for specific functionalities and business modules.
    *   **Authentication:** Points to `jinx/jinx-client/src/main/java/com/learn/controller/auth/RbacController.java` for RBAC implementation details.
    *   **File Module:** Describes file upload functionality, with the backend endpoint at `com.learn.controller.file.FileController#uploadFile` (`/api/file/upload`).
    *   **Organization, User, and Role Modules:** Points to respective controllers under `jinx-client/src/main/java/com/learn/controller/` for managing departments, users, and roles.
It emphasizes using existing common module services and avoiding mock data or skipping business logic.

## Directory Path: `jinx`

**Description:** This is a directory, not a regular file. Its contents will be summarized in a separate step if required.

## File Path: `jinx-api/pom.xml`

**Description:** This POM file defines the `jinx-api` module, which is a sub-module of the main `jinx` project. It includes dependencies such as `lombok` (for boilerplate code reduction), `javax.validation:validation-api` (for data validation annotations), and Jackson libraries (`jackson-databind`, `jackson-annotations`) for JSON serialization and deserialization. This module likely contains the API definitions, data transfer objects (DTOs), and other shared data structures used across the Jinx application.

## File Path: `jinx-client/pom.xml`

**Description:** This POM file defines the `jinx-client` module, a sub-module of the `jinx` project. It has a key dependency on `spring-boot-starter-web`, suggesting it's responsible for handling web-layer functionalities, such as REST controllers. It also depends on the `jinx-service` module (for business logic) and `jinx-api` module (for data transfer objects and API contracts). Other dependencies include `lombok`, `jakarta.servlet-api`, and `javax.validation:validation-api`. This module acts as the client-facing part of the application, processing incoming requests and interacting with the service layer.

## File Path: `jinx-service/pom.xml`

**Description:** This POM file defines the `jinx-service` module, which forms the core business logic layer of the `jinx` application. It has an extensive list of dependencies, indicating its wide range of responsibilities. Key dependencies include:
    *   `jinx-api` (for using shared DTOs and API contracts).
    *   Data access: `mybatis-plus-boot-starter`, `mysql-connector-j`.
    *   External service integrations: `aliyun-sdk-oss` (Alibaba Cloud Storage), `openai4j` (OpenAI), various Alibaba DingTalk SDKs, `schedulerx2-spring-boot-starter` (distributed tasks), and Alibaba MOS components for messaging, caching, and configuration.
    *   Spring Boot starters: `spring-boot-starter-websocket`, `spring-boot-starter-web`.
    *   Security: `jjwt` for JSON Web Token handling.
    *   Utilities: `lombok`, `commons-collections4`, `fastjson`, `commons-text`, `guava`.
This module likely contains service implementations, business logic, data manipulation, and integrations with third-party systems.

## File Path: `jinx-starter/pom.xml`

**Description:** This POM file defines the `jinx-starter` module. This module appears to be the main entry point for the Jinx application, responsible for assembling and running the application. It depends on other key modules: `jinx-service` (business logic), `jinx-client` (web controllers), and `jinx-ui` (user interface components). It includes `spring-boot-starter-web` for web application capabilities and `mysql-connector-j` and `sqlite-jdbc` for database connectivity. The `spring-boot-maven-plugin` suggests it builds the executable application. A `maven-antrun-plugin` configuration to copy static resources indicates it also packages frontend assets, making it the runnable application bundle.

## File Path: `jinx-ui/package.json`

**Description:** This is the standard `package.json` file for the `jinx-ui` frontend module, which is a Vue.js application. It defines the project's metadata (name, version), scripts for development (`serve`, `build`, `lint`), and its dependencies. Key dependencies include Vue.js itself (`vue`), `vue-router` for navigation, `ant-design-vue` as the UI component library, `axios` for HTTP requests, `echarts` for visualizations, and `dingtalk-jsapi` for DingTalk integration. Development dependencies include `@vue/cli-service`, ESLint, Babel, and LESS for CSS preprocessing. This file is central to managing the UI project's build process and dependencies.

## File Path: `jinx-ui/vue.config.js`

**Description:** This is the Vue CLI configuration file for the `jinx-ui` project. It customizes the build and development server settings. Key configurations include:
    *   Setting the `outputDir` to `../starter/src/main/resources/static`, which means the compiled frontend assets are placed directly into the `jinx-starter` module's static resources, allowing the Spring Boot application to serve them.
    *   Configuring a development server proxy to forward API requests starting with `/api` to `http://localhost:8088` (likely the backend server address during development).
    *   Customizing webpack's SVG handling using `chainWebpack` to create an SVG sprite from icons located in `src/assets/icons`.
    *   Disabling `lintOnSave` for faster development iteration.
This file is critical for integrating the frontend build with the Java backend and for tailoring the development environment.

## File Path: `jinx-ui/README.md`

**Description:** This README file provides basic instructions for the `jinx-ui` Vue.js application, titled "Hello 应用前端". It outlines standard commands for project setup (`npm install`), running in a development environment with hot-reloading (`npm run serve`), building for production (`npm run build`), and linting code (`npm run lint`). It also includes simple usage instructions: ensure the backend is running, start the frontend, and access it at `http://localhost:8081`. This file serves as a quick start guide for developers working on the UI.

## File Path: `jinx-ui/pom.xml`

**Description:** This Maven POM file is for the `jinx-ui` module. Although `jinx-ui` is a Vue.js frontend project, this POM integrates its build process with the overall Maven build of the `jinx` application. It uses the `exec-maven-plugin` to execute `npm install` and `npm run build` during the `generate-resources` phase, effectively building the frontend application using Node.js tools. Subsequently, it uses the `maven-antrun-plugin` to copy the built frontend assets from the output directory (configured in `vue.config.js` and referenced here, likely `../starter/src/main/resources/static` relative to `jinx-ui`) into the `jinx-starter` module's static resources directory (`jinx-starter/src/main/resources/static`). This ensures that the compiled UI is included in the final runnable application packaged by `jinx-starter`.

## File Path: `jinx-service/src/main/java/com/learn/service/user/UserService.java`

**Description:** This Java interface defines the contract for user management services within the `jinx-service` module. It outlines methods for creating single and batch users (`createUser`, `batchCreateUsers`), querying users and administrators (`queryUsers`, `queryAdministrators`), and deleting users (`deleteUser`). The interface specifies business rules, such as checking for existing users via third-party relations and requirements for `user_id` generation (random, unique, length constraints). It interacts with `user` and `user_third_party` database tables and utilizes various Data Transfer Objects (DTOs) for requests and responses.

## File Path: `jinx-service/src/main/java/com/learn/service/course/CourseQueryService.java`

**Description:** This Java interface defines the contract for querying course-related information within the `jinx-service` module. It provides methods to fetch a list of courses (`getCourseList`) based on specified criteria, retrieve detailed information for a single course (`getCourseDetail`) by its ID, and get statistical data for a course (`getCourseStatistics`). The interface uses various Data Transfer Objects (DTOs) from the `com.learn.dto.course` package for requests and responses. (Note: This file was chosen as a representative for "CourseService.java" as a general one was not found).

## File Path: `jinx-service/src/main/java/com/learn/service/order/OrderService.java` (Not Found)

**Description:** The specified file `jinx-service/src/main/java/com/jinx/service/order/OrderService.java` (or a similar path under `com/learn/service/order/`) was not found in the repository during the directory listing.

## File Path: `department_fulltext_index.sql`

**Description:** This SQL script provides instructions for enhancing search performance on hierarchical department data, specifically targeting the `department_path` column in the `department` table (and a similar `path` column in the `category` table). It details steps to:
    1.  Ensure the `department_path` column is suitable for full-text indexing.
    2.  Create a full-text index (`idx_department_path_fulltext`) on `department(department_path)` and `idx_path_fulltext` on `category(path)`.
    3.  Add standard B-tree indexes to frequently queried columns.
    4.  Provides an example of an optimized query using `MATCH ... AGAINST ... IN BOOLEAN MODE`.
    5.  Briefly mentions relevant MySQL full-text search configuration options.

## File Path: `specifications.md`

**Description:** This document details the coding specifications and best practices to be followed across the Jinx project. It covers:
    *   **Database Operations:** Emphasizes using batch operations efficiently and ensuring MyBatis-Plus XML mappers primarily target their respective tables, returning entity objects.
    *   **API Definitions:** Mandates that Java methods with more than four parameters must use a request object.
    *   **Exception Handling:** Prohibits generic `try-catch` blocks for predictable errors; instead, custom `CommonException` should be thrown.
    *   **Constants:** Forbids magic numbers/strings, requiring the use of defined constants or enums.
    *   **General Rules:** Limits GET requests to two parameters and stresses that all code must be production-ready.
The document provides clear examples for each guideline.

# Project Modules

This section describes the main modules of the Jinx project.

## `jinx-api`

*   **Purpose:** Defines the data transfer objects (DTOs), validation annotations, and API contracts (interfaces) used by other modules for communication. Ensures consistent data structures across the application.
*   **Primary Directories/Files:** `src/main/java` (containing DTOs, validation annotations, and Java interfaces for API contracts), `pom.xml`.

## `jinx-client`

*   **Purpose:** Handles incoming web requests (likely RESTful APIs), acting as the primary interface for external clients (including the `jinx-ui` frontend). It orchestrates calls to `jinx-service` for business logic execution and uses DTOs from `jinx-api`.
*   **Primary Directories/Files:** `src/main/java` (containing Spring MVC controllers that define API endpoints), `pom.xml`.

## `jinx-service`

*   **Purpose:** Implements the core business logic of the application. It interacts with the database (via mappers and entities), integrates with external services (like cloud storage, AI services, messaging systems), and provides services consumed by `jinx-client`.
*   **Primary Directories/Files:** `src/main/java` (containing service interfaces, implementations, database mappers, entities, and utility classes), `src/main/resources` (containing MyBatis XML mappers, configuration files like `application.properties`), `pom.xml`.

## `jinx-starter`

*   **Purpose:** Acts as the main entry point for the application. It assembles all other modules (`jinx-api`, `jinx-client`, `jinx-service`, `jinx-ui`) into a runnable Spring Boot application. It's responsible for packaging the final application and potentially managing application-level configurations.
*   **Primary Directories/Files:** `src/main/java` (containing the main Spring Boot application class, e.g., `JinxApplication.java`), `src/main/resources/static` (this is where compiled `jinx-ui` frontend assets are placed to be served by the Spring Boot application), `pom.xml`.

## `jinx-ui`

*   **Purpose:** Provides the frontend user interface for the application. It's a Vue.js single-page application (SPA) that interacts with the backend APIs (exposed by `jinx-client`) to display data and allow user interaction.
*   **Primary Directories/Files:** `src` (containing Vue components, JavaScript/TypeScript files, LESS/CSS styles, and assets like images/icons), `public` (for static assets like `index.html`), `package.json` (manages Node.js dependencies and scripts like `serve`, `build`), `vue.config.js` (Vue CLI project configuration, including build output paths and dev server settings), `pom.xml` (integrates the frontend build into the main Maven build process).
