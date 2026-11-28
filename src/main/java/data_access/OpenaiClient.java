package data_access;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.agent.AgentMessage;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolResult;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenaiClient implements OpenAiGateway {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public OpenaiClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20)).build());
    }

    public OpenaiClient(String baseUrl, String apiKey, HttpClient httpClient) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = httpClient;
    }

    public static OpenaiClient fromEnvironment() {
        Map<String, String> file = readDotEnv(Path.of(".env"));
        String apiKey = setting(file, "OPENAI_ADAPTER_KEY", "OPENAI_API_KEY");
        String baseUrl = setting(file, "OPENAI_BASE_URL", "SOL_MODEL_URL",
                "OPUS_MODEL_URL", "GEMINI_MODEL_URL");
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
        String body = requestBody(request, model).toString();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl(model)))
                .timeout(Duration.ofSeconds(300))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return OpenAiResponse.failure(errorMessage(response.body(), response.statusCode()));
            }
            return parseResponse(response.body(), model);
        } catch (IOException exception) {
            return OpenAiResponse.failure("Could not reach the model service.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return OpenAiResponse.failure("The model request was interrupted.");
        }
    }

    private JSONObject requestBody(OpenAiRequest request, String model) {
        if (model.startsWith("gemini-")) {
            return geminiBody(request, model);
        }
        if (model.startsWith("claude-")) {
            return claudeBody(request, model);
        }
        return openAiBody(request, model);
    }

    private JSONObject openAiBody(OpenAiRequest request, String model) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        String effort = normalizedEffort(request.getEffort());
        if (!effort.isEmpty() && !model.startsWith("deepseek-")) {
            body.put("reasoning_effort", effort);
        }
        body.put("messages", openAiMessages(request.getMessages()));
        if (request.hasTools()) {
            body.put("tools", openAiTools(request.getTools()));
        }
        return body;
    }

    private JSONObject geminiBody(OpenAiRequest request, String model) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        String system = systemText(request.getMessages());
        if (!system.isEmpty()) {
            body.put("systemInstruction", new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject().put("text", system))));
        }
        JSONArray contents = new JSONArray();
        for (AgentMessage message : request.getMessages()) {
            if (message.getRole() == AgentMessage.Role.SYSTEM) {
                continue;
            }
            contents.put(geminiContent(message));
        }
        body.put("contents", contents);
        String effort = normalizedEffort(request.getEffort());
        String level = effort.isEmpty() ? "MEDIUM" : effort.toUpperCase(Locale.ROOT);
        body.put("generationConfig", new JSONObject()
                .put("thinkingConfig", new JSONObject().put("thinkingLevel", level)));
        if (request.hasTools()) {
            body.put("tools", geminiTools(request.getTools()));
        }
        return body;
    }

    private JSONObject claudeBody(OpenAiRequest request, String model) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 16000);
        body.put("thinking", new JSONObject().put("type", "adaptive"));
        String effort = normalizedEffort(request.getEffort());
        body.put("output_config", new JSONObject()
                .put("effort", effort.isEmpty() ? "medium" : effort));
        String system = systemText(request.getMessages());
        if (!system.isEmpty()) {
            body.put("system", system);
        }
        JSONArray messages = new JSONArray();
        for (AgentMessage message : request.getMessages()) {
            if (message.getRole() == AgentMessage.Role.SYSTEM) {
                continue;
            }
            messages.put(claudeMessage(message));
        }
        body.put("messages", messages);
        if (request.hasTools()) {
            body.put("tools", claudeTools(request.getTools()));
        }
        return body;
    }

    private JSONArray openAiMessages(List<AgentMessage> messages) {
        JSONArray array = new JSONArray();
        for (AgentMessage message : messages) {
            switch (message.getRole()) {
                case SYSTEM:
                    array.put(new JSONObject()
                            .put("role", "system").put("content", message.getContent()));
                    break;
                case USER:
                    array.put(new JSONObject()
                            .put("role", "user").put("content", message.getContent()));
                    break;
                case ASSISTANT:
                    JSONObject assistant = new JSONObject().put("role", "assistant");
                    assistant.put("content", message.getContent().isEmpty()
                            ? JSONObject.NULL : message.getContent());
                    if (!message.getToolCalls().isEmpty()) {
                        JSONArray calls = new JSONArray();
                        for (AgentToolCall call : message.getToolCalls()) {
                            calls.put(new JSONObject()
                                    .put("id", callId(call, "call"))
                                    .put("type", "function")
                                    .put("function", new JSONObject()
                                            .put("name", call.getName())
                                            .put("arguments", arguments(call))));
                        }
                        assistant.put("tool_calls", calls);
                    }
                    array.put(assistant);
                    break;
                case TOOL:
                    for (AgentToolResult result : message.getToolResults()) {
                        array.put(new JSONObject()
                                .put("role", "tool")
                                .put("tool_call_id", result.getCallId())
                                .put("content", result.getContent()));
                    }
                    break;
                default:
                    break;
            }
        }
        return array;
    }

    private JSONObject geminiContent(AgentMessage message) {
        JSONArray parts = new JSONArray();
        String role;
        switch (message.getRole()) {
            case USER:
                role = "user";
                parts.put(new JSONObject().put("text", message.getContent()));
                break;
            case ASSISTANT:
                role = "model";
                if (!message.getContent().isEmpty()) {
                    parts.put(new JSONObject().put("text", message.getContent()));
                }
                for (AgentToolCall call : message.getToolCalls()) {
                    parts.put(new JSONObject().put("functionCall", new JSONObject()
                            .put("name", call.getName())
                            .put("args", argumentsObject(call))));
                }
                break;
            case TOOL:
                role = "user";
                for (AgentToolResult result : message.getToolResults()) {
                    parts.put(new JSONObject().put("functionResponse", new JSONObject()
                            .put("name", result.getName())
                            .put("response", new JSONObject()
                                    .put("success", result.isSuccess())
                                    .put("content", result.getContent()))));
                }
                break;
            default:
                role = "user";
                break;
        }
        return new JSONObject().put("role", role).put("parts", parts);
    }

    private JSONObject claudeMessage(AgentMessage message) {
        switch (message.getRole()) {
            case USER:
                return new JSONObject().put("role", "user").put("content", message.getContent());
            case ASSISTANT:
                JSONObject assistant = new JSONObject().put("role", "assistant");
                if (!message.getProviderContentJson().isEmpty()) {
                    try {
                        assistant.put("content", new JSONArray(message.getProviderContentJson()));
                    } catch (JSONException exception) {
                        assistant.put("content", claudeAssistantBlocks(message));
                    }
                } else {
                    assistant.put("content", claudeAssistantBlocks(message));
                }
                return assistant;
            case TOOL:
                JSONArray results = new JSONArray();
                for (AgentToolResult result : message.getToolResults()) {
                    results.put(new JSONObject()
                            .put("type", "tool_result")
                            .put("tool_use_id", result.getCallId())
                            .put("content", result.getContent()));
                }
                return new JSONObject().put("role", "user").put("content", results);
            default:
                return new JSONObject().put("role", "user").put("content", message.getContent());
        }
    }

    private JSONArray claudeAssistantBlocks(AgentMessage message) {
        JSONArray blocks = new JSONArray();
        if (!message.getContent().isEmpty()) {
            blocks.put(new JSONObject().put("type", "text").put("text", message.getContent()));
        }
        for (AgentToolCall call : message.getToolCalls()) {
            blocks.put(new JSONObject()
                    .put("type", "tool_use")
                    .put("id", callId(call, "toolu"))
                    .put("name", call.getName())
                    .put("input", argumentsObject(call)));
        }
        if (blocks.isEmpty()) {
            blocks.put(new JSONObject().put("type", "text").put("text", ""));
        }
        return blocks;
    }

    private JSONArray openAiTools(List<AgentToolDefinition> tools) {
        JSONArray array = new JSONArray();
        for (AgentToolDefinition tool : tools) {
            array.put(new JSONObject()
                    .put("type", "function")
                    .put("function", new JSONObject()
                            .put("name", tool.getName())
                            .put("description", tool.getDescription())
                            .put("parameters", schema(tool))));
        }
        return array;
    }

    private JSONArray geminiTools(List<AgentToolDefinition> tools) {
        JSONArray declarations = new JSONArray();
        for (AgentToolDefinition tool : tools) {
            declarations.put(new JSONObject()
                    .put("name", tool.getName())
                    .put("description", tool.getDescription())
                    .put("parameters", geminiSchema(schema(tool))));
        }
        return new JSONArray().put(new JSONObject().put("functionDeclarations", declarations));
    }

    private JSONArray claudeTools(List<AgentToolDefinition> tools) {
        JSONArray array = new JSONArray();
        for (AgentToolDefinition tool : tools) {
            array.put(new JSONObject()
                    .put("name", tool.getName())
                    .put("description", tool.getDescription())
                    .put("input_schema", schema(tool)));
        }
        return array;
    }

    private static JSONObject schema(AgentToolDefinition tool) {
        try {
            return new JSONObject(tool.getParametersJson());
        } catch (JSONException exception) {
            return new JSONObject().put("type", "object")
                    .put("properties", new JSONObject());
        }
    }

    private static Object geminiSchema(Object value) {
        if (value instanceof JSONObject) {
            JSONObject source = (JSONObject) value;
            JSONObject result = new JSONObject();
            for (String key : source.keySet()) {
                switch (key) {
                    case "type":
                        result.put("type", source.optString("type", "")
                                .toUpperCase(Locale.ROOT));
                        break;
                    case "description":
                    case "required":
                    case "enum":
                        result.put(key, source.get(key));
                        break;
                    case "properties":
                        JSONObject properties = source.optJSONObject("properties");
                        JSONObject converted = new JSONObject();
                        if (properties != null) {
                            for (String property : properties.keySet()) {
                                converted.put(property, geminiSchema(properties.get(property)));
                            }
                        }
                        result.put("properties", converted);
                        break;
                    case "items":
                        result.put("items", geminiSchema(source.get("items")));
                        break;
                    default:
                        break;
                }
            }
            return result;
        }
        if (value instanceof JSONArray) {
            JSONArray source = (JSONArray) value;
            JSONArray result = new JSONArray();
            for (int index = 0; index < source.length(); index++) {
                result.put(geminiSchema(source.get(index)));
            }
            return result;
        }
        return value;
    }

    private OpenAiResponse parseResponse(String body, String model) {
        try {
            JSONObject root = new JSONObject(body);
            if (model.startsWith("gemini-")) {
                return parseGemini(root);
            }
            if (model.startsWith("claude-")) {
                return parseClaude(root);
            }
            return parseOpenAi(root);
        } catch (JSONException exception) {
            return OpenAiResponse.failure("The model returned an invalid response.");
        }
    }

    private OpenAiResponse parseOpenAi(JSONObject root) {
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        JSONObject message = choices.getJSONObject(0).optJSONObject("message");
        if (message == null) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        String content = message.isNull("content") ? "" : message.optString("content", "");
        List<AgentToolCall> calls = new ArrayList<>();
        JSONArray toolCalls = message.optJSONArray("tool_calls");
        if (toolCalls != null) {
            for (int index = 0; index < toolCalls.length(); index++) {
                JSONObject call = toolCalls.getJSONObject(index);
                JSONObject function = call.optJSONObject("function");
                if (function == null) {
                    continue;
                }
                Object arguments = function.opt("arguments");
                calls.add(new AgentToolCall(
                        call.optString("id", "call_" + index),
                        function.optString("name", ""),
                        arguments == null ? "{}" : arguments.toString()));
            }
        }
        if (content.isBlank() && calls.isEmpty()) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        return calls.isEmpty()
                ? OpenAiResponse.success(content)
                : OpenAiResponse.toolCalls(content, calls);
    }

    private OpenAiResponse parseGemini(JSONObject root) {
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        if (content == null) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        JSONArray parts = content.optJSONArray("parts");
        StringBuilder text = new StringBuilder();
        List<AgentToolCall> calls = new ArrayList<>();
        if (parts != null) {
            for (int index = 0; index < parts.length(); index++) {
                JSONObject part = parts.getJSONObject(index);
                String partText = part.optString("text", "");
                if (!partText.isEmpty()) {
                    text.append(partText);
                }
                JSONObject functionCall = part.optJSONObject("functionCall");
                if (functionCall != null) {
                    Object args = functionCall.opt("args");
                    calls.add(new AgentToolCall(
                            functionCall.optString("id", "gemini_" + index),
                            functionCall.optString("name", ""),
                            args == null ? "{}" : args.toString()));
                }
            }
        }
        String responseText = text.toString();
        if (responseText.isBlank() && calls.isEmpty()) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        return calls.isEmpty()
                ? OpenAiResponse.success(responseText)
                : OpenAiResponse.toolCalls(responseText, calls);
    }

    private OpenAiResponse parseClaude(JSONObject root) {
        JSONArray content = root.optJSONArray("content");
        if (content == null) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        StringBuilder text = new StringBuilder();
        List<AgentToolCall> calls = new ArrayList<>();
        for (int index = 0; index < content.length(); index++) {
            JSONObject block = content.getJSONObject(index);
            String type = block.optString("type", "");
            if ("text".equals(type)) {
                text.append(block.optString("text", ""));
            } else if ("tool_use".equals(type)) {
                Object input = block.opt("input");
                calls.add(new AgentToolCall(
                        block.optString("id", "toolu_" + index),
                        block.optString("name", ""),
                        input == null ? "{}" : input.toString()));
            }
        }
        String responseText = text.toString();
        if (responseText.isBlank() && calls.isEmpty()) {
            return OpenAiResponse.failure("The model returned no text.");
        }
        return calls.isEmpty()
                ? OpenAiResponse.success(responseText)
                : OpenAiResponse.toolCalls(responseText, calls, content.toString());
    }

    private static String systemText(List<AgentMessage> messages) {
        StringBuilder builder = new StringBuilder();
        for (AgentMessage message : messages) {
            if (message.getRole() == AgentMessage.Role.SYSTEM && !message.getContent().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(message.getContent());
            }
        }
        return builder.toString();
    }

    private static String normalizedEffort(String effort) {
        return effort == null ? "" : effort.trim().toLowerCase(Locale.ROOT);
    }

    private static String callId(AgentToolCall call, String prefix) {
        return call.getId() == null || call.getId().isBlank()
                ? prefix + "_" + Integer.toHexString(System.identityHashCode(call))
                : call.getId();
    }

    private static String arguments(AgentToolCall call) {
        return call.getArgumentsJson() == null || call.getArgumentsJson().isBlank()
                ? "{}" : call.getArgumentsJson();
    }

    private static JSONObject argumentsObject(AgentToolCall call) {
        try {
            return new JSONObject(arguments(call));
        } catch (JSONException exception) {
            return new JSONObject();
        }
    }

    private String errorMessage(String body, int status) {
        try {
            JSONObject root = new JSONObject(body);
            Object error = root.opt("error");
            if (error instanceof JSONObject) {
                String message = ((JSONObject) error).optString("message", "");
                if (!message.isBlank()) {
                    return message;
                }
            } else if (error instanceof String && !((String) error).isBlank()) {
                return (String) error;
            }
            String message = root.optString("message", "");
            if (!message.isBlank()) {
                return message;
            }
        } catch (JSONException exception) {
            // Fall through to the status message.
        }
        return "The model request failed with status " + status + ".";
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
}
