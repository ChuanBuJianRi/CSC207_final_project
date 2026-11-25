package use_case.todo.list_tasks;

import entity.Task;

import java.util.List;

public final class ListTasksOutputData {
    private final List<Task> tasks;

    public ListTasksOutputData(List<Task> tasks) {
        this.tasks = List.copyOf(tasks);
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
