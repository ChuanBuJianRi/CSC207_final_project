package todo;

import data_access.InMemoryTaskDAO;
import entity.Task;
import org.junit.jupiter.api.Test;
import use_case.todo.TaskDataAccessInterface;
import use_case.todo.complete_task.CompleteTaskInputData;
import use_case.todo.complete_task.CompleteTaskInteractor;
import use_case.todo.complete_task.CompleteTaskOutputBoundary;
import use_case.todo.complete_task.CompleteTaskOutputData;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteTaskInteractorTest {
    private final InMemoryTaskDAO dataAccess = new InMemoryTaskDAO();
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final CompleteTaskInteractor interactor = new CompleteTaskInteractor(dataAccess, presenter);

    @Test
    void completesOnlyTheMatchingIdEvenWhenTitlesAreTheSame() {
        Task first = new Task("1", "Review CSC207", false);
        Task second = new Task("2", "Review CSC207", false);
        dataAccess.save(first);
        dataAccess.save(second);

        interactor.execute(new CompleteTaskInputData("2"));

        List<Task> tasks = dataAccess.findAll();
        assertEquals(2, tasks.size());
        assertSame(first, tasks.get(0));
        assertFalse(tasks.get(0).isCompleted());
        assertEquals("2", presenter.task.getId());
        assertEquals(second.getTitle(), presenter.task.getTitle());
        assertTrue(presenter.task.isCompleted());
        assertSame(presenter.task, tasks.get(1));
        assertNull(presenter.error);
    }

    @Test
    void completingAnAlreadyCompletedTaskKeepsItsState() {
        Task completed = new Task("1", "Review CSC207", true);
        dataAccess.save(completed);

        interactor.execute(new CompleteTaskInputData("1"));

        assertSame(completed, presenter.task);
        assertEquals(List.of(completed), dataAccess.findAll());
        assertNull(presenter.error);
    }

    @Test
    void reportsAnUnknownIdWithoutAddingATask() {
        interactor.execute(new CompleteTaskInputData("unknown"));

        assertEquals("Task not found.", presenter.error);
        assertNull(presenter.task);
        assertTrue(dataAccess.findAll().isEmpty());
    }

    @Test
    void rejectsABlankTaskId() {
        interactor.execute(new CompleteTaskInputData(" "));

        assertEquals("Select a task to complete.", presenter.error);
        assertNull(presenter.task);
    }

    @Test
    void reportsAStorageFailure() {
        TaskDataAccessInterface failingStore = new TaskDataAccessInterface() {
            @Override
            public void save(Task task) {
                throw new AssertionError("Must not insert a task.");
            }

            @Override
            public List<Task> findAll() {
                throw new AssertionError("Must update the task directly by ID.");
            }

            @Override
            public Optional<Task> markCompleted(String taskId) {
                throw new IllegalStateException("Storage unavailable.");
            }
        };

        new CompleteTaskInteractor(failingStore, presenter).execute(new CompleteTaskInputData("1"));

        assertEquals("Could not complete the task. Try again.", presenter.error);
        assertNull(presenter.task);
    }

    private static final class RecordingPresenter implements CompleteTaskOutputBoundary {
        private Task task;
        private String error;

        @Override
        public void prepareSuccessView(CompleteTaskOutputData output) {
            task = output.getTask();
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
