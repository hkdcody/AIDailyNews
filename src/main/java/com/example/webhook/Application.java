package com.example.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}



//webhook-receiver/
//├── pom.xml (same as before)
//└── src/main/java/com/example/webhook/
//    ├── Application.java               # Main app with @EnableScheduling
//    ├── config/
//    │   └── WebhookConfig.java         # Configuration properties
//    ├── model/
//    │   └── WebhookResponse.java       # Response data model
//    ├── service/
//    │   └── WebhookService.java        # Webhook calling logic
//    ├── scheduler/
//    │   └── WebhookScheduler.java      # 9 AM daily trigger
//    └── controller/
//        └── DashboardController.java   # Web UI controller