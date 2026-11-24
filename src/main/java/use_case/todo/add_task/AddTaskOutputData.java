package use_case.todo.add_task;

import entity.Task;

public final class AddTaskOutputData {
    private final Task task;

    public AddTaskOutputData(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
