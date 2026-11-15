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

            client.complete(new OpenAiRequest("gpt-4.1", "hello"));
            assertTrue(body.get().contains("\"model\":\"gpt-4.1\""));
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
