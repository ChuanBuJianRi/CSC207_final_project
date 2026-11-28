package interface_adapter.notes;

import use_case.notes.create_note.CreateNoteOutputBoundary;
import use_case.notes.create_note.CreateNoteOutputData;
import use_case.notes.delete_note.DeleteNoteOutputBoundary;
import use_case.notes.delete_note.DeleteNoteOutputData;
import use_case.notes.get_note.GetNoteOutputBoundary;
import use_case.notes.get_note.GetNoteOutputData;
import use_case.notes.list_notes.ListNotesOutputBoundary;
import use_case.notes.list_notes.ListNotesOutputData;
import use_case.notes.update_note.UpdateNoteOutputBoundary;
import use_case.notes.update_note.UpdateNoteOutputData;

/**
 * Capturing output boundaries used by the agent's notes tools. Write tools
 * delegate to the shared UI presenter; read/list tools only capture.
 */
public final class NotesToolCaptures {
    private NotesToolCaptures() {
    }

    public static final class Create implements CreateNoteOutputBoundary {
        private final NotesPresenter uiPresenter;
        private CreateNoteOutputData output;
        private String error;

        public Create(NotesPresenter uiPresenter) {
            this.uiPresenter = uiPresenter;
        }

        public void beginEditing() {
            if (uiPresenter != null) {
                uiPresenter.beginAgentEdit();
            }
        }

        public void endEditing() {
            if (uiPresenter != null) {
                uiPresenter.endAgentEdit();
            }
        }

        public void reset() {
            output = null;
            error = null;
        }

        public CreateNoteOutputData getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        @Override
        public void prepareSuccessView(CreateNoteOutputData output) {
            this.output = output;
            if (uiPresenter != null) {
                uiPresenter.prepareSuccessView(output);
            }
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
            if (uiPresenter != null) {
                uiPresenter.prepareFailView(errorMessage);
            }
        }
    }

    public static final class List implements ListNotesOutputBoundary {
        private final NotesPresenter uiPresenter;
        private ListNotesOutputData output;
        private String error;

        public List(NotesPresenter uiPresenter) {
            this.uiPresenter = uiPresenter;
        }

        public void reset() {
            output = null;
            error = null;
        }

        public ListNotesOutputData getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        @Override
        public void prepareSuccessView(ListNotesOutputData output) {
            this.output = output;
            if (uiPresenter != null) {
                uiPresenter.prepareSuccessView(output);
            }
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
            if (uiPresenter != null) {
                uiPresenter.prepareFailView(errorMessage);
            }
        }
    }

    public static final class Get implements GetNoteOutputBoundary {
        private GetNoteOutputData output;
        private String error;

        public void reset() {
            output = null;
            error = null;
        }

        public GetNoteOutputData getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        @Override
        public void prepareSuccessView(GetNoteOutputData output) {
            this.output = output;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }

    public static final class Update implements UpdateNoteOutputBoundary {
        private final NotesPresenter uiPresenter;
        private UpdateNoteOutputData output;
        private String error;

        public Update(NotesPresenter uiPresenter) {
            this.uiPresenter = uiPresenter;
        }

        public void beginEditing() {
            if (uiPresenter != null) {
                uiPresenter.beginAgentEdit();
            }
        }

        public void endEditing() {
            if (uiPresenter != null) {
                uiPresenter.endAgentEdit();
            }
        }

        public void reset() {
            output = null;
            error = null;
        }

        public UpdateNoteOutputData getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        @Override
        public void prepareSuccessView(UpdateNoteOutputData output) {
            this.output = output;
            if (uiPresenter != null) {
                uiPresenter.prepareSuccessView(output);
            }
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
            if (uiPresenter != null) {
                uiPresenter.prepareFailView(errorMessage);
            }
        }
    }

    public static final class Delete implements DeleteNoteOutputBoundary {
        private final NotesPresenter uiPresenter;
        private DeleteNoteOutputData output;
        private String error;

        public Delete(NotesPresenter uiPresenter) {
            this.uiPresenter = uiPresenter;
        }

        public void beginEditing() {
            if (uiPresenter != null) {
                uiPresenter.beginAgentEdit();
            }
        }

        public void endEditing() {
            if (uiPresenter != null) {
                uiPresenter.endAgentEdit();
            }
        }

        public void reset() {
            output = null;
            error = null;
        }

        public DeleteNoteOutputData getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        @Override
        public void prepareSuccessView(DeleteNoteOutputData output) {
            this.output = output;
            if (uiPresenter != null) {
                uiPresenter.prepareSuccessView(output);
            }
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
            if (uiPresenter != null) {
                uiPresenter.prepareFailView(errorMessage);
            }
        }
    }
}
