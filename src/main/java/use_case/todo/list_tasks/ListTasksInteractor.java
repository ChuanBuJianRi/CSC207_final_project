package use_case.todo.list_tasks;

import entity.Task;
import use_case.todo.TaskDataAccessInterface;

import java.util.List;

public final class ListTasksInteractor implements ListTasksInputBoundary {
    private final TaskDataAccessInterface taskDataAccess;
    private final ListTasksOutputBoundary presenter;

    public ListTasksInteractor(TaskDataAccessInterface taskDataAccess, ListTasksOutputBoundary presenter) {
        this.taskDataAccess = taskDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ListTasksInputData input) {
        List<Task> tasks;
        try {
            tasks = taskDataAccess.findAll();
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not load tasks. Try again.");
            return;
        }

        presenter.prepareSuccessView(new ListTasksOutputData(tasks));
    }
}
