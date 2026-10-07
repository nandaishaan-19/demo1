# DriveU - Group Project

DriveU is a driver-booking web application.

- **Frontend:** Angular (runs on port 8081)
- **Backend:** Spring Boot, Java 17, JWT security (runs on port 8080)
- **Roles:** Admin (manages drivers, requests and feedback) and Customer (browses drivers, books trips, posts feedback)

## How to run

Backend:

    cd springapp
    mvn spring-boot:run

Frontend:

    cd angularapp
    npm install
    ng serve --port 8081

Open http://localhost:8081 (the login page is the first page).

---

## Sign-in, sign-up and configuration

- **Predefined admin:** `admin@driveu.com` / `Admin@123` (override with `ADMIN_EMAIL`, `ADMIN_PASSWORD`, ... or `app.admin.*`). Change it before real use.
- **Sign-up** creates Customers only. Email and mobile number must each be verified with a 6-digit OTP (5 min expiry, 30 s resend wait, 5 wrong tries lock). Passwords need 8-64 chars with upper, lower, digit and special character; the same rule is checked in the browser and in the backend.
- **More admins:** log in as an admin and use the **Add Admin** button in the admin navbar (`POST /api/admin/register`, protected by `@PreAuthorize("hasRole('ADMIN')")`).
- **OTP delivery:** email goes over SMTP (`MAIL_USERNAME`, `MAIL_PASSWORD` = a Gmail app password); SMS uses Twilio (`TWILIO_*`, see `application.properties`). While neither is configured, `otp.dev-mode=true` prints the OTP in the server console.
- **Logins:** `JWT_SECRET` env var keeps tokens valid across restarts; if empty, a random secret is generated at each start and everyone must log in again.
- Request/response objects go through DTOs in `springapp/.../dto`. Entities and DTOs are converted by **ModelMapper** (dependency in `pom.xml`, bean in `config/ModelMapperConfig.java`, STRICT matching, the password is never copied into a `UserDTO`).
- **Trips:** a trip can be ended only after its start time (date + time slot) has been reached. An approved trip can be cancelled by the customer up to 24 hours before it starts (**Cancel Trip** button); after that the button is locked. Both rules are enforced again in the backend (`DriverRequestServiceImpl`, answer HTTP 400 with a message). Set `APP_TIME_ZONE` (e.g. `Asia/Kolkata`) if the server runs in another time zone than the users.
- **Available Drivers:** a driver the customer has booked shows **Awaiting Approval** while the request is pending and **Already Approved** once the admin approved it.
- **Admin > Requests:** the *Show* drop-down filters by All / Awaiting Approval / Upcoming / Ongoing / Trip Ended / Completed / Rejected / Cancelled (with counts).
- **Skeleton loaders** (`components/skeleton` + `.sk` classes in `styles.css`) are shown on the driver, request and feedback pages, the pay-amount pop-up and the AI review summary while data loads.
- **Logging (AOP):** `aspect/LoggingAspect.java` logs every controller / service call with `@Before`, `@After` and `@AfterThrowing` (no passwords, OTPs or request bodies). Destinations: console + `logs/driveu.log`, the `ActivityLogs` table (admins read it at `GET /api/admin/logs?limit=100`, or in the H2 console), and optionally **Jira**: set `JIRA_ENABLED=true`, `JIRA_BASE_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_PROJECT_KEY` to get every unexpected error as a Jira issue (`JIRA_EVENTS=ALL` + `JIRA_LOG_ISSUE_KEY` also adds each API call as a comment on one issue).
- `docs/` holds the team division PDF, UML diagrams, workflow document and code walkthrough. These were written before the DTO/OTP/admin changes, so the newer files (OtpController, AdminController, add-admin component, ...) are not listed in them.

---

## Team File Division (for evaluation)

Every member owns a mix of backend and frontend files. Each file belongs to exactly one member.

