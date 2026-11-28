package app;

import data_access.InMemoryTaskDAO;
import data_access.OpenaiClient;
import data_access.SqliteDatabase;
import data_access.SqliteMemoryDAO;
import data_access.SqliteNoteDAO;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentPresenter;
import interface_adapter.agent.AgentToolDispatcher;
import interface_adapter.agent.AgentViewModel;
import interface_adapter.notes.CreateNoteTool;
import interface_adapter.notes.DeleteNoteTool;
import interface_adapter.notes.ListNotesTool;
import interface_adapter.notes.NotesController;
import interface_adapter.notes.NotesPresenter;
import interface_adapter.notes.NotesToolCaptures;
import interface_adapter.notes.NotesViewModel;
import interface_adapter.notes.ReadNoteTool;
import interface_adapter.notes.UpdateNoteTool;
import interface_adapter.todo.TodoController;
import interface_adapter.todo.TodoPresenter;
import interface_adapter.todo.TodoViewModel;
import use_case.agent.AgentInteractor;
import use_case.agent.AgentToolExecutor;
import use_case.notes.create_note.CreateNoteInteractor;
import use_case.notes.delete_note.DeleteNoteInteractor;
import use_case.notes.get_note.GetNoteInteractor;
import use_case.notes.list_notes.ListNotesInteractor;
import use_case.notes.update_note.UpdateNoteInteractor;
import use_case.todo.TaskDataAccessInterface;
import use_case.todo.add_task.AddTaskInteractor;
import use_case.todo.complete_task.CompleteTaskInteractor;
import use_case.todo.list_tasks.ListTasksInteractor;
import view.AgentPanel;
import view.MainFrame;
import view.NotesPanel;
import view.TodoPanel;

import java.util.List;

public class AppBuilder {
    public MainFrame build() {
        SqliteDatabase database = SqliteDatabase.fromEnvironment();
        database.initializeSchema();
        SqliteNoteDAO noteDataAccess = new SqliteNoteDAO(database);
        SqliteMemoryDAO memoryDataAccess = new SqliteMemoryDAO(database);
        OpenaiClient openaiClient = OpenaiClient.fromEnvironment();

        NotesModule notes = buildNotesModule(noteDataAccess, memoryDataAccess, openaiClient);
        AgentPanel agentPanel = buildAgentPanel(memoryDataAccess, openaiClient, notes.dispatcher);
        return new MainFrame(agentPanel, buildTodoPanel(), notes.panel);
    }

    private NotesModule buildNotesModule(SqliteNoteDAO noteDataAccess,
                                         SqliteMemoryDAO memoryDataAccess,
                                         OpenaiClient openaiClient) {
        NotesViewModel viewModel = new NotesViewModel();
        NotesPresenter presenter = new NotesPresenter(viewModel);

        NotesController controller = new NotesController(
                new CreateNoteInteractor(noteDataAccess, presenter),
                new ListNotesInteractor(noteDataAccess, presenter),
                new UpdateNoteInteractor(noteDataAccess, presenter),
                new DeleteNoteInteractor(noteDataAccess, presenter));

        NotesToolCaptures.Create createCapture = new NotesToolCaptures.Create(presenter);
        NotesToolCaptures.List listCapture = new NotesToolCaptures.List(null);
        NotesToolCaptures.Get getCapture = new NotesToolCaptures.Get();
        NotesToolCaptures.Update updateCapture = new NotesToolCaptures.Update(presenter);
        NotesToolCaptures.Delete deleteCapture = new NotesToolCaptures.Delete(presenter);

        List<AgentToolExecutor> tools = List.of(
                new ListNotesTool(new ListNotesInteractor(noteDataAccess, listCapture), listCapture),
                new ReadNoteTool(new GetNoteInteractor(noteDataAccess, getCapture), getCapture),
                new CreateNoteTool(new CreateNoteInteractor(noteDataAccess, createCapture), createCapture),
                new UpdateNoteTool(new UpdateNoteInteractor(noteDataAccess, updateCapture), updateCapture),
                new DeleteNoteTool(new DeleteNoteInteractor(noteDataAccess, deleteCapture), deleteCapture));
        AgentToolDispatcher dispatcher = new AgentToolDispatcher(tools);

        AgentViewModel notesAgentViewModel = new AgentViewModel();
        AgentPresenter notesAgentPresenter = new AgentPresenter(notesAgentViewModel);
        AgentInteractor notesAgentInteractor = new AgentInteractor(
                openaiClient, notesAgentPresenter, memoryDataAccess, dispatcher);

        NotesPanel panel = new NotesPanel(controller, viewModel,
                new AgentController(notesAgentInteractor), notesAgentViewModel);
        panel.refresh();
        return new NotesModule(panel, dispatcher);
    }

    private AgentPanel buildAgentPanel(SqliteMemoryDAO memoryDataAccess,
                                       OpenaiClient openaiClient,
                                       AgentToolDispatcher dispatcher) {
        AgentViewModel viewModel = new AgentViewModel();
        AgentPresenter presenter = new AgentPresenter(viewModel);
        AgentInteractor interactor = new AgentInteractor(
                openaiClient, presenter, memoryDataAccess, dispatcher);
        return new AgentPanel(new AgentController(interactor), viewModel);
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

    private static final class NotesModule {
        private final NotesPanel panel;
        private final AgentToolDispatcher dispatcher;

        private NotesModule(NotesPanel panel, AgentToolDispatcher dispatcher) {
            this.panel = panel;
            this.dispatcher = dispatcher;
        }
    }
}
