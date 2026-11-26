package use_case.todo.complete_task;

public final class CompleteTaskInputData {
    private final String taskId;

    public CompleteTaskInputData(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