How the lines were counted:
- LOC = actual number of lines in the file.
- Not counted (already-prepared test setup): `karma/`, `junit/`, all `*.spec.ts` files, `karma.conf.js`, `test.ts`, `tsconfig.spec.json`, `SpringappApplicationTests.java`, puppeteer.
- Not counted (AI features): `AiController.java`, `AiService.java`, `GeminiService.java`, `ai.service.ts`, `ai.model.ts`.
- Not counted (generated / non-code): `package-lock.json`, `mvnw`, `mvnw.cmd`, `HELP.md`, images, `.gitignore`, `db.json`, CI files.
- Files marked `[AI-touch]` are normal files that also contain a small AI hook. They stay whole with their owner.

### Summary

| Member | Name | Main Module | Backend LOC | Frontend LOC | Total LOC |
|---|---|---|---|---|---|
| 1 | Aleena | Authentication | 326 | 450 | 776 |
| 2 | Vanshika | Security & App Shell | 381 | 411 | 792 |
| 3 | Arjun | Driver Management | 279 | 568 | 847 |
| 4 | Prajan | Customer Browsing & Booking | 266 | 500 | 766 |
| 5 | Visalini | Request Tracking & Errors | 150 | 615 | 765 |
| 6 | Saumya | Feedback | 260 | 596 | 856 |
| 7 | Aarthi | Setup, Styling & Error Logs | 120 | 635 | 755 |
| | **Total** | | **1782** | **3775** | **5557** |

---

### Member 1 - Aleena

**Focus:** User Authentication (Login / Signup / JWT login flow)

**Backend files**

- `springapp/src/main/java/com/examly/springapp/controller/AuthController.java` - 58
- `springapp/src/main/java/com/examly/springapp/model/User.java` - 79
- `springapp/src/main/java/com/examly/springapp/model/LoginDTO.java` - 50
- `springapp/src/main/java/com/examly/springapp/repository/UserRepo.java` - 12
- `springapp/src/main/java/com/examly/springapp/service/UserService.java` - 8
- `springapp/src/main/java/com/examly/springapp/service/UserServiceImpl.java` - 35
- `springapp/pom.xml` - 84

**Frontend files**

- `angularapp/src/app/components/login/login.component.ts` - 60
- `angularapp/src/app/components/login/login.component.html` - 38
- `angularapp/src/app/components/login/login.component.css` - 2
- `angularapp/src/app/components/signup/signup.component.ts` - 82
- `angularapp/src/app/components/signup/signup.component.html` - 75
- `angularapp/src/app/components/signup/signup.component.css` - 2
- `angularapp/src/app/models/login.model.ts` - 4
- `angularapp/src/app/models/user.model.ts` - 8
- `angularapp/src/app/services/auth.service.ts` - 107
- `angularapp/src/app/components/authguard/authguard.guard.ts` - 28
- `angularapp/src/app/services/http-error.interceptor.ts` - 35
- `angularapp/src/apiconfig.ts` - 4
- `angularapp/src/environments/environment.ts` - 5

**Total: 776 LOC** (Backend 326 + Frontend 450)

---

### Member 2 - Vanshika

**Focus:** Security, Navigation & App Shell (Spring Security/JWT filters, role-based navbars, routing, home page)

**Backend files**

- `springapp/src/main/java/com/examly/springapp/config/SecurityConfig.java` - 107 [AI-touch]
- `springapp/src/main/java/com/examly/springapp/config/JwtAuthenticationFilter.java` - 64
- `springapp/src/main/java/com/examly/springapp/config/JwtUtils.java` - 46
- `springapp/src/main/java/com/examly/springapp/config/UserPrinciple.java` - 80
- `springapp/src/main/java/com/examly/springapp/config/MyUserDetailsService.java` - 26
- `springapp/src/main/java/com/examly/springapp/config/JwtAccessDeniedHandler.java` - 20
- `springapp/src/main/java/com/examly/springapp/config/JwtAuthenticationEntryPoint.java` - 20
- `springapp/src/main/java/com/examly/springapp/config/CrosConfig.java` - 18

**Frontend files**

