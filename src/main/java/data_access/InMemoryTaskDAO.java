package data_access;

import entity.Task;
import use_case.todo.TaskDataAccessInterface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores tasks for the lifetime of this DAO instance.
 */
public final class InMemoryTaskDAO implements TaskDataAccessInterface {
    private final Map<String, Task> tasks = new LinkedHashMap<>();

    @Override
    public synchronized void save(Task task) {
        if (tasks.putIfAbsent(task.getId(), task) != null) {
            throw new IllegalStateException("A task with this ID already exists.");
        }
    }

    @Override
    public synchronized List<Task> findAll() {
        return List.copyOf(tasks.values());
    }

    @Override
    public synchronized Optional<Task> markCompleted(String taskId) {
        Task task = tasks.computeIfPresent(taskId, (id, current) -> {
            if (current.isCompleted()) {
                return current;
            }
            return new Task(id, current.getTitle(), true);
        });
        return Optional.ofNullable(task);
    }
}
