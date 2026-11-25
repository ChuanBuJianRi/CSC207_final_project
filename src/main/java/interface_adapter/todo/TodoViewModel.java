package interface_adapter.todo;

import entity.Task;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public final class TodoViewModel {
    public static final String TASKS_LOADED = "tasksLoaded";
    public static final String TASK_ADDED = "taskAdded";
    public static final String ERROR = "error";

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final List<Task> tasks = new ArrayList<>();

    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    void taskAdded(Task task) {
        tasks.add(task);
        changes.firePropertyChange(TASK_ADDED, null, task);
    }

    void replaceTasks(List<Task> loadedTasks) {
        tasks.clear();
        tasks.addAll(loadedTasks);
        changes.firePropertyChange(TASKS_LOADED, null, loadedTasks);
    }

    void showError(String message) {
        changes.firePropertyChange(ERROR, null, message);
    }
}
