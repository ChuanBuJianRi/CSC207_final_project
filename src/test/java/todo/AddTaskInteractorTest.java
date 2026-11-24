package todo;

import data_access.InMemoryTaskDAO;
import entity.Task;
import org.junit.jupiter.api.Test;
import use_case.todo.TaskDataAccessInterface;
import use_case.todo.add_task.AddTaskInputData;
import use_case.todo.add_task.AddTaskInteractor;
import use_case.todo.add_task.AddTaskOutputBoundary;
import use_case.todo.add_task.AddTaskOutputData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddTaskInteractorTest {
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final InMemoryTaskDAO dataAccess = new InMemoryTaskDAO();
    private final AddTaskInteractor interactor = new AddTaskInteractor(dataAccess, presenter);

    @Test
    void savesAndReportsAnIncompleteTaskWithATrimmedTitle() {
        interactor.execute(new AddTaskInputData("  Review CSC207  "));

        Task saved = dataAccess.findAll().get(0);
        assertEquals("Review CSC207", saved.getTitle());
        assertFalse(saved.isCompleted());
        assertFalse(saved.getId().isBlank());
        assertEquals(saved, presenter.task);
        assertNull(presenter.error);
    }

    @Test
    void givesTasksWithTheSameTitleDifferentIds() {
        interactor.execute(new AddTaskInputData("Review CSC207"));
        interactor.execute(new AddTaskInputData("Review CSC207"));

        List<Task> saved = dataAccess.findAll();
        assertEquals(2, saved.size());
        assertNotEquals(saved.get(0).getId(), saved.get(1).getId());
    }

    @Test
    void rejectsWhitespaceOnlyTitleWithoutSaving() {
        interactor.execute(new AddTaskInputData(" \t\n"));

        assertTrue(dataAccess.findAll().isEmpty());
        assertEquals("Enter a task title.", presenter.error);
        assertNull(presenter.task);
    }

    @Test
    void reportsStorageFailureWithoutReportingSuccess() {
        TaskDataAccessInterface failingDataAccess = new TaskDataAccessInterface() {
            @Override
            public void save(Task task) {
                throw new IllegalStateException("Store is unavailable");
            }

            @Override
            public List<Task> findAll() {
                return List.of();
            }
        };

        new AddTaskInteractor(failingDataAccess, presenter).execute(new AddTaskInputData("Review CSC207"));

        assertEquals("Could not save the task. Try again.", presenter.error);
        assertNull(presenter.task);
    }

    private static final class RecordingPresenter implements AddTaskOutputBoundary {
        private Task task;
        private String error;

        @Override
        public void prepareSuccessView(AddTaskOutputData output) {
            task = output.getTask();
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
