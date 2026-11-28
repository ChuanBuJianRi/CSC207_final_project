package data_access;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import use_case.agent.AgentMessage;
import use_case.agent.AgentToolCall;
import use_case.agent.AgentToolDefinition;
import use_case.agent.AgentToolResult;
import use_case.agent.OpenAiRequest;
import use_case.agent.OpenAiResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenaiClientTest {
    @Test
    void completeUsesTheModelSelectedForThatRequest() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            OpenaiClient client = new OpenaiClient(baseUrl, "test-key");

            OpenAiResponse first = client.complete(new OpenAiRequest("gpt-4o-mini", "hello"));
            assertTrue(first.isSuccess());
            assertEquals("ok", first.getContent());
            assertTrue(body.get().contains("\"model\":\"gpt-4o-mini\""));

            client.complete(new OpenAiRequest("gpt-6.1-sol", "hello", "high"));
            assertTrue(body.get().contains("\"model\":\"gpt-6.1-sol\""));
            assertTrue(body.get().contains("\"reasoning_effort\":\"high\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void geminiUsesGenerateContentWithThinkingLevel() throws IOException {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"hi there\"}]}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            OpenaiClient client = new OpenaiClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
            OpenAiResponse response = client.complete(new OpenAiRequest("gemini-3.8-flash", "hello", "low"));
            assertTrue(response.isSuccess());
            assertEquals("hi there", response.getContent());
            assertEquals("/v1beta/models/gemini-3.8-flash:generateContent", path.get());
            assertTrue(body.get().contains("\"thinkingLevel\":\"LOW\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void claudeUsesMessagesWithEffort() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"content\":[{\"type\":\"text\",\"text\":\"answer\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            OpenaiClient client = new OpenaiClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
            OpenAiResponse response = client.complete(new OpenAiRequest("claude-opus-5-5", "hello", "high"));
            assertTrue(response.isSuccess());
            assertEquals("answer", response.getContent());
            assertTrue(body.get().contains("\"effort\":\"high\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void completeRejectsABlankModel() {
        OpenaiClient client = new OpenaiClient("http://127.0.0.1/v1", "test-key");
        OpenAiResponse response = client.complete(new OpenAiRequest("  ", "hello"));
        assertFalse(response.isSuccess());
        assertEquals("Choose a model.", response.getErrorMessage());
    }

    @Test
    void openAiSendsToolsAndParsesToolCalls() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        String responseJson = "{\"choices\":[{\"message\":{\"content\":null,\"tool_calls\":"
                + "[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"read_note\","
                + "\"arguments\":\"{\\\"id\\\":\\\"1\\\"}\"}}]}}]}";
        HttpServer server = startServer("/v1/chat/completions", responseJson, body, path);
        try {
            OpenaiClient client = new OpenaiClient(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key");
            AgentToolCall call = new AgentToolCall("call_1", "read_note", "{\"id\":\"1\"}");
            List<AgentMessage> messages = List.of(
                    AgentMessage.user("read note 1"),
                    AgentMessage.assistant("", List.of(call), ""),
                    AgentMessage.toolResults(List.of(
                            AgentToolResult.success("call_1", "read_note", "{\"id\":\"1\"}"))));

            OpenAiResponse response = client.complete(new OpenAiRequest(
                    "gpt-6.1-sol", messages, "high", List.of(toolDefinition())));

            assertTrue(response.isSuccess());
            assertTrue(response.hasToolCalls());
            assertEquals("read_note", response.getToolCalls().get(0).getName());
            assertEquals("{\"id\":\"1\"}", response.getToolCalls().get(0).getArgumentsJson());
            assertEquals("/v1/chat/completions", path.get());
            assertTrue(body.get().contains("\"tools\":["));
            assertTrue(body.get().contains("\"reasoning_effort\":\"high\""));
            assertTrue(body.get().contains("\"role\":\"tool\""));
            assertTrue(body.get().contains("\"tool_call_id\":\"call_1\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void geminiSendsFunctionDeclarationsAndParsesFunctionCalls() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        String responseJson = "{\"candidates\":[{\"content\":{\"parts\":[{\"functionCall\":"
                + "{\"name\":\"read_note\",\"args\":{\"id\":\"1\"}}}]}}]}";
        HttpServer server = startServer("/v1beta/models", responseJson, body, path);
        try {
            OpenaiClient client = new OpenaiClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
            AgentToolCall call = new AgentToolCall("call_1", "read_note", "{\"id\":\"1\"}");
            List<AgentMessage> messages = List.of(
                    AgentMessage.user("read note 1"),
                    AgentMessage.assistant("", List.of(call), ""),
                    AgentMessage.toolResults(List.of(
                            AgentToolResult.success("call_1", "read_note", "{\"id\":\"1\"}"))));

            OpenAiResponse response = client.complete(new OpenAiRequest(
                    "gemini-3.8-flash", messages, "low", List.of(toolDefinition())));

            assertTrue(response.isSuccess());
            assertTrue(response.hasToolCalls());
            assertEquals("read_note", response.getToolCalls().get(0).getName());
            assertEquals("{\"id\":\"1\"}", response.getToolCalls().get(0).getArgumentsJson());
            assertEquals("/v1beta/models/gemini-3.8-flash:generateContent", path.get());
            assertTrue(body.get().contains("\"functionDeclarations\""));
            assertTrue(body.get().contains("\"type\":\"OBJECT\""));
            assertTrue(body.get().contains("\"type\":\"STRING\""));
            assertTrue(body.get().contains("\"functionResponse\""));
            assertTrue(body.get().contains("\"thinkingLevel\":\"LOW\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void claudeSendsToolsAndParsesToolUseBlocks() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        String responseJson = "{\"content\":[{\"type\":\"text\",\"text\":\"Let me read.\"},"
                + "{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"read_note\","
                + "\"input\":{\"id\":\"1\"}}],\"stop_reason\":\"tool_use\"}";
        HttpServer server = startServer("/v1/messages", responseJson, body, path);
        try {
            OpenaiClient client = new OpenaiClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
            AgentToolCall call = new AgentToolCall("call_1", "read_note", "{\"id\":\"1\"}");
            List<AgentMessage> messages = List.of(
                    AgentMessage.user("read note 1"),
                    AgentMessage.assistant("", List.of(call), ""),
                    AgentMessage.toolResults(List.of(
                            AgentToolResult.success("call_1", "read_note", "{\"id\":\"1\"}"))));

            OpenAiResponse response = client.complete(new OpenAiRequest(
                    "claude-opus-5-5", messages, "high", List.of(toolDefinition())));

            assertTrue(response.isSuccess());
            assertTrue(response.hasToolCalls());
            assertEquals("Let me read.", response.getContent());
            assertEquals("read_note", response.getToolCalls().get(0).getName());
            assertEquals("{\"id\":\"1\"}", response.getToolCalls().get(0).getArgumentsJson());
            assertFalse(response.getRawAssistantContentJson().isEmpty());
            assertEquals("/v1/messages", path.get());
            assertTrue(body.get().contains("\"input_schema\""));
            assertTrue(body.get().contains("\"tool_result\""));
            assertTrue(body.get().contains("\"tool_use_id\":\"call_1\""));
            assertTrue(body.get().contains("\"thinking\":{\"type\":\"adaptive\"}"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void deepSeekUsesOpenAiToolsWithoutReasoningEffort() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        HttpServer server = startServer("/v1/chat/completions",
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}", body, path);
        try {
            OpenaiClient client = new OpenaiClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "test-key");

            OpenAiResponse response = client.complete(new OpenAiRequest(
                    "deepseek-chat", List.of(AgentMessage.user("hi")), "high",
                    List.of(toolDefinition())));

            assertTrue(response.isSuccess());
            assertEquals("ok", response.getContent());
            assertEquals("/v1/chat/completions", path.get());
            assertTrue(body.get().contains("\"tools\":["));
            assertFalse(body.get().contains("\"reasoning_effort\""));
        } finally {
            server.stop(0);
        }
    }

    private static AgentToolDefinition toolDefinition() {
        return new AgentToolDefinition("read_note", "Read a note.",
                "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}},"
                        + "\"required\":[\"id\"]}");
    }

    private static HttpServer startServer(String context, String responseJson,
                                          AtomicReference<String> body,
                                          AtomicReference<String> path) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(context, exchange -> {
            if (path != null) {
                path.set(exchange.getRequestURI().getPath());
            }
            if (body != null) {
                body.set(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
            }
            byte[] response = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return server;
    }
}
