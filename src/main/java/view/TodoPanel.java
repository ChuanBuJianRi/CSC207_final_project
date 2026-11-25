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
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;

public final class TodoPanel extends JPanel {
    private final JTextField titleField = new JTextField(24);
    private final DefaultListModel<Task> tasks = new DefaultListModel<>();
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
        JList<Task> taskList = new JList<>(tasks);
        taskList.getAccessibleContext().setAccessibleName("Tasks");
        taskList.setVisibleRowCount(8);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        taskList.setCellRenderer((list, task, index, selected, focused) ->
                renderer.getListCellRendererComponent(list, task.getTitle()
                        + (task.isCompleted() ? " (completed)" : " (pending)"), index, selected, focused));

        add(input, BorderLayout.NORTH);
        add(new JScrollPane(taskList), BorderLayout.CENTER);
        add(feedback, BorderLayout.SOUTH);
        viewModel.addPropertyChangeListener(this::updateView);
    }

    private void updateView(PropertyChangeEvent event) {
        if (TodoViewModel.TASK_ADDED.equals(event.getPropertyName())) {
            tasks.addElement((Task) event.getNewValue());
            titleField.setText("");
            feedback.setText("Task added.");
            titleField.requestFocusInWindow();
        } else if (TodoViewModel.ERROR.equals(event.getPropertyName())) {
            feedback.setText((String) event.getNewValue());
        }
    }
}
