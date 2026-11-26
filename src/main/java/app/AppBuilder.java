package app;

import data_access.InMemoryTaskDAO;
import data_access.OpenaiClient;
import data_access.VectorDataAccess;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentPresenter;
import interface_adapter.agent.AgentViewModel;
import interface_adapter.todo.TodoController;
import interface_adapter.todo.TodoPresenter;
import interface_adapter.todo.TodoViewModel;
import use_case.agent.AgentInteractor;
import use_case.todo.TaskDataAccessInterface;
import use_case.todo.add_task.AddTaskInteractor;
import use_case.todo.complete_task.CompleteTaskInteractor;
import use_case.todo.list_tasks.ListTasksInteractor;
import view.AgentPanel;
import view.MainFrame;
import view.TodoPanel;

public class AppBuilder {
    public MainFrame build() {
        AgentViewModel agentViewModel = new AgentViewModel();
        AgentPresenter agentPresenter = new AgentPresenter(agentViewModel);
        AgentInteractor agentInteractor = new AgentInteractor(
                OpenaiClient.fromEnvironment(),
                agentPresenter,
                VectorDataAccess.fromEnvironment());
        AgentController agentController = new AgentController(agentInteractor);
        AgentPanel agentPanel = new AgentPanel(agentController, agentViewModel);
        return new MainFrame(agentPanel, buildTodoPanel());
    }

    private TodoPanel buildTodoPanel() {
        TodoViewModel viewModel = new TodoViewModel();
        TodoPresenter presenter = new TodoPresenter(viewModel);
        TaskDataAccessInterface taskDataAccess = new InMemoryTaskDAO();
        AddTaskInteractor addTask = new AddTaskInteractor(taskDataAccess, presenter);
        ListTasksInteractor listTasks = new ListTasksInteractor(taskDataAccess, presenter);
        CompleteTaskInteractor completeTask = new CompleteTaskInteractor(taskDataAccess, presenter);
        return new TodoPanel(new TodoController(addTask, listTasks, completeTask), viewModel);
    }
}
