package use_case.todo.complete_task;

import entity.Task;

public final class CompleteTaskOutputData {
    private final Task task;

    public CompleteTaskOutputData(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
