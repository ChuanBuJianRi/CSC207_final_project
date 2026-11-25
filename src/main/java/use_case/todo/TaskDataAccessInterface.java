package use_case.todo;

import entity.Task;

import java.util.List;

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
     * Returns a snapshot of the stored tasks in creation order.
     */
    List<Task> findAll();
}
