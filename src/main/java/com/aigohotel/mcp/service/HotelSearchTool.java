package com.aigohotel.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import com.aigohotel.mcp.config.AigoHotelProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HotelSearchTool {
    private static final String API_BASE_URL = "https://travelportal-api-staging.aigohotel.com/api/mcp/hotelsearch";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final AigoHotelProperties properties;

    public HotelSearchTool(AigoHotelProperties properties) {
        this.properties = properties;
    }

    @McpTool(
            name = "search_hotels",
            description = "查询海外酒店信息。支持按指定的地点类型查询酒店。",
            generateOutputSchema = true
    )
    public McpSchema.CallToolResult searchHotels(
            @McpToolParam(description = "地点名称(支持城市、景点、酒店、交通枢纽、地标和具体地址等)", required = true)
            String place,
            @McpToolParam(description = "地点的类型(支持以下类型: 城市、区/县、机场、景点、火车站、地铁站、酒店、具体地址)", required = true)
            String placeType,
            @McpToolParam(description = "用户的原始问询句", required = false)
            String originalQuery,
            @McpToolParam(description = "入住日期,如: 2025-10-01,未填写时默认为次日", required = false)
            String checkIn,
            @McpToolParam(description = "入住天数,未填写时默认为1天", required = false)
            Integer stayNights,
            @McpToolParam(description = "酒店星级(0.0-5.0,梯度为0.5),默认3星以上,例如[4.5, 5.0]、[0.0, 2.0]", required = false)
            List<Double> starRatings,
            @McpToolParam(description = "每间房入住的成人数量,默认两成人", required = false)
            Integer adultCount,
            @McpToolParam(description = "直线距离,单位(米),当地点是一个POI位置时生效,生效时默认值为5000", required = false)
            Integer distanceInMeter,
            @McpToolParam(description = "返回酒店结果数量,默认10个酒店,最大不超过20个", required = false)
            Integer size,
            @McpToolParam(description = "是否包含酒店设施", required = false)
            Boolean withHotelAmenities,
            @McpToolParam(description = "是否包含客房设施", required = false)
            Boolean withRoomAmenities,
            @McpToolParam(description = "当前语言环境,如: zh_CN, en_US等,默认zh_CN", required = false)
            String language,
            @McpToolParam(description = "是否对用户的提问询语句进行分析得到用户的需求倾向性,默认为true", required = false)
            Boolean queryParsing
    ) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("place", place);
        params.put("placeType", placeType);
        params.put("stayNights", stayNights == null ? 1 : stayNights);
        params.put("adultCount", adultCount == null ? 2 : adultCount);
        params.put("distanceInMeter", distanceInMeter == null ? 5000 : distanceInMeter);
        params.put("size", size == null ? 10 : size);
        params.put("withHotelAmenities", withHotelAmenities == null ? Boolean.TRUE : withHotelAmenities);
        params.put("withRoomAmenities", withRoomAmenities == null ? Boolean.TRUE : withRoomAmenities);
        params.put("language", (language == null || language.isBlank()) ? "zh_CN" : language);
        params.put("queryParsing", queryParsing == null ? Boolean.TRUE : queryParsing);

        if (originalQuery != null && !originalQuery.isBlank()) {
            params.put("originalQuery", originalQuery);
        }
        if (checkIn != null && !checkIn.isBlank()) {
            params.put("checkIn", checkIn);
        }
        if (starRatings != null && !starRatings.isEmpty()) {
            params.put("starRatings", starRatings);
        }

        try {
            String body = objectMapper.writeValueAsString(params);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(API_BASE_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");
            addAuthHeaders(requestBuilder);
            HttpRequest httpRequest = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return structuredOnly(errorPayload("查询酒店失败", response.statusCode(), response.body()));
            }

            return structuredOnly(parseJsonPayload(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return structuredOnly(errorPayload("查询酒店失败", 500, e.getMessage()));
        } catch (IOException e) {
            return structuredOnly(errorPayload("查询酒店失败", 500, e.getMessage()));
        }
    }

    private void addAuthHeaders(HttpRequest.Builder requestBuilder) {
        String apiKey = properties.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey.trim());
        }
        String secretKey = properties.getSecretKey();
        if (secretKey != null && !secretKey.isBlank()) {
            requestBuilder.header("X-Secret-Key", secretKey.trim());
        }
    }

    private static Map<String, Object> parseJsonPayload(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", raw);
            return fallback;
        }
    }

    private static Map<String, Object> errorPayload(String message, int status, String body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", message);
        payload.put("status", status);
        payload.put("body", body);
        return payload;
    }

    private static McpSchema.CallToolResult structuredOnly(Map<String, Object> payload) {
        return McpSchema.CallToolResult.builder()
                .structuredContent(payload)
                .build();
    }
}
