package com.example.webhook.controller;

import com.example.webhook.model.WebhookResponse;
import com.example.webhook.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final WebhookService webhookService;

    public DashboardController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping("/")
    public String index(Model model) {
        try {
            logger.info("Dashboard index page accessed");
            List<WebhookResponse> history = new ArrayList<>(webhookService.getHistory());
            // Don't reverse - latest should be first (at top)
            logger.debug("Displaying {} webhook responses", history.size());
            model.addAttribute("responses", history);
            return "index";
        } catch (Exception e) {
            logger.error("Error loading dashboard", e);
            model.addAttribute("responses", new ArrayList<>());
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "index";
        }
    }

    @PostMapping("/api/trigger")
    @ResponseBody
    public WebhookResponse manualTrigger() {
        try {
            logger.info("Manual webhook trigger requested");
            WebhookResponse response = webhookService.callWebhook();
            logger.info("Manual trigger completed with status: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            logger.error("Error in manual trigger", e);
            WebhookResponse errorResponse = new WebhookResponse();
            errorResponse.setStatusCode(500);
            errorResponse.setError("Trigger error: " + e.getMessage());
            return errorResponse;
        }
    }

    @PostMapping("/api/trigger/weixin")
    @ResponseBody
    public WebhookResponse manualTriggerWeixin() {
        try {
            logger.info("Manual Weixin webhook trigger requested");
            WebhookResponse response = webhookService.callWeixinWebhook();
            logger.info("Manual Weixin trigger completed with status: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            logger.error("Error in manual Weixin trigger", e);
            WebhookResponse errorResponse = new WebhookResponse();
            errorResponse.setStatusCode(500);
            errorResponse.setError("Trigger error: " + e.getMessage());
            return errorResponse;
        }
    }

    @GetMapping("/api/data")
    @ResponseBody
    public List<WebhookResponse> getData() {
        try {
            logger.debug("API data request");
            List<WebhookResponse> history = new ArrayList<>(webhookService.getHistory());
            return history;
        } catch (Exception e) {
            logger.error("Error getting data", e);
            return new ArrayList<>();
        }
    }

    @GetMapping("/api/latest")
    @ResponseBody
    public WebhookResponse getLatestResponse() {
        List<WebhookResponse> history = new ArrayList<>(webhookService.getHistory());
        if (history.isEmpty()) {
            return null;
        }
        // ✅ 取最后一个元素（最新）
        return history.get(history.size() - 1);
    }

    @DeleteMapping("/api/clear")
    @ResponseBody
    public Map<String, String> clearHistory() {
        try {
            logger.info("Clear history requested");
            webhookService.clearHistory();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "History cleared");
            return response;
        } catch (Exception e) {
            logger.error("Error clearing history", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return response;
        }
    }
}