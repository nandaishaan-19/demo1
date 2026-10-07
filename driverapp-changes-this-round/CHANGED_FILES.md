# Files changed in this round (on top of driverapp-main-final.zip)

A = new file, M = modified, D = deleted (remove it from your copy)

## Backend (springapp/)

- `M` springapp/pom.xml - adds modelmapper 3.1.1 and spring-boot-starter-aop
- `A` springapp/src/main/java/com/examly/springapp/aspect/LoggingAspect.java
- `A` springapp/src/main/java/com/examly/springapp/config/ClockConfig.java
- `A` springapp/src/main/java/com/examly/springapp/config/ModelMapperConfig.java
- `A` springapp/src/main/java/com/examly/springapp/controller/ActivityLogController.java
- `A` springapp/src/main/java/com/examly/springapp/dto/ActivityLogDTO.java
- `M` springapp/src/main/java/com/examly/springapp/dto/DriverRequestDTO.java
- `M` springapp/src/main/java/com/examly/springapp/dto/ErrorLogDTO.java
- `M` springapp/src/main/java/com/examly/springapp/dto/validation/ValidationPatterns.java
- `M` springapp/src/main/java/com/examly/springapp/exceptions/GlobalExceptionHandler.java
- `A` springapp/src/main/java/com/examly/springapp/logging/ActivityLogger.java
- `A` springapp/src/main/java/com/examly/springapp/logging/JiraLogClient.java
- `D` springapp/src/main/java/com/examly/springapp/mapper/DtoMapper.java
- `A` springapp/src/main/java/com/examly/springapp/model/ActivityLog.java
- `A` springapp/src/main/java/com/examly/springapp/repository/ActivityLogRepo.java
- `M` springapp/src/main/java/com/examly/springapp/service/AiService.java
- `M` springapp/src/main/java/com/examly/springapp/service/DriverRequestServiceImpl.java
- `M` springapp/src/main/java/com/examly/springapp/service/DriverServiceImpl.java
- `M` springapp/src/main/java/com/examly/springapp/service/FeedbackServiceImpl.java
- `M` springapp/src/main/java/com/examly/springapp/service/UserServiceImpl.java
- `M` springapp/src/main/resources/application.properties

## Frontend (angularapp/)

- `M` angularapp/src/app/app.module.ts
- `M` angularapp/src/app/components/admin-view-drivers/admin-view-drivers.component.html
- `M` angularapp/src/app/components/admin-view-drivers/admin-view-drivers.component.ts
- `M` angularapp/src/app/components/adminviewfeedback/adminviewfeedback.component.html
- `M` angularapp/src/app/components/adminviewfeedback/adminviewfeedback.component.ts
- `M` angularapp/src/app/components/adminviewrequests/adminviewrequests.component.css
- `M` angularapp/src/app/components/adminviewrequests/adminviewrequests.component.html
- `M` angularapp/src/app/components/adminviewrequests/adminviewrequests.component.ts
- `M` angularapp/src/app/components/customerviewdriver/customerviewdriver.component.css
- `M` angularapp/src/app/components/customerviewdriver/customerviewdriver.component.html
- `M` angularapp/src/app/components/customerviewdriver/customerviewdriver.component.ts
- `M` angularapp/src/app/components/customerviewfeedback/customerviewfeedback.component.html
- `M` angularapp/src/app/components/customerviewfeedback/customerviewfeedback.component.ts
- `M` angularapp/src/app/components/customerviewrequested/customerviewrequested.component.css
- `M` angularapp/src/app/components/customerviewrequested/customerviewrequested.component.html
- `M` angularapp/src/app/components/customerviewrequested/customerviewrequested.component.ts
- `A` angularapp/src/app/components/skeleton/skeleton.component.css
- `A` angularapp/src/app/components/skeleton/skeleton.component.html
- `A` angularapp/src/app/components/skeleton/skeleton.component.ts
- `M` angularapp/src/app/models/driver-request.model.ts
- `M` angularapp/src/app/utils/constants.ts
- `A` angularapp/src/app/utils/trip.ts
- `M` angularapp/src/styles.css

## Project root

- `M` README.md - new sections for the features of this round

