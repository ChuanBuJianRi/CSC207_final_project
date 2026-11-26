package use_case.todo.complete_task;

public interface CompleteTaskOutputBoundary {
    void prepareSuccessView(CompleteTaskOutputData output);

    void prepareFailView(String errorMessage);
}