- `angularapp/src/app/app.component.ts` - 21
- `angularapp/src/app/app.component.html` - 3
- `angularapp/src/app/app.component.css` - 0
- `angularapp/src/app/app.module.ts` - 56
- `angularapp/src/app/app-routing.module.ts` - 52
- `angularapp/src/app/components/adminnav/adminnav.component.ts` - 42
- `angularapp/src/app/components/adminnav/adminnav.component.html` - 45
- `angularapp/src/app/components/adminnav/adminnav.component.css` - 3
- `angularapp/src/app/components/customernav/customernav.component.ts` - 42
- `angularapp/src/app/components/customernav/customernav.component.html` - 26
- `angularapp/src/app/components/customernav/customernav.component.css` - 3
- `angularapp/src/app/components/home-page/home-page.component.ts` - 11
- `angularapp/src/app/components/home-page/home-page.component.html` - 25
- `angularapp/src/app/components/home-page/home-page.component.css` - 53
- `angularapp/src/app/utils/driver-image.ts` - 29

**Total: 792 LOC** (Backend 381 + Frontend 411)

---

### Member 3 - Arjun

**Focus:** Driver Management (admin add / edit / view / delete drivers)

**Backend files**

- `springapp/src/main/java/com/examly/springapp/controller/DriverController.java` - 65
- `springapp/src/main/java/com/examly/springapp/model/Driver.java` - 106
- `springapp/src/main/java/com/examly/springapp/repository/DriverRepo.java` - 11
- `springapp/src/main/java/com/examly/springapp/service/DriverService.java` - 13
- `springapp/src/main/java/com/examly/springapp/service/DriverServiceImpl.java` - 70
- `springapp/src/main/java/com/examly/springapp/exceptions/DriverDeletionException.java` - 7
- `springapp/src/main/java/com/examly/springapp/exceptions/DuplicateDriverException.java` - 7

**Frontend files**

- `angularapp/src/app/components/driver-management/driver-management.component.ts` - 183
- `angularapp/src/app/components/driver-management/driver-management.component.html` - 70
- `angularapp/src/app/components/driver-management/driver-management.component.css` - 13
- `angularapp/src/app/components/admin-view-drivers/admin-view-drivers.component.ts` - 129
- `angularapp/src/app/components/admin-view-drivers/admin-view-drivers.component.html` - 63
- `angularapp/src/app/components/admin-view-drivers/admin-view-drivers.component.css` - 59
- `angularapp/src/app/models/driver.model.ts` - 12
- `angularapp/src/app/services/driver.service.ts` - 39

**Total: 847 LOC** (Backend 279 + Frontend 568)

---

### Member 4 - Prajan

**Focus:** Customer Driver Browsing & Request Booking (data model, persistence and service layer of driver requests)

**Backend files**

- `springapp/src/main/java/com/examly/springapp/model/DriverRequest.java` - 156
- `springapp/src/main/java/com/examly/springapp/repository/DriverRequestRepo.java` - 13
- `springapp/src/main/java/com/examly/springapp/service/DriverRequestService.java` - 15
- `springapp/src/main/java/com/examly/springapp/service/DriverRequestServiceImpl.java` - 82

**Frontend files**

- `angularapp/src/app/components/customerviewdriver/customerviewdriver.component.ts` - 126 [AI-touch]
- `angularapp/src/app/components/customerviewdriver/customerviewdriver.component.html` - 45 [AI-touch]
- `angularapp/src/app/components/customerviewdriver/customerviewdriver.component.css` - 24
- `angularapp/src/app/components/customer-request/customer-request.component.ts` - 138
- `angularapp/src/app/components/customer-request/customer-request.component.html` - 45
- `angularapp/src/app/components/customer-request/customer-request.component.css` - 2
- `angularapp/src/app/utils/format.ts` - 86
- `angularapp/tsconfig.json` - 20
- `angularapp/tsconfig.app.json` - 14

**Total: 766 LOC** (Backend 266 + Frontend 500)

---

### Member 5 - Visalini

**Focus:** Request Tracking & Trip Lifecycle (approve / reject / trip end / pay amount, request API & global error handling)

**Backend files**

