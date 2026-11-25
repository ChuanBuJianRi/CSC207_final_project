package use_case.todo.list_tasks;

public interface ListTasksOutputBoundary {
    void prepareSuccessView(ListTasksOutputData output);

    void prepareFailView(String errorMessage);
}
