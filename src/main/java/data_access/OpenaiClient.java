package data_access;

import use_case.agent.OpenAiGateway;
import use_case.agent.OpenAiRequest;
import use_case.agent.OpenAiResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenaiClient implements OpenAiGateway {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public OpenaiClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
    }

    public OpenaiClient(String baseUrl, String apiKey, HttpClient httpClient) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = httpClient;
    }

    public static OpenaiClient fromEnvironment() {
        Map<String, String> file = readDotEnv(Path.of(".env"));
        String apiKey = setting(file, "OPENAI_ADAPTER_KEY", "OPENAI_API_KEY");
        String baseUrl = setting(file, "OPENAI_BASE_URL", "SOL_MODEL_URL", "OPUS_MODEL_URL", "GEMINI_MODEL_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        return new OpenaiClient(baseUrl, apiKey);
    }

    @Override
    public OpenAiResponse complete(OpenAiRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            return OpenAiResponse.failure("Choose a model.");
        }
        if (apiKey.isBlank()) {
            return OpenAiResponse.failure("OPENAI_ADAPTER_KEY is not set.");
        }
        String model = request.getModel().trim();
        String body = requestBody(request, model);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl(model)))
                .timeout(Duration.ofSeconds(300))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = extractString(response.body(), "message");
                if (message == null || message.isBlank()) {
                    message = "OpenAI request failed with status " + response.statusCode() + ".";
                }
                return OpenAiResponse.failure(message);
            }
            String content = extractString(response.body(), "content");
            if (content == null) {
                content = extractString(response.body(), "text");
            }
            if (content == null) {
                return OpenAiResponse.failure("The model returned no text.");
            }
            return OpenAiResponse.success(content);
        } catch (IOException exception) {
            return OpenAiResponse.failure("Could not reach the model service.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return OpenAiResponse.failure("The model request was interrupted.");
        }
    }

    private String requestUrl(String model) {
        if (model.startsWith("gemini-")) {
            return serviceRoot() + "/v1beta/models/" + model + ":generateContent";
        }
        if (model.startsWith("claude-")) {
            return serviceRoot() + "/v1/messages";
        }
        return chatCompletionsUrl();
    }

    private String requestBody(OpenAiRequest request, String model) {
        String effort = request.getEffort() == null ? "" : request.getEffort().trim().toLowerCase();
        if (model.startsWith("gemini-")) {
            String level = effort.isEmpty() ? "MEDIUM" : effort.toUpperCase();
            return "{\"model\":" + jsonString(model)
                    + ",\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":" + jsonString(request.getUserMessage()) + "}]}],"
                    + "\"generationConfig\":{\"thinkingConfig\":{\"thinkingLevel\":" + jsonString(level) + "}}}";
        }
        if (model.startsWith("claude-")) {
            String selected = effort.isEmpty() ? "medium" : effort;
            return "{\"model\":" + jsonString(model)
                    + ",\"max_tokens\":16000,\"thinking\":{\"type\":\"adaptive\"}"
                    + ",\"output_config\":{\"effort\":" + jsonString(selected) + "}"
                    + ",\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(request.getUserMessage()) + "}]}";
        }
        String effortField = effort.isEmpty() ? "" : ",\"reasoning_effort\":" + jsonString(effort);
        return "{\"model\":" + jsonString(model)
                + effortField
                + ",\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(request.getUserMessage()) + "}]}";
    }

    private String serviceRoot() {
        String root = baseUrl;
        if (root.endsWith("/chat/completions")) {
            root = root.substring(0, root.length() - "/chat/completions".length());
        }
        if (root.endsWith("/v1")) {
            root = root.substring(0, root.length() - "/v1".length());
        }
        return root;
    }

    private String chatCompletionsUrl() {
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/chat/completions";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String setting(Map<String, String> file, String... keys) {
        for (String key : keys) {
            String fromEnv = System.getenv(key);
            if (fromEnv != null && !fromEnv.isBlank()) {
                return fromEnv.trim();
            }
            String fromFile = file.get(key);
            if (fromFile != null && !fromFile.isBlank()) {
                return fromFile.trim();
            }
        }
        return null;
    }

    private static Map<String, String> readDotEnv(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(path)) {
            return values;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException exception) {
            return values;
        }
        return values;
    }

    static String jsonString(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(text.length() + 2);
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
            }
        }
        builder.append('"');
        return builder.toString();
    }

    static String extractString(String json, String field) {
        if (json == null) {
            return null;
        }
        String needle = "\"" + field + "\"";
        int from = 0;
        while (from < json.length()) {
            int key = json.indexOf(needle, from);
            if (key < 0) {
                return null;
            }
            int colon = json.indexOf(':', key + needle.length());
            if (colon < 0) {
                return null;
            }
            int index = colon + 1;
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
            if (index < json.length() && json.startsWith("null", index)) {
                return null;
            }
            if (index < json.length() && json.charAt(index) == '"') {
                return readJsonString(json, index);
            }
            from = colon + 1;
        }
        return null;
    }

    private static String readJsonString(String json, int openingQuote) {
        StringBuilder builder = new StringBuilder();
        for (int i = openingQuote + 1; i < json.length(); i++) {
            char character = json.charAt(i);
            if (character == '"') {
                return builder.toString();
            }
            if (character != '\\' || i + 1 >= json.length()) {
                builder.append(character);
                continue;
            }
            char escaped = json.charAt(++i);
            switch (escaped) {
                case '"':
                case '\\':
                case '/':
                    builder.append(escaped);
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'u':
                    if (i + 4 >= json.length()) {
                        return builder.toString();
                    }
                    String hex = json.substring(i + 1, i + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                    break;
                default:
                    builder.append(escaped);
            }
        }
        return builder.toString();
    }
}
