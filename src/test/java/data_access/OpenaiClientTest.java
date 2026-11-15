package data_access;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import use_case.agent.OpenAiRequest;
import use_case.agent.OpenAiResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
}
