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

    // AI News: Run at 9 AM, 1 PM, and 6 PM every day
    @Scheduled(cron = "0 0 9,13,18 * * *")
    public void scheduledAINewsCall() {
        if (!webhookConfig.isSchedulerEnabled()) {
            logger.info("Scheduler is disabled. Skipping scheduled AI news call.");
            return;
        }

        logger.info("Scheduled AI news call started");
        WebhookResponse response = webhookService.callWebhook();
        logger.info("AI news call completed with status: {}", response.getStatusCode());
    }

    // Weixin News: Run at 9 AM every day
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledWeixinNewsCall() {
        if (!webhookConfig.isSchedulerEnabled()) {
            logger.info("Scheduler is disabled. Skipping scheduled Weixin news call.");
            return;
        }

        String weixinUrl = webhookConfig.getWeixin().getUrl();
        if (weixinUrl == null || weixinUrl.isEmpty()) {
            logger.warn("Weixin webhook URL is not configured. Skipping scheduled Weixin news call.");
            return;
        }

        logger.info("Scheduled Weixin news call started");
        WebhookResponse response = webhookService.callWeixinWebhook();
        logger.info("Weixin news call completed with status: {}", response.getStatusCode());
    }
}