package use_case.notes.list_notes;

import entity.Note;
import use_case.notes.NoteDataAccessInterface;

import java.util.List;

public final class ListNotesInteractor implements ListNotesInputBoundary {
    private final NoteDataAccessInterface noteDataAccess;
    private final ListNotesOutputBoundary presenter;

    public ListNotesInteractor(NoteDataAccessInterface noteDataAccess,
                               ListNotesOutputBoundary presenter) {
        this.noteDataAccess = noteDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ListNotesInputData input) {
        String query = input.getQuery() == null ? "" : input.getQuery().trim();
        String tag = input.getTag() == null ? "" : input.getTag().trim();
        List<Note> notes;
        List<String> tags;
        try {
            notes = noteDataAccess.search(query, tag);
            tags = noteDataAccess.findAllTags();
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not load notes. Try again.");
            return;
        }
        presenter.prepareSuccessView(new ListNotesOutputData(notes, tags));
    }
}
