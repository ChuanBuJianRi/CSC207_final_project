package use_case.todo.add_task;

public final class AddTaskInputData {
    private final String title;

    public AddTaskInputData(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
