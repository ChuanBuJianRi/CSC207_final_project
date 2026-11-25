package interface_adapter.todo;

import use_case.todo.add_task.AddTaskInputBoundary;
import use_case.todo.add_task.AddTaskInputData;
import use_case.todo.list_tasks.ListTasksInputBoundary;
import use_case.todo.list_tasks.ListTasksInputData;

public final class TodoController {
    private final AddTaskInputBoundary addTask;
    private final ListTasksInputBoundary listTasks;

    public TodoController(AddTaskInputBoundary addTask, ListTasksInputBoundary listTasks) {
        this.addTask = addTask;
        this.listTasks = listTasks;
    }

    public void loadTasks() {
        listTasks.execute(new ListTasksInputData());
    }

    public void addTask(String title) {
        addTask.execute(new AddTaskInputData(title));
    }
}
