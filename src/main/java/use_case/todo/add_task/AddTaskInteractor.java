package use_case.todo.add_task;

import entity.Task;
import use_case.todo.TaskDataAccessInterface;

import java.util.UUID;

public final class AddTaskInteractor implements AddTaskInputBoundary {
    private final TaskDataAccessInterface taskDataAccess;
    private final AddTaskOutputBoundary presenter;

    public AddTaskInteractor(TaskDataAccessInterface taskDataAccess, AddTaskOutputBoundary presenter) {
        this.taskDataAccess = taskDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(AddTaskInputData input) {
        String title = input.getTitle();
        if (title == null || title.isBlank()) {
            presenter.prepareFailView("Enter a task title.");
            return;
        }

        Task task = new Task(UUID.randomUUID().toString(), title.strip(), false);
        try {
            taskDataAccess.save(task);
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not save the task. Try again.");
            return;
        }
        presenter.prepareSuccessView(new AddTaskOutputData(task));
    }
}
