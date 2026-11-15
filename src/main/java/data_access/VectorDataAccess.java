package data_access;

import entity.Memory;
import use_case.agent.AgentDataAccessInterface;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VectorDataAccess implements AgentDataAccessInterface {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public VectorDataAccess(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    }

    public VectorDataAccess(String baseUrl, String apiKey, HttpClient httpClient) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = httpClient;
    }

    public static VectorDataAccess fromEnvironment() {
        Map<String, String> file = readDotEnv(Path.of(".env"));
        return new VectorDataAccess(setting(file, "VECTOR_DB_URL"), setting(file, "VECTOR_DB_API_KEY"));
    }

    @Override
    public void save(Memory memory) {
        String body = "{"
                + "\"id\":" + jsonString(memory.getId())
                + ",\"content\":" + jsonString(memory.getContent())
                + ",\"source\":" + jsonString(memory.getSource())
                + "}";
        send("/memories", body);
    }

    @Override
    public List<Memory> findRelevant(String query) {
        String response = send("/memories/search", "{\"query\":" + jsonString(query) + "}");
        return parseMemories(response);
    }

    private String send(String path, String body) {
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("VECTOR_DB_URL is not set.");
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (!apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Vector database request failed with status " + response.statusCode() + ".");
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reach the vector database.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The vector database request was interrupted.");
        }
    }

    private static List<Memory> parseMemories(String json) {
        List<Memory> memories = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return memories;
        }
        int from = 0;
        while (from < json.length()) {
            int idKey = json.indexOf("\"id\"", from);
            if (idKey < 0) {
                break;
            }
            String id = readField(json, idKey);
            int contentKey = json.indexOf("\"content\"", idKey);
            int sourceKey = json.indexOf("\"source\"", idKey);
            if (contentKey < 0 || sourceKey < 0) {
                break;
            }
            memories.add(new Memory(id, readField(json, contentKey), readField(json, sourceKey)));
            from = sourceKey + 1;
        }
        return memories;
    }

    private static String readField(String json, int keyIndex) {
        int colon = json.indexOf(':', keyIndex);
        int quote = json.indexOf('"', colon + 1);
        StringBuilder builder = new StringBuilder();
        for (int i = quote + 1; i < json.length(); i++) {
            char character = json.charAt(i);
            if (character == '"') {
                return builder.toString();
            }
            if (character == '\\' && i + 1 < json.length()) {
                builder.append(json.charAt(++i));
                continue;
            }
            builder.append(character);
        }
        return builder.toString();
    }

    private static String jsonString(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '"' || character == '\\') {
                builder.append('\\').append(character);
            } else if (character == '\n') {
                builder.append("\\n");
            } else if (character == '\r') {
                builder.append("\\r");
            } else if (character == '\t') {
                builder.append("\\t");
            } else if (character < 0x20) {
                builder.append(String.format("\\u%04x", (int) character));
            } else {
                builder.append(character);
            }
        }
        return builder.append('"').toString();
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String setting(Map<String, String> file, String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = file.get(key);
        return fromFile == null ? "" : fromFile.trim();
    }

    private static Map<String, String> readDotEnv(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
        } catch (IOException exception) {
            return values;
        }
        return values;
    }
}
