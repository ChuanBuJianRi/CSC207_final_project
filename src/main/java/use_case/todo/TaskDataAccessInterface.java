package use_case.todo;

import entity.Task;

import java.util.List;
import java.util.Optional;

/**
 * Storage operations shared by the todo use cases.
 */
public interface TaskDataAccessInterface {
    /**
     * Stores a new task. If storage fails or the ID already exists, throws
     * an IllegalStateException without replacing an existing task.
     */
    void save(Task task);

    /**
     * Returns a snapshot of the stored tasks in creation order. Throws an
     * IllegalStateException if the tasks cannot be read.
     */
    List<Task> findAll();

    /**
     * Marks an existing task complete and returns its current state.
     * Repeating the operation returns the already completed task.
     * Returns empty for an unknown ID. Throws IllegalStateException on storage failure.
     */
    Optional<Task> markCompleted(String taskId);
}
