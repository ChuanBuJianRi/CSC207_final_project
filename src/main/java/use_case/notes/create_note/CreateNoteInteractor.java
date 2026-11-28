package use_case.notes.create_note;

import entity.Note;
import use_case.notes.NoteDataAccessInterface;
import use_case.notes.NoteTags;

import java.time.Instant;
import java.util.UUID;

public final class CreateNoteInteractor implements CreateNoteInputBoundary {
    private final NoteDataAccessInterface noteDataAccess;
    private final CreateNoteOutputBoundary presenter;

    public CreateNoteInteractor(NoteDataAccessInterface noteDataAccess,
                                CreateNoteOutputBoundary presenter) {
        this.noteDataAccess = noteDataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(CreateNoteInputData input) {
        String title = input.getTitle() == null ? "" : input.getTitle().trim();
        if (title.isEmpty()) {
            presenter.prepareFailView("Enter a note title.");
            return;
        }
        String markdown = input.getMarkdown() == null ? "" : input.getMarkdown();
        Instant now = Instant.now();
        Note note = new Note(
                UUID.randomUUID().toString(),
                title,
                markdown,
                NoteTags.normalize(input.getTags()),
                now,
                now);
        try {
            noteDataAccess.save(note);
        } catch (IllegalStateException exception) {
            presenter.prepareFailView("Could not save the note. Try again.");
            return;
        }
        presenter.prepareSuccessView(new CreateNoteOutputData(note));
    }
}
