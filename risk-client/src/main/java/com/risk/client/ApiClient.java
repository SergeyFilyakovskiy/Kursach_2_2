package com.risk.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApiClient
{

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String BASE = "http://localhost:8080/api";
    private static final ObjectMapper OM = new ObjectMapper();

    /** GET /api/ping */
    public static String ping() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/ping")).GET().build();
            return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception ex) {
            return "error: " + ex.getMessage();
        }
    }
    public static String rawGet(String path){
        try{
            return HTTP.send(req(path).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();
        }catch(Exception e){ return "error:"+e; }
    }

    /** POST /api/fs */
    public static String createFS(String period, String currency, String source) {
        String json = String.format(
                "{\"period\":\"%s\",\"currency\":\"%s\",\"source\":\"%s\"}",
                period, currency, source);

        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/fs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception ex) {
            return "error: " + ex.getMessage();
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record CalculationConfigDto(
            Long id,
            String method,
            Double confidenceLevel,
            Integer horizonDays
    ) {}
    public record FsDto(Long id, String period, String currency, String source) {}
    public record PositionDto(
            Long      id,
            String    symbol,
            int       quantity,
            BigDecimal lastPrice,
            String    valDate
    ) {}
    public record ValidationErrorDto(
            Long   id,
            String symbol,
            String date,    // или LocalDate, если будете регистрировать JavaTimeModule
            String field,
            String message
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HistoricalDataDto(
            Long   id,
            String symbol,
            String date,     // приходит как "yyyy-MM-dd"
            BigDecimal price,
            BigDecimal ret   // может быть null
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DatasetDto(
            Long   id,
            String name,
            String uploadedAt
    ) {}
    // 6) VaR по dataset (опционально)
    public static BigDecimal getVarByDataset(long datasetId) throws Exception {
        // HTTP GET http://…/api/data/{datasetId}/var
        HttpResponse<String> r = HTTP.send(
                HttpRequest.newBuilder(URI.create(BASE + "/data/" + datasetId + "/var"))
                        .header("Authorization", basicAuth())
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        System.out.println(">>> VAR response (status=" + r.statusCode() + "): " + r.body());
        if (r.statusCode() != 200) {
            throw new RuntimeException("Ошибка от сервера: " + r.body());
        }
        ObjectMapper om = new ObjectMapper();
        return om.readValue(r.body(), BigDecimal.class);
    }
    // 5) Валидация конкретного датасета
    public static List<ValidationErrorDto> validateData(long dsId)
            throws Exception {
        var r = HTTP.send(req("/data/" + dsId + "/validate").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .readValue(r.body(),
                        new TypeReference<List<ValidationErrorDto>>() {});
    }
    // 4) Список доходностей по датасету
    public static List<HistoricalDataDto> listData(long datasetId) throws Exception {
        HttpRequest request = req("/data/" + datasetId + "/history").GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка при listData(): "
                    + response.statusCode() + " / " + response.body());
        }
        return OM.readValue(
                response.body(),
                new TypeReference<List<HistoricalDataDto>>() {}
        );
    }

    // 3) Загрузка CSV → DatasetDto
    public static DatasetDto uploadDataset(File f) throws Exception {
        var req = buildMultipartRequest(f);
        var r   = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return new ObjectMapper().readValue(r.body(), DatasetDto.class);
    }
    private static HttpRequest buildMultipartRequest(File file) throws IOException {
        String boundary = "Boundary-" + UUID.randomUUID();
        String sep      = "\r\n";

        // Читаем байты файла
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        // Формируем части тела запроса
        var byteArrays = List.of(
                ("--" + boundary + sep +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" +
                        file.getName() + "\"" + sep +
                        "Content-Type: text/csv" + sep + sep
                ).getBytes(StandardCharsets.UTF_8),
                fileBytes,
                (sep + "--" + boundary + "--" + sep)
                        .getBytes(StandardCharsets.UTF_8)
        );

        return HttpRequest.newBuilder(URI.create(BASE + "/data/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", basicAuth())
                .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
                .build();
    }

    // 2) Список датасетов
    public static List<DatasetDto> listDatasets() throws Exception {
        HttpResponse<String> r = HTTP.send(
                req("/data/datasets").GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        System.out.println(">>> JSON from /data/datasets:\n" + r.body());

        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return om.readValue(
                r.body(),
                new TypeReference<List<DatasetDto>>() {}
        );
    }
    /** Сохранить конфигурацию */
    public static CalculationConfigDto updateConfig(CalculationConfigDto cfg) throws Exception {
        String json = OM.writeValueAsString(cfg);
        HttpRequest request = req("/data/config")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка при updateConfig(): "
                    + response.statusCode() + " / " + response.body());
        }
        return OM.readValue(response.body(), CalculationConfigDto.class);
    }

    public static String listFSJson() {
        try {
            HttpRequest r = req("/fs").GET().build();         // req() — builder с Basic-auth
            return HTTP.send(r, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            return "error: " + e;
        }
    }

    /** Получить текущую конфигурацию расчёта VaR */
    public static CalculationConfigDto getConfig() throws Exception {
        HttpRequest request = req("/data/config").GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка при getConfig(): "
                    + response.statusCode() + " / " + response.body());
        }
        return OM.readValue(response.body(), CalculationConfigDto.class);
    }

    public static String basicAuth() {
        if (Session.user == null) return "";
        String pair = Session.user + ":" + Session.pass;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    private static HttpRequest.Builder req(String path) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", basicAuth());
    }

    /** Проверяем учётку, возвращаем роль или null, если 401 */
    public static String login(String u, String p) {
        try {
            // собираем заголовок Basic Auth
            String hdr = "Basic " + Base64.getEncoder()
                    .encodeToString((u + ":" + p).getBytes(StandardCharsets.UTF_8));

            // делаем запрос на /api/role и получаем JSON {"role":"ROLE_ADMIN"}
            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder(URI.create(BASE + "/role"))
                            .header("Authorization", hdr)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            String roleJson = resp.body(); // e.g. {"role":"ROLE_ADMIN"}

            // парсим JSON, достаём значение поля "role"
            String role = new ObjectMapper()
                    .readTree(roleJson)
                    .get("role").asText();      // "ROLE_ADMIN"

            // убираем префикс "ROLE_"
            String trimmed = role.replace("ROLE_", ""); // "ADMIN"

            // сохраняем user/pass/role в сессии
            Session.user = u;
            Session.pass = p;
            Session.role = trimmed;

            return trimmed;
        } catch (Exception ignore) {
            return null;
        }
    }



    public static boolean register(String u,String p,String role){
        try{
            String body=OM.writeValueAsString(Map.of(
                    "username",u,"password",p,"role",role));
            int st=HTTP.send(HttpRequest.newBuilder(URI.create(BASE+"/register"))
                            .header("Content-Type","application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
            return st==200;
        }catch(Exception e){return false;}
    }

    public class Session
    {
        public static String user  = null;         // null → не вошёл
        public static String pass  = null;
        public static String role  = "GUEST";
    }

    public static PositionDto createPosition(String symbol, int qty,
                                             BigDecimal price, String date) {
        try {
            String json = String.format(
                    "{\"symbol\":\"%s\",\"quantity\":%d,\"lastPrice\":%s,\"valDate\":\"%s\"}",
                    symbol, qty, price.toPlainString(), date
            );
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/var/position"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", basicAuth())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            String body = HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
            // разбираем в PositionDto, а не FsDto
            return new ObjectMapper().readValue(body, PositionDto.class);
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Не удалось создать позицию: " + ex.getMessage(), ex);
        }
    }

    public static List<PositionDto> listPositions() throws IOException, InterruptedException {
        String body = HTTP.send(
                req("/var").GET().build(),
                HttpResponse.BodyHandlers.ofString()
        ).body();
        return new ObjectMapper().readValue(
                body,
                new TypeReference<List<PositionDto>>() {}
        );
    }

}
