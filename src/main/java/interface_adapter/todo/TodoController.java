package interface_adapter.todo;

import use_case.todo.add_task.AddTaskInputBoundary;
import use_case.todo.add_task.AddTaskInputData;

public final class TodoController {
    private final AddTaskInputBoundary addTask;

    public TodoController(AddTaskInputBoundary addTask) {
        this.addTask = addTask;
    }

    public void addTask(String title) {
        addTask.execute(new AddTaskInputData(title));
    }
}
