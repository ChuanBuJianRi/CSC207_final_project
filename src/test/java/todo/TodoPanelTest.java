package todo;

import data_access.InMemoryTaskDAO;
import entity.Task;
import interface_adapter.todo.TodoController;
import interface_adapter.todo.TodoPresenter;
import interface_adapter.todo.TodoViewModel;
import org.junit.jupiter.api.Test;
import use_case.todo.add_task.AddTaskInteractor;
import use_case.todo.list_tasks.ListTasksInteractor;
import view.TodoPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodoPanelTest {
    @Test
    void addButtonShowsValidationAndRefreshesTheListAfterSaving() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InMemoryTaskDAO dataAccess = new InMemoryTaskDAO();
            TodoViewModel viewModel = new TodoViewModel();
            TodoPresenter presenter = new TodoPresenter(viewModel);
            TodoController controller = new TodoController(
                    new AddTaskInteractor(dataAccess, presenter), new ListTasksInteractor(dataAccess, presenter));
            TodoPanel panel = new TodoPanel(controller, viewModel);

            BorderLayout layout = (BorderLayout) panel.getLayout();
            JPanel input = (JPanel) layout.getLayoutComponent(BorderLayout.NORTH);
            BorderLayout inputLayout = (BorderLayout) input.getLayout();
            JTextField title = (JTextField) inputLayout.getLayoutComponent(BorderLayout.CENTER);
            JButton add = (JButton) inputLayout.getLayoutComponent(BorderLayout.EAST);
            JScrollPane scroll = (JScrollPane) layout.getLayoutComponent(BorderLayout.CENTER);
            JList<?> tasks = (JList<?>) scroll.getViewport().getView();
            JLabel feedback = (JLabel) layout.getLayoutComponent(BorderLayout.SOUTH);

            add.doClick(0);
            assertEquals(0, tasks.getModel().getSize());
            assertEquals("Enter a task title.", feedback.getText());

            title.setText("Review CSC207");
            add.doClick(0);
            assertEquals(1, tasks.getModel().getSize());
            Task displayed = (Task) tasks.getModel().getElementAt(0);
            assertEquals(dataAccess.findAll().get(0), displayed);
            assertEquals("", title.getText());
            assertEquals("Task added.", feedback.getText());
        });
    }

    @Test
    void openingAndReturningToTodoReloadsTheStoredTasksWithoutDuplicates() throws Exception {
        InMemoryTaskDAO dataAccess = new InMemoryTaskDAO();
        Task completed = new Task("1", "Read project guide", true);
        dataAccess.save(completed);

        FutureTask<JTabbedPane> openTodo = new FutureTask<>(() -> {
            TodoViewModel viewModel = new TodoViewModel();
            TodoPresenter presenter = new TodoPresenter(viewModel);
            TodoController controller = new TodoController(
                    new AddTaskInteractor(dataAccess, presenter), new ListTasksInteractor(dataAccess, presenter));
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Agent", new JPanel());
            tabs.addTab("Todo", new TodoPanel(controller, viewModel));
            tabs.setSelectedIndex(1);
            return tabs;
        });
        SwingUtilities.invokeAndWait(openTodo);
        JTabbedPane tabs = openTodo.get();

        SwingUtilities.invokeAndWait(() -> {
            JList<?> tasks = taskList(tabs);
            assertEquals(1, tasks.getModel().getSize());
            assertEquals(completed, tasks.getModel().getElementAt(0));
            assertTrue(((Task) tasks.getModel().getElementAt(0)).isCompleted());
            tabs.setSelectedIndex(0);
        });

        Task pending = new Task("2", "Review CSC207", false);
        dataAccess.save(pending);
        SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));

        SwingUtilities.invokeAndWait(() -> {
            JList<?> tasks = taskList(tabs);
            assertEquals(2, tasks.getModel().getSize());
            assertEquals(completed, tasks.getModel().getElementAt(0));
            assertEquals(pending, tasks.getModel().getElementAt(1));
        });
    }

    private JList<?> taskList(JTabbedPane tabs) {
        TodoPanel panel = (TodoPanel) tabs.getSelectedComponent();
        BorderLayout layout = (BorderLayout) panel.getLayout();
        JScrollPane scroll = (JScrollPane) layout.getLayoutComponent(BorderLayout.CENTER);
        return (JList<?>) scroll.getViewport().getView();
    }
}
