package use_case.notes.list_notes;

public final class ListNotesInputData {
    private final String query;
    private final String tag;

    public ListNotesInputData() {
        this("", "");
    }

    public ListNotesInputData(String query, String tag) {
        this.query = query;
        this.tag = tag;
    }

    public String getQuery() {
        return query;
    }

    public String getTag() {
        return tag;
    }
}
