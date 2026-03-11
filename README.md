# AigoHotel MCP Server (Java)

AIGOHOTEL 酒店 MCP Server（Spring AI / Java 版本）。

当前版本与 `aigohotel-mcp` 主仓对齐，提供以下 3 个工具：

- `searchHotels`：按地点 + 结构化条件搜索酒店候选
- `getHotelDetail`：查询单个酒店实时房型与价格明细
- `getHotelSearchTags`：获取酒店搜索标签元数据（AI Cache）

## 工具参数概览

### `searchHotels`

必填参数：

- `originQuery` (string)
- `place` (string)
- `placeType` (string)

可选参数：

- `checkInParam` (object)
- `countryCode` (string)
- `filterOptions` (object)
- `hotelTags` (object)
- `size` (number, 默认 5)

### `getHotelDetail`

可选参数（`hotelId` 和 `name` 至少提供一个）：

- `hotelId` (number)
- `name` (string)
- `dateParam` (object)
- `occupancyParam` (object)
- `localeParam` (object)

### `getHotelSearchTags`

- 无参数

## 启动

```bash
mvn spring-boot:run
```

默认监听：`http://localhost:8000/mcp`

## 鉴权

服务端从 MCP HTTP 请求头读取 API Key，支持：

- `Authorization: Bearer <your_key>`
- `X-Secret-Key: <your_key>`

## MCP 客户端配置示例

```json
{
  "mcpServers": {
    "aigohotel": {
      "url": "http://localhost:8000/mcp",
      "headers": {
        "Authorization": "Bearer mcp_your_key"
      }
    }
  }
}
```

## 相关链接

- API Key 申请：https://mcp.agentichotel.cn/apply
- Spring AI MCP：https://docs.spring.io/spring-ai/reference/
