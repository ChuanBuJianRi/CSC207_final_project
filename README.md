# Team Project

Please keep this up-to-date with information about your project throughout the term.

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses 
- screenshots or animations demonstrating current functionality

By keeping this README up-to-date,
your team will find it easier to prepare for the final presentation
at the end of the term.

## Desktop Assistant

A Java 11 Swing study app with three tabs:

- **Agent** — chat with OpenAI-, Gemini-, Claude- or DeepSeek-backed models.
  The agent can list, read, create, update and delete the user's notes through
  native tool calling.
- **Todo** — the existing in-memory task list.
- **Notes** — Markdown notes with tags, search, and a split source/live
  preview.

### Notes

Write Markdown in the left pane and see the rendered HTML in the right pane.
Use the tag field for comma-separated tags, the search field for title/body
search, and the tag dropdown to filter. Notes are saved explicitly with the
Save button; unsaved edits are protected when switching notes.

The Notes tab also has an **Agent** pane:

- Quick actions: **Summarize**, **Rewrite**, **Generate tags**, **Continue**,
  **Explain**.
- Free-form chat scoped to the open note; pick the model and effort in the
  pane.
- Select text in the Markdown editor or rendered preview, then click
  **Quote to agent** to attach it to the next agent message.
- While the agent works, a pulsing **Agent is working on this note...** banner
  appears. The same banner shows for note-writing tool calls made from the
  Agent tab.
- After the agent edits the note, the right pane gains a **Changes** tab with a
  GitHub-style line diff (green additions, red deletions). Switch back to
  **Preview** at any time to turn the diff view off.
- Agent edits do not overwrite unsaved editor content; the tab shows a warning
  and keeps your text.

### Local data

Notes and agent chat memories are stored in one embedded SQLite database:

- Default path: `data/notes.db` (created on first run, gitignored).
- Override with `NOTES_DB_PATH`.
- No database server, MySQL account, or vector database is needed.
- See `database/README.md` for the schema and reset instructions.

### Model configuration

`OpenaiClient` reads the first nonblank value from the environment or a `.env`
file in the working directory:

- API key: `OPENAI_ADAPTER_KEY` or `OPENAI_API_KEY`.
- Base URL: `OPENAI_BASE_URL`, `SOL_MODEL_URL`, `OPUS_MODEL_URL`, or
  `GEMINI_MODEL_URL` (defaults to `https://api.openai.com/v1`).

The model dropdown is editable. Any model id that does not start with
`gemini-` or `claude-` uses the OpenAI-compatible protocol, which covers
DeepSeek (`deepseek-chat`) as well. Use `deepseek-chat` for notes tool calling;
`deepseek-reasoner` may not support tools.

### How to run

1. Install JDK 11+ and Maven 3.9+ (or use the Maven bundled with your IDE).
2. Configure the model API key and base URL as above.
3. Run the tests: `mvn test`.
4. Launch the app: `mvn -q compile exec:java`, or run `app.Main` from your IDE.

The `data/notes.db` file is created automatically; no manual schema setup is
required.
