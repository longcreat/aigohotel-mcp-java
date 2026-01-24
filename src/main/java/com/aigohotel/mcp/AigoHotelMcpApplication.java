package com.aigohotel.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import com.aigohotel.mcp.config.AigoHotelProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class AigoHotelMcpApplication {
    private static final Logger logger = LoggerFactory.getLogger(AigoHotelMcpApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AigoHotelMcpApplication.class, args);
    }

    @Bean
    ApplicationRunner logStartup(Environment environment, AigoHotelProperties properties) {
        return args -> logApiKeyStatus(environment, properties);
    }

    private static void logApiKeyStatus(Environment environment, AigoHotelProperties properties) {
        String apiKey = properties.getApiKey();
        String secretKey = properties.getSecretKey();
        String host = environment.getProperty("server.address", "0.0.0.0");
        String port = environment.getProperty("server.port", "8000");
        String endpoint = environment.getProperty(
                "spring.ai.mcp.server.streamable-http.mcp-endpoint",
                "/mcp"
        );

        logger.info("AigoHotel MCP Server 启动中...");
        logger.info("MCP 端点: http://{}:{}{} (默认)", host, port, endpoint);
        boolean hasApiKey = apiKey != null && !apiKey.isBlank();
        boolean hasSecretKey = secretKey != null && !secretKey.isBlank();
        if (!hasApiKey && !hasSecretKey) {
            logger.warn("警告: 未配置 AIGOHOTEL_API_KEY 或 AIGOHOTEL_SECRET_KEY");
            return;
        }
        if (hasSecretKey && !secretKey.startsWith("mcp_")) {
            logger.warn("警告: Secret Key 格式错误,应以 'mcp_' 开头");
        }
        if (hasApiKey) {
            logger.info("Bearer Token: 已配置");
        }
        if (hasSecretKey) {
            logger.info("X-Secret-Key: 已配置");
        }
    }
}
