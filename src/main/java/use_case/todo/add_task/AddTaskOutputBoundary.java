package use_case.todo.add_task;

public interface AddTaskOutputBoundary {
    void prepareSuccessView(AddTaskOutputData output);

    void prepareFailView(String errorMessage);
}
