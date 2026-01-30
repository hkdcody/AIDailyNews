package com.example.webhook.service;

import com.example.webhook.config.WebhookConfig;
import com.example.webhook.model.WebhookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private final WebhookConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Queue<WebhookResponse> responseHistory;
    private static final int MAX_HISTORY = 100;

    public WebhookService(WebhookConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
        this.responseHistory = new ConcurrentLinkedQueue<>();
        logger.info("WebhookService initialized with URL: {}", config.getUrl());
    }

    public WebhookResponse callWebhook() {
        return callWebhookInternal(config.getUrl(), config.getMethod(), config.getTimeoutSeconds());
    }

    public WebhookResponse callWeixinWebhook() {
        String url = config.getWeixin().getUrl();
        if (url == null || url.isEmpty()) {
            logger.warn("Weixin webhook URL is not configured");
            WebhookResponse errorResponse = new WebhookResponse();
            errorResponse.setStatusCode(400);
            errorResponse.setStatusMessage("Error");
            errorResponse.setError("Weixin webhook URL is not configured in application.properties");
            addToHistory(errorResponse);
            return errorResponse;
        }
        return callWebhookInternal(url, config.getWeixin().getMethod(), config.getWeixin().getTimeoutSeconds());
    }

    private WebhookResponse callWebhookInternal(String webhookUrl, String method, int timeout) {
        WebhookResponse response = new WebhookResponse();

        try {
            logger.info("Starting webhook call to: {}", webhookUrl);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(timeout));

            HttpRequest request;
            if ("POST".equalsIgnoreCase(method)) {
                request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .header("Content-Type", "application/json")
                        .build();
                logger.debug("Using POST method");
            } else {
                request = requestBuilder.GET().build();
                logger.debug("Using GET method");
            }

            HttpResponse<String> httpResponse = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            logger.info("Received response with status code: {}", httpResponse.statusCode());
            logger.debug("Response body length: {} characters", httpResponse.body().length());
            logger.debug("Response body: {}", httpResponse.body());

            response.setStatusCode(httpResponse.statusCode());
            response.setStatusMessage("Success");

            try {
                Object parsedData = objectMapper.readValue(httpResponse.body(), Object.class);
                logger.debug("Successfully parsed JSON response");

                Map<String, Object> extractedData = extractOutputContent(parsedData);
                logger.info("Extracted data - has title: {}, has content: {}",
                        extractedData.containsKey("title"),
                        extractedData.containsKey("content"));

                response.setData(extractedData);
            } catch (Exception e) {
                logger.error("Error parsing JSON response", e);
                Map<String, Object> textData = Map.of("response", httpResponse.body());
                response.setData(textData);
            }

        } catch (Exception e) {
            logger.error("Error calling webhook: {}", e.getMessage(), e);
            response.setStatusCode(500);
            response.setStatusMessage("Error");
            response.setError(e.getMessage());
        }

        addToHistory(response);
        logger.info("Webhook call completed, total history size: {}", responseHistory.size());
        return response;
    }

    private Map<String, Object> extractOutputContent(Object data) {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.debug("Extracting output content from data type: {}", data.getClass().getSimpleName());

            if (data instanceof List) {
                List<?> list = (List<?>) data;
                logger.debug("Data is a List with {} items", list.size());

                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> firstItem = (Map<String, Object>) list.get(0);
                    logger.debug("First item keys: {}", firstItem.keySet());

                    if (firstItem.containsKey("output")) {
                        result.put("content", firstItem.get("output"));
                        logger.debug("Found 'output' field");

                        // Extract title from dingtalkPayload.markdown.title
                        if (firstItem.containsKey("dingtalkPayload")) {
                            logger.debug("Found 'dingtalkPayload' field");

                            @SuppressWarnings("unchecked")
                            Map<String, Object> dingtalk = (Map<String, Object>) firstItem.get("dingtalkPayload");

                            if (dingtalk != null && dingtalk.containsKey("markdown")) {
                                logger.debug("Found 'markdown' field in dingtalkPayload");

                                @SuppressWarnings("unchecked")
                                Map<String, Object> markdown = (Map<String, Object>) dingtalk.get("markdown");

                                if (markdown != null && markdown.containsKey("title")) {
                                    Object titleObj = markdown.get("title");
                                    if (titleObj != null) {
                                        String title = titleObj.toString();
                                        result.put("title", title);
                                        logger.info("Extracted title: {}", title);
                                    }
                                } else {
                                    logger.warn("No 'title' field found in markdown object");
                                }
                            } else {
                                logger.warn("No 'markdown' field found in dingtalkPayload");
                            }
                        } else {
                            logger.warn("No 'dingtalkPayload' field found");
                        }

                        return result;
                    } else {
                        logger.warn("No 'output' field found in first item");
                    }
                }
            } else if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) data;
                logger.debug("Data is a Map with keys: {}", map.keySet());

                if (map.containsKey("output")) {
                    result.put("content", map.get("output"));
                    logger.debug("Found 'output' field in map");
                    return result;
                }
                return map;
            }

            logger.warn("Could not extract output content, returning raw data");
            result.put("raw_data", data);
            return result;

        } catch (Exception e) {
            logger.error("Error extracting output content", e);
            result.put("parse_error", "Unable to extract content: " + e.getMessage());
            result.put("raw_data", data.toString());
            return result;
        }
    }

    private void addToHistory(WebhookResponse response) {
        responseHistory.offer(response);
        while (responseHistory.size() > MAX_HISTORY) {
            responseHistory.poll();
        }
    }

    public Queue<WebhookResponse> getHistory() {
        return responseHistory;
    }

    public void clearHistory() {
        logger.info("Clearing webhook history");
        responseHistory.clear();
    }
}