- `springapp/src/main/java/com/examly/springapp/controller/DriverRequestController.java` - 83
- `springapp/src/main/java/com/examly/springapp/exceptions/DriverRequestDeletionException.java` - 7
- `springapp/src/main/java/com/examly/springapp/exceptions/GlobalExceptionHandler.java` - 60

**Frontend files**

- `angularapp/src/app/components/customerviewrequested/customerviewrequested.component.ts` - 184
- `angularapp/src/app/components/customerviewrequested/customerviewrequested.component.html` - 86
- `angularapp/src/app/components/customerviewrequested/customerviewrequested.component.css` - 17
- `angularapp/src/app/components/adminviewrequests/adminviewrequests.component.ts` - 135
- `angularapp/src/app/components/adminviewrequests/adminviewrequests.component.html` - 83
- `angularapp/src/app/components/adminviewrequests/adminviewrequests.component.css` - 21
- `angularapp/src/app/components/error/error.component.ts` - 11
- `angularapp/src/app/components/error/error.component.html` - 4
- `angularapp/src/app/components/error/error.component.css` - 3
- `angularapp/src/app/models/driver-request.model.ts` - 24
- `angularapp/src/app/services/driver-request.service.ts` - 47

**Total: 765 LOC** (Backend 150 + Frontend 615)

---

### Member 6 - Saumya

**Focus:** Feedback Module (customer post / view / delete feedback, admin feedback review)

**Backend files**

- `springapp/src/main/java/com/examly/springapp/controller/FeedbackController.java` - 64
- `springapp/src/main/java/com/examly/springapp/model/Feedback.java` - 112
- `springapp/src/main/java/com/examly/springapp/repository/FeedbackRepo.java` - 13
- `springapp/src/main/java/com/examly/springapp/service/FeedbackService.java` - 12
- `springapp/src/main/java/com/examly/springapp/service/FeedbackServiceImpl.java` - 59 [AI-touch]

**Frontend files**

- `angularapp/src/app/components/customerpostfeedback/customerpostfeedback.component.ts` - 91
- `angularapp/src/app/components/customerpostfeedback/customerpostfeedback.component.html` - 36
- `angularapp/src/app/components/customerpostfeedback/customerpostfeedback.component.css` - 4
- `angularapp/src/app/components/customerviewfeedback/customerviewfeedback.component.ts` - 94
- `angularapp/src/app/components/customerviewfeedback/customerviewfeedback.component.html` - 56
- `angularapp/src/app/components/customerviewfeedback/customerviewfeedback.component.css` - 13
- `angularapp/src/app/components/adminviewfeedback/adminviewfeedback.component.ts` - 121 [AI-touch]
- `angularapp/src/app/components/adminviewfeedback/adminviewfeedback.component.html` - 102 [AI-touch]
- `angularapp/src/app/components/adminviewfeedback/adminviewfeedback.component.css` - 23
- `angularapp/src/app/models/feedback.model.ts` - 21
- `angularapp/src/app/services/feedback.service.ts` - 35

**Total: 856 LOC** (Backend 260 + Frontend 596)

---

### Member 7 - Aarthi

**Focus:** Project Setup, Global Styling & Error Logging (Spring Boot bootstrap/config, ErrorLogs table, global CSS, Angular build config)

**Backend files**

- `springapp/src/main/java/com/examly/springapp/SpringappApplication.java` - 14
- `springapp/src/main/java/com/examly/springapp/model/ErrorLog.java` - 81
- `springapp/src/main/java/com/examly/springapp/repository/ErrorLogRepo.java` - 9
- `springapp/src/main/resources/application.properties` - 16 [AI-touch]

**Frontend files**

- `angularapp/src/styles.css` - 384
- `angularapp/src/app/utils/constants.ts` - 21
- `angularapp/src/index.html` - 13
- `angularapp/src/main.ts` - 7
- `angularapp/src/polyfills.ts` - 63
- `angularapp/angular.json` - 107
- `angularapp/package.json` - 40

**Total: 755 LOC** (Backend 120 + Frontend 635)

---
