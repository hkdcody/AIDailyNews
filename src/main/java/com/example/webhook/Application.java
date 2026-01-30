package com.example.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);

        // 从系统环境变量读取 PORT（Railway 会自动设置）
        String port = System.getenv("PORT");
        if (port != null && !port.isEmpty()) {
            Map<String, Object> props = new HashMap<>();
            props.put("server.port", port);
            app.setDefaultProperties(props);
            System.out.println(">>> Using PORT from environment: " + port);
        } else {
            System.out.println(">>> No PORT env var found, using default (8080)");
        }

        app.run(args);
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