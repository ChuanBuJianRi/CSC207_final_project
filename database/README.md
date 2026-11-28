# Database

The app uses one embedded **SQLite** database for notes and agent memories.
No server or account is required.

## Location

- Default: `data/notes.db` relative to the working directory.
- Override: set the `NOTES_DB_PATH` environment variable to an absolute or
  relative path.
- The folder is created on first run; the database file is created and the
  schema is applied automatically. `data/` and `*.db*` files are gitignored.

## Schema (version 1)

```sql
CREATE TABLE note (
  id         TEXT PRIMARY KEY,
  title      TEXT NOT NULL,
  markdown   TEXT NOT NULL,
  created_at TEXT NOT NULL,   -- ISO-8601 UTC, 9 fractional digits
  updated_at TEXT NOT NULL
);

CREATE TABLE note_tag (
  note_id TEXT NOT NULL REFERENCES note(id) ON DELETE CASCADE,
  tag     TEXT COLLATE NOCASE NOT NULL,
  PRIMARY KEY (note_id, tag)
);

CREATE TABLE memory (
  id      TEXT PRIMARY KEY,
  content TEXT NOT NULL,
  source  TEXT
);

CREATE INDEX idx_note_updated ON note(updated_at DESC);
CREATE INDEX idx_note_tag_tag ON note_tag(tag);
```

`PRAGMA user_version = 1` is written by `SqliteDatabase.initializeSchema()`.
`foreign_keys = ON` and a 5-second busy timeout are set on every connection;
the database uses WAL mode.

## Notes

- Tags are case-insensitive for uniqueness and filtering, while the first
  spelling is preserved for display.
- Notes are listed newest first (`updated_at DESC`).
- Search uses `instr(lower(...), lower(...))` on the title and markdown, so
  `%` and `_` are literal characters.
- Deleting a note cascades to its tags.
- Agent chat turns are stored in `memory`; `findRelevant` performs a simple
  substring search over recent memories.

## Reset and backup

- Reset: close the app, delete `data/notes.db` (and any `-wal`/`-shm` files),
  then start the app again.
- Backup: close the app and copy `data/notes.db`.
- To use a different file, set `NOTES_DB_PATH`, for example:

```bash
NOTES_DB_PATH=/tmp/study-notes.db mvn -q compile exec:java
```
