package use_case.notes.get_note;

import entity.Note;
import use_case.notes.NoteDataAccessInterface;

import java.util.Optional;

public final class GetNoteInteractor implements GetNoteInputBoundary {
    private final NoteDataAccessInterface noteDataAccess;
    private final GetNoteOutputBoundary presenter;

    public GetNoteInteractor(NoteDataAccessInterface noteDataAccess,
                             GetNoteOutputBoundary presenter) {
        this.noteDataAccess = noteDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(GetNoteInputData input) {
        String noteId = input.getNoteId() == null ? "" : input.getNoteId().trim();
        if (noteId.isEmpty()) {
            presenter.prepareFailView("Select a note.");
            return;
        }
        Optional<Note> note;
        try {
            note = noteDataAccess.findById(noteId);
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not load the note. Try again.");
            return;
        }
        if (note.isEmpty()) {
            presenter.prepareFailView("Note not found.");
            return;
        }
        presenter.prepareSuccessView(new GetNoteOutputData(note.get()));
    }
}
