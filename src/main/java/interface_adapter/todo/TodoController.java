package interface_adapter.todo;

import use_case.todo.add_task.AddTaskInputBoundary;
import use_case.todo.add_task.AddTaskInputData;
import use_case.todo.complete_task.CompleteTaskInputBoundary;
import use_case.todo.complete_task.CompleteTaskInputData;
import use_case.todo.list_tasks.ListTasksInputBoundary;
import use_case.todo.list_tasks.ListTasksInputData;

public final class TodoController {
    private final AddTaskInputBoundary addTask;
    private final ListTasksInputBoundary listTasks;
    private final CompleteTaskInputBoundary completeTask;

    public TodoController(AddTaskInputBoundary addTask, ListTasksInputBoundary listTasks,
                          CompleteTaskInputBoundary completeTask) {
        this.addTask = addTask;
        this.listTasks = listTasks;
        this.completeTask = completeTask;
    }

    public void loadTasks() {
        listTasks.execute(new ListTasksInputData());
    }

    public void addTask(String title) {
        addTask.execute(new AddTaskInputData(title));
    }

    public void completeTask(String taskId) {
        completeTask.execute(new CompleteTaskInputData(taskId));
    }
}
