package view;

import entity.Task;
import interface_adapter.todo.TodoController;
import interface_adapter.todo.TodoViewModel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;

public final class TodoPanel extends JPanel {
    private final JTextField titleField = new JTextField(24);
    private final DefaultListModel<Task> tasks = new DefaultListModel<>();
    private final JList<Task> taskList = new JList<>(tasks);
    private final JLabel feedback = new JLabel(" ");

    public TodoPanel(TodoController controller, TodoViewModel viewModel) {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel("Task title");
        titleLabel.setLabelFor(titleField);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(event -> controller.addTask(titleField.getText()));
        titleField.addActionListener(event -> controller.addTask(titleField.getText()));

        JPanel input = new JPanel(new BorderLayout(8, 0));
        input.add(titleLabel, BorderLayout.WEST);
        input.add(titleField, BorderLayout.CENTER);
        input.add(addButton, BorderLayout.EAST);

        viewModel.getTasks().forEach(tasks::addElement);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.getAccessibleContext().setAccessibleName("Tasks");
        taskList.setVisibleRowCount(8);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        taskList.setCellRenderer((list, task, index, selected, focused) ->
                renderer.getListCellRendererComponent(list, task.getTitle()
                        + (task.isCompleted() ? " (completed)" : " (pending)"), index, selected, focused));

        JButton completeButton = new JButton("Complete");
        completeButton.setEnabled(false);
        taskList.addListSelectionListener(event -> {
            Task selected = taskList.getSelectedValue();
            completeButton.setEnabled(selected != null && !selected.isCompleted());
        });
        completeButton.addActionListener(event -> controller.completeTask(taskList.getSelectedValue().getId()));

        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.add(feedback, BorderLayout.CENTER);
        actions.add(completeButton, BorderLayout.EAST);

        add(input, BorderLayout.NORTH);
        add(new JScrollPane(taskList), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        viewModel.addPropertyChangeListener(event -> updateView(event, viewModel));
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent event) {
                controller.loadTasks();
            }
        });
    }

    private void updateView(PropertyChangeEvent event, TodoViewModel viewModel) {
        if (TodoViewModel.TASKS_LOADED.equals(event.getPropertyName())) {
            tasks.clear();
            tasks.addAll(viewModel.getTasks());
            feedback.setText(" ");
        } else if (TodoViewModel.TASK_ADDED.equals(event.getPropertyName())) {
            tasks.addElement((Task) event.getNewValue());
            titleField.setText("");
            feedback.setText("Task added.");
            titleField.requestFocusInWindow();
        } else if (TodoViewModel.TASK_COMPLETED.equals(event.getPropertyName())) {
            int selection = taskList.getSelectedIndex();
            tasks.clear();
            tasks.addAll(viewModel.getTasks());
            taskList.setSelectedIndex(selection);
            feedback.setText("Task completed.");
        } else if (TodoViewModel.ERROR.equals(event.getPropertyName())) {
            feedback.setText((String) event.getNewValue());
        }
    }
}
