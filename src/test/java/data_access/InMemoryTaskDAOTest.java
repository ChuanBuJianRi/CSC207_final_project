package data_access;

import entity.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTaskDAOTest {
    @Test
    void returnsAnEmptyListBeforeTasksAreAdded() {
        assertTrue(new InMemoryTaskDAO().findAll().isEmpty());
    }

    @Test
    void returnsIndependentSnapshotsInCreationOrder() {
        InMemoryTaskDAO dao = new InMemoryTaskDAO();
        Task first = new Task("1", "Review CSC207", false);
        Task second = new Task("2", "Review CSC207", false);

        dao.save(first);
        List<Task> snapshot = dao.findAll();
        dao.save(second);

        assertEquals(List.of(first), snapshot);
        assertEquals(List.of(first, second), dao.findAll());
    }

    @Test
    void rejectsDuplicateIdsWithoutReplacingTheOriginalTask() {
        InMemoryTaskDAO dao = new InMemoryTaskDAO();
        Task first = new Task("1", "Review CSC207", false);
        dao.save(first);

        assertThrows(IllegalStateException.class,
                () -> dao.save(new Task("1", "Different title", true)));

        assertEquals(List.of(first), dao.findAll());
    }
}
