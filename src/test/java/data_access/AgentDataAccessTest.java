package data_access;

import com.sun.net.httpserver.HttpServer;
import entity.Memory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentDataAccessTest {
    @Test
    void saveSendsTheMemoryToTheVectorDatabase() throws IOException {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/memories", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            VectorDataAccess dataAccess = new VectorDataAccess("http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
            dataAccess.save(new Memory("1", "CSC207 exam", "note"));
            assertEquals("/memories", path.get());
            assertTrue(body.get().contains("\"content\":\"CSC207 exam\""));
            assertTrue(body.get().contains("\"source\":\"note\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void findRelevantReturnsMemoriesFromTheVectorDatabase() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/memories/search", exchange -> {
            byte[] response = "[{\"id\":\"1\",\"content\":\"exam at 9\",\"source\":\"event\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            VectorDataAccess dataAccess = new VectorDataAccess("http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
            List<Memory> memories = dataAccess.findRelevant("when is the exam");
            assertEquals(1, memories.size());
            assertEquals("exam at 9", memories.get(0).getContent());
            assertEquals("event", memories.get(0).getSource());
        } finally {
            server.stop(0);
        }
    }
}
