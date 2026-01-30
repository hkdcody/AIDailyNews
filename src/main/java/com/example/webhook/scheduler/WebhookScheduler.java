package com.example.webhook.scheduler;

import com.example.webhook.config.WebhookConfig;
import com.example.webhook.model.WebhookResponse;
import com.example.webhook.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WebhookScheduler.class);
    private final WebhookService webhookService;
    private final WebhookConfig webhookConfig;

    public WebhookScheduler(WebhookService webhookService, WebhookConfig webhookConfig) {
        this.webhookService = webhookService;
        this.webhookConfig = webhookConfig;
    }

    // Run at 9 AM every day
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledWebhookCall() {
        if (!webhookConfig.isSchedulerEnabled()) {
            logger.info("Scheduler is disabled. Skipping scheduled webhook call.");
            return;
        }

        logger.info("Scheduled webhook call started at 9 AM");
        WebhookResponse response = webhookService.callWebhook();
        logger.info("Webhook call completed with status: {}", response.getStatusCode());
    }
}