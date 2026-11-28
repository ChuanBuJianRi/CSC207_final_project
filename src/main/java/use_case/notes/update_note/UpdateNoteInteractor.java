package use_case.notes.update_note;

import entity.Note;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.NoteTags;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class UpdateNoteInteractor implements UpdateNoteInputBoundary {
    private final NoteDataAccessInterface noteDataAccess;
    private final UpdateNoteOutputBoundary presenter;

    public UpdateNoteInteractor(NoteDataAccessInterface noteDataAccess,
                                UpdateNoteOutputBoundary presenter) {
        this.noteDataAccess = noteDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(UpdateNoteInputData input) {
        String noteId = input.getNoteId() == null ? "" : input.getNoteId().trim();
        if (noteId.isEmpty()) {
            presenter.prepareFailView("Select a note.");
            return;
        }
        if (input.getTitle() == null && input.getMarkdown() == null && input.getTags() == null) {
            presenter.prepareFailView("Provide a note title, markdown, or tags to update.");
            return;
        }
        Optional<Note> existing;
        try {
            existing = noteDataAccess.findById(noteId);
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not load the note. Try again.");
            return;
        }
        if (existing.isEmpty()) {
            presenter.prepareFailView("Note not found.");
            return;
        }
        Note current = existing.get();
        String title = input.getTitle() == null ? current.getTitle() : input.getTitle().trim();
        if (title.isEmpty()) {
            presenter.prepareFailView("Enter a note title.");
            return;
        }
        String markdown = input.getMarkdown() == null ? current.getMarkdown() : input.getMarkdown();
        List<String> tags = input.getTags() == null
                ? current.getTags()
                : NoteTags.normalize(input.getTags());
        Note updated = new Note(
                current.getId(),
                title,
                markdown,
                tags,
                current.getCreatedAt(),
                Instant.now());
        try {
            if (!noteDataAccess.update(updated)) {
                presenter.prepareFailView("Note not found.");
                return;
            }
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not update the note. Try again.");
            return;
        }
        presenter.prepareSuccessView(new UpdateNoteOutputData(updated));
    }
}
