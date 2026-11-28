package interface_adapter.notes;

import use_case.notes.create_note.CreateNoteOutputBoundary;
import use_case.notes.create_note.CreateNoteOutputData;
import use_case.notes.delete_note.DeleteNoteOutputBoundary;
import use_case.notes.delete_note.DeleteNoteOutputData;
import use_case.notes.list_notes.ListNotesOutputBoundary;
import use_case.notes.list_notes.ListNotesOutputData;
import use_case.notes.update_note.UpdateNoteOutputBoundary;
import use_case.notes.update_note.UpdateNoteOutputData;

public final class NotesPresenter implements CreateNoteOutputBoundary, ListNotesOutputBoundary,
        UpdateNoteOutputBoundary, DeleteNoteOutputBoundary {
    private final NotesViewModel viewModel;

    public NotesPresenter(NotesViewModel viewModel) {
        this.viewModel = viewModel;
    }

    void beginAgentEdit() {
        viewModel.setAgentEditing(true);
    }

    void endAgentEdit() {
        viewModel.setAgentEditing(false);
    }

    @Override
    public void prepareSuccessView(CreateNoteOutputData output) {
        viewModel.noteSaved(output.getNote());
    }

    @Override
    public void prepareSuccessView(ListNotesOutputData output) {
        viewModel.replaceNotes(output.getNotes(), output.getTags());
    }

    @Override
    public void prepareSuccessView(UpdateNoteOutputData output) {
        viewModel.noteSaved(output.getNote());
    }

    @Override
    public void prepareSuccessView(DeleteNoteOutputData output) {
        viewModel.noteDeleted(output.getNoteId());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
