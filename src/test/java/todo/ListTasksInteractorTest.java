package todo;

import data_access.InMemoryTaskDAO;
import entity.Task;
import org.junit.jupiter.api.Test;
import use_case.todo.TaskDataAccessInterface;
import use_case.todo.list_tasks.ListTasksInputData;
import use_case.todo.list_tasks.ListTasksInteractor;
import use_case.todo.list_tasks.ListTasksOutputBoundary;
import use_case.todo.list_tasks.ListTasksOutputData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListTasksInteractorTest {
    private final RecordingPresenter presenter = new RecordingPresenter();

    @Test
    void returnsTheSavedTasksInOrderWithTheirCompletionState() {
        InMemoryTaskDAO dataAccess = new InMemoryTaskDAO();
        Task pending = new Task("1", "Review CSC207", false);
        Task completed = new Task("2", "Read project guide", true);
        dataAccess.save(pending);
        dataAccess.save(completed);

        new ListTasksInteractor(dataAccess, presenter).execute(new ListTasksInputData());

        assertEquals(List.of(pending, completed), presenter.output.getTasks());
        assertNull(presenter.error);
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoTasks() {
        new ListTasksInteractor(new InMemoryTaskDAO(), presenter).execute(new ListTasksInputData());

        assertEquals(List.of(), presenter.output.getTasks());
        assertNull(presenter.error);
    }

    @Test
    void reportsAReadFailureWithoutReportingAnEmptyList() {
        TaskDataAccessInterface failingDataAccess = new TaskDataAccessInterface() {
            @Override
            public void save(Task task) {
                throw new AssertionError("Loading tasks must not write to storage.");
            }

            @Override
            public List<Task> findAll() {
                throw new IllegalStateException("Store is unavailable.");
            }
        };

        new ListTasksInteractor(failingDataAccess, presenter).execute(new ListTasksInputData());

        assertEquals("Could not load tasks. Try again.", presenter.error);
        assertNull(presenter.output);
    }

    private static final class RecordingPresenter implements ListTasksOutputBoundary {
        private ListTasksOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(ListTasksOutputData output) {
            this.output = output;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
