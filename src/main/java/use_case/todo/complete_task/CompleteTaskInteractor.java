package use_case.todo.complete_task;

import entity.Task;
import use_case.todo.TaskDataAccessInterface;

import java.util.Optional;

public final class CompleteTaskInteractor implements CompleteTaskInputBoundary {
    private final TaskDataAccessInterface taskDataAccess;
    private final CompleteTaskOutputBoundary presenter;

    public CompleteTaskInteractor(TaskDataAccessInterface taskDataAccess, CompleteTaskOutputBoundary presenter) {
        this.taskDataAccess = taskDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(CompleteTaskInputData input) {
        String taskId = input.getTaskId();
        if (taskId == null || taskId.isBlank()) {
            presenter.prepareFailView("Select a task to complete.");
            return;
        }

        Optional<Task> task;
        try {
            task = taskDataAccess.markCompleted(taskId);
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not complete the task. Try again.");
            return;
        }

        if (task.isPresent()) {
            presenter.prepareSuccessView(new CompleteTaskOutputData(task.get()));
        } else {
            presenter.prepareFailView("Task not found.");
        }
    }
}
