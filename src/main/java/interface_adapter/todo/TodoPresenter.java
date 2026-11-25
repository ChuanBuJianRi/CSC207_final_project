package interface_adapter.todo;

import use_case.todo.add_task.AddTaskOutputBoundary;
import use_case.todo.add_task.AddTaskOutputData;

public final class TodoPresenter implements AddTaskOutputBoundary {
    private final TodoViewModel viewModel;

    public TodoPresenter(TodoViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(AddTaskOutputData output) {
        viewModel.taskAdded(output.getTask());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
