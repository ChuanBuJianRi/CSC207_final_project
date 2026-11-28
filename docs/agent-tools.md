# Agent tools

The agent has native tool access to the Notes module. Tools are registered in
`AgentToolDispatcher` and invoked by `AgentInteractor`; the model chooses a
tool, the dispatcher calls the relevant notes **input boundary**, and the
confirmed result is sent back to the model.

## Notes tools

| Tool | Parameters | Result |
| --- | --- | --- |
| `list_notes` | `query` (optional string), `tag` (optional string), `limit` (optional integer 1-50, default 20) | `{notes, count, more?}`; each note has `id`, `title`, `tags`, `updatedAt` |
| `read_note` | `id` (required string) | note metadata plus `markdown` (truncated after 20,000 characters) |
| `create_note` | `title` (required string), `markdown` (optional string), `tags` (optional string array) | new note `id`, `title`, `tags`, `updatedAt` |
| `update_note` | `id` (required string), plus at least one of `title`, `markdown`, `tags` | updated note `id`, `title`, `tags`, `updatedAt` |
| `delete_note` | `id` (required string) | `{deleted:true, id}` |

`update_note` replaces only the fields it receives. `delete_note` is permanent
and should only be used when the user explicitly asks to delete a note; the
tool description says so.

### Tool schemas

The tools use simple JSON Schema (`type`, `properties`, `required`,
`description`). The source schemas are provider-neutral:

```json
{
  "type": "object",
  "properties": {
    "query": {"type": "string", "description": "Text to match in titles or markdown."},
    "tag": {"type": "string", "description": "Only return notes with this tag."},
    "limit": {"type": "integer", "description": "Maximum notes to return (1-50, default 20)."}
  }
}
```

`read_note`, `delete_note`, `create_note` and `update_note` use the same shape
with their respective fields. Gemini receives an OpenAPI-style conversion of
the schema (uppercase type names and only supported keywords).

## Java layers

```text
AgentToolDispatcher
  -> ListNotesInputBoundary / GetNoteInputBoundary /
     CreateNoteInputBoundary / UpdateNoteInputBoundary / DeleteNoteInputBoundary
  -> NotesToolCaptures.*
       -> shared NotesPresenter (write tools only) -> NotesViewModel -> NotesPanel
  -> AgentToolResult -> AgentInteractor -> model
```

- Read/list tools only capture their output; they do **not** replace the
  user's visible note list.
- Create/update/delete tools delegate to the shared `NotesPresenter` so the
  Notes tab refreshes. `NotesPanel` reloads with its current search/tag filter.
- Tool failures are returned to the model as error content, so it can explain
  or retry; they do not crash the chat.
- The agent loop is bounded to 5 tool rounds; each assistant tool-call turn is
  followed by one grouped tool-result message.

## Provider support

| Provider | Request shape | Tool result shape |
| --- | --- | --- |
| OpenAI / DeepSeek | `tools:[{type:"function",function:{name,description,parameters}}]`, `choices[].message.tool_calls` | `role:"tool"` messages with `tool_call_id` |
| Gemini | `tools:[{functionDeclarations:[...]}]`, `candidates[].content.parts[].functionCall` | `functionResponse` parts in one user content |
| Claude | `tools:[{name,description,input_schema}]`, content blocks with `type:"tool_use"` | `tool_result` blocks in one user message |

DeepSeek uses the OpenAI-compatible path; `reasoning_effort` is omitted for
`deepseek-*` models. Use `deepseek-chat` for tool calling; `deepseek-reasoner`
may reject tools.

## Failure cases

- Missing or invalid JSON arguments.
- Missing required fields (`title`, `id`).
- Unknown note id.
- Blank title.
- Storage/database failure.
- Deleting an already-deleted note.
- Provider tool-support or API errors.

Tool errors are passed back to the model as `AgentToolResult.failure(...)`
content; the UI receives the same error through `NotesPresenter` for write
tools.

## Configuration

No tool-specific configuration is required. Model credentials and the local
database path are documented in `README.md` and `database/README.md`.
