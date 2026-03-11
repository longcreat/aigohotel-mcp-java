package com.aigohotel.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HotelSearchTool {
    private static final String API_BASE_URL = "https://mcp.aigohotel.com/mcp";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @McpTool(
            name = "searchHotels",
            description = "该工具用于查询全球酒店。根据地点及结构化筛选条件（日期、入住晚数、人数、星级、距离、标签、品牌、预算）返回符合条件的酒店候选列表与最低价格，用于酒店初筛与比选。",
            generateOutputSchema = true
    )
    public McpSchema.CallToolResult searchHotels(
            @McpToolParam(description = "用户原始自然语言需求（原句），用于语义理解与召回排序。", required = true)
            String originQuery,
            @McpToolParam(description = "用于定位检索范围的地点名称。请传可被地理解析的单一地点文本。", required = true)
            String place,
            @McpToolParam(description = "地点类型。仅允许以下值之一：城市、机场、景点、火车站、地铁站、酒店、区/县、详细地址。必须与 place 语义一致。", required = true)
            String placeType,
            @McpToolParam(description = "入住参数对象。字段：adultCount、checkInDate、stayNights。", required = false)
            Map<String, Object> checkInParam,
            @McpToolParam(description = "国家二字码（ISO 3166-1 alpha-2，大写），如 CN、US、JP。", required = false)
            String countryCode,
            @McpToolParam(description = "基础筛选对象。字段：distanceInMeter、starRatings。", required = false)
            Map<String, Object> filterOptions,
            @McpToolParam(description = "标签筛选对象。字段：preferredTags、requiredTags、excludedTags、preferredBrands、maxPricePerNight、minRoomSize。", required = false)
            Map<String, Object> hotelTags,
            @McpToolParam(description = "返回酒店数量上限，默认5。", required = false)
            Integer size
    ) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("originQuery", originQuery);
        params.put("place", place);
        params.put("placeType", placeType);
        params.put("size", size == null ? 5 : size);

        if (checkInParam != null && !checkInParam.isEmpty()) {
            params.put("checkInParam", checkInParam);
        }
        if (countryCode != null && !countryCode.isBlank()) {
            params.put("countryCode", countryCode);
        }
        if (filterOptions != null && !filterOptions.isEmpty()) {
            params.put("filterOptions", filterOptions);
        }
        if (hotelTags != null && !hotelTags.isEmpty()) {
            params.put("hotelTags", hotelTags);
        }

        return callApi("POST", "/hotelsearch", params);
    }

    @McpTool(
            name = "getHotelDetail",
            description = "查询单个酒店实时房型与价格明细（房型、价税、可售状态、退改规则等）。用于用户已选定具体酒店后的二次查价。",
            generateOutputSchema = true
    )
    public McpSchema.CallToolResult getHotelDetail(
            @McpToolParam(description = "酒店唯一ID。与 name 二选一；若同时传入，优先使用 hotelId（从 searchHotels 工具获取）。", required = false)
            Integer hotelId,
            @McpToolParam(description = "酒店名称（模糊匹配）。仅在没有 hotelId 时使用。", required = false)
            String name,
            @McpToolParam(description = "入离店日期对象。字段：checkInDate、checkOutDate。", required = false)
            Map<String, Object> dateParam,
            @McpToolParam(description = "入住人数与房间数量对象。字段：adultCount、childCount、childAgeDetails、roomCount。", required = false)
            Map<String, Object> occupancyParam,
            @McpToolParam(description = "区域与币种对象。字段：countryCode、currency。", required = false)
            Map<String, Object> localeParam
    ) {
        if (hotelId == null && (name == null || name.isBlank())) {
            return structuredOnly(errorPayload("hotelId 和 name 需至少提供一个", 400, "Invalid Arguments"));
        }

        Map<String, Object> params = new LinkedHashMap<>();
        if (hotelId != null) {
            params.put("hotelId", hotelId);
        }
        if (name != null && !name.isBlank()) {
            params.put("name", name);
        }
        if (dateParam != null && !dateParam.isEmpty()) {
            params.put("dateParam", dateParam);
        }
        if (occupancyParam != null && !occupancyParam.isEmpty()) {
            params.put("occupancyParam", occupancyParam);
        }
        if (localeParam != null && !localeParam.isEmpty()) {
            params.put("localeParam", localeParam);
        }

        return callApi("POST", "/hoteldetail", params);
    }

    @McpTool(
            name = "getHotelSearchTags",
            description = "获取酒店搜索元数据（AI Cache），包含可用的标签列表（以当前启用数据为准）。",
            generateOutputSchema = true
    )
    public McpSchema.CallToolResult getHotelSearchTags() {
        return callApi("GET", "/hoteltags", null);
    }

    private McpSchema.CallToolResult callApi(String method, String endpoint, Map<String, Object> payload) {
        try {
            String apiKey = resolveApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return structuredOnly(errorPayload("未提供 API Key，请在请求 header 中添加 Authorization: Bearer <your_api_key> 或设置环境变量 AIGOHOTEL_API_KEY/AIGOHOTEL_SECRET_KEY", 401, "Missing API Key"));
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(API_BASE_URL + endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json");

            HttpRequest request;
            if ("GET".equalsIgnoreCase(method)) {
                request = requestBuilder.GET().build();
            } else {
                String body = OBJECT_MAPPER.writeValueAsString(payload == null ? Map.of() : payload);
                request = requestBuilder
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
            }

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return structuredOnly(errorPayload("调用上游接口失败", response.statusCode(), response.body()));
            }

            return structuredOnly(parseJsonPayload(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return structuredOnly(errorPayload("调用上游接口失败", 500, e.getMessage()));
        } catch (IOException e) {
            return structuredOnly(errorPayload("调用上游接口失败", 500, e.getMessage()));
        }
    }

    private String extractApiKeyFromRequest() {
        try {
            return Mono.deferContextual(ctx -> {
                if (ctx.hasKey("mcp.request.headers")) {
                    HttpHeaders headers = ctx.get("mcp.request.headers");
                    String authHeader = headers.getFirst("authorization");
                    if (authHeader == null) {
                        authHeader = headers.getFirst("Authorization");
                    }
                    if (authHeader == null) {
                        authHeader = headers.getFirst("x-secret-key");
                    }
                    if (authHeader == null) {
                        authHeader = headers.getFirst("X-Secret-Key");
                    }
                    if (authHeader != null && !authHeader.isBlank()) {
                        String key = authHeader.startsWith("Bearer ")
                                ? authHeader.substring(7).trim()
                                : authHeader.trim();
                        return Mono.just(key);
                    }
                }
                return Mono.empty();
            }).block();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveApiKey() {
        String fromRequest = extractApiKeyFromRequest();
        if (fromRequest != null && !fromRequest.isBlank()) {
            return fromRequest;
        }

        String fromApiKeyEnv = System.getenv("AIGOHOTEL_API_KEY");
        if (fromApiKeyEnv != null && !fromApiKeyEnv.isBlank()) {
            return fromApiKeyEnv.trim();
        }

        String fromSecretKeyEnv = System.getenv("AIGOHOTEL_SECRET_KEY");
        if (fromSecretKeyEnv != null && !fromSecretKeyEnv.isBlank()) {
            return fromSecretKeyEnv.trim();
        }

        return null;
    }

    private static Map<String, Object> parseJsonPayload(String raw) {
        try {
            return OBJECT_MAPPER.readValue(raw, new TypeReference<Map<String, Object>>() {});
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
