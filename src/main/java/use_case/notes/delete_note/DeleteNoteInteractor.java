package use_case.notes.delete_note;

import use_case.notes.NoteDataAccessInterface;

public final class DeleteNoteInteractor implements DeleteNoteInputBoundary {
    private final NoteDataAccessInterface noteDataAccess;
    private final DeleteNoteOutputBoundary presenter;

    public DeleteNoteInteractor(NoteDataAccessInterface noteDataAccess,
                                DeleteNoteOutputBoundary presenter) {
        this.noteDataAccess = noteDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(DeleteNoteInputData input) {
        String noteId = input.getNoteId() == null ? "" : input.getNoteId().trim();
        if (noteId.isEmpty()) {
            presenter.prepareFailView("Select a note.");
            return;
        }
        try {
            if (!noteDataAccess.delete(noteId)) {
                presenter.prepareFailView("Note not found.");
                return;
            }
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not delete the note. Try again.");
            return;
        }
        presenter.prepareSuccessView(new DeleteNoteOutputData(noteId));
    }
}
