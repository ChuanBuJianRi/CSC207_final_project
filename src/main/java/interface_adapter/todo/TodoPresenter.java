package interface_adapter.todo;

import use_case.todo.add_task.AddTaskOutputBoundary;
import use_case.todo.add_task.AddTaskOutputData;
import use_case.todo.complete_task.CompleteTaskOutputBoundary;
import use_case.todo.complete_task.CompleteTaskOutputData;
import use_case.todo.list_tasks.ListTasksOutputBoundary;
import use_case.todo.list_tasks.ListTasksOutputData;

public final class TodoPresenter implements AddTaskOutputBoundary, ListTasksOutputBoundary,
        CompleteTaskOutputBoundary {
    private final TodoViewModel viewModel;

    public TodoPresenter(TodoViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(AddTaskOutputData output) {
        viewModel.taskAdded(output.getTask());
    }

    @Override
    public void prepareSuccessView(ListTasksOutputData output) {
        viewModel.replaceTasks(output.getTasks());
    }

    @Override
    public void prepareSuccessView(CompleteTaskOutputData output) {
        viewModel.taskCompleted(output.getTask());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
