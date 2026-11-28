# Agent interfaces

The agent use case sends one user message to a chosen model, adds relevant
memories to the prompt, runs any requested tools, and saves the turn.

## Flow

```text
AgentPanel -> AgentController -> AgentInputBoundary (AgentInteractor)
AgentInteractor -> AgentDataAccessInterface   (findRelevant, save)
AgentInteractor -> AgentToolRegistry          (getDefinitions, execute)
AgentInteractor -> OpenAiGateway              (complete)
AgentInteractor -> AgentOutputBoundary (AgentPresenter) -> AgentViewModel -> AgentPanel
```

`AppBuilder` creates every object. `AgentInteractor` depends only on the
interfaces in `use_case.agent`.

## Use case layer (`use_case.agent`)

### `AgentInputBoundary`

```java
void execute(AgentInputData agentInputData);
```

Implemented by `AgentInteractor`.

### `AgentInputData`

| Field | Meaning |
| --- | --- |
| `userMessage` | Text typed by the user. |
| `model` | Model id, for example `gpt-6.1-sol` or `deepseek-chat`. |
| `effort` | `low`, `medium`, `high`, or `""` for the model default. |

Constructors: `AgentInputData(userMessage, model)` sets effort to `""`;
`AgentInputData(userMessage, model, effort)`.

### `AgentOutputBoundary`

```java
void prepareSuccessView(AgentOutputData agentOutputData);
void prepareFailView(String errorMessage);
```

Implemented by `AgentPresenter`.

### `AgentOutputData`

| Field | Meaning |
| --- | --- |
| `response` | Final model reply (or `"Done."` after successful tools with no text). |
| `model` | Model that produced the reply. |
| `errorMessage` | `null` on success. |

`isSuccess()` returns true when `errorMessage` is null or blank.

### `OpenAiGateway`

```java
OpenAiResponse complete(OpenAiRequest request);
```

Implemented by `data_access.OpenaiClient`. It must not throw; failures come
back as `OpenAiResponse.failure(...)`.

`OpenAiRequest` holds `model`, a list of `AgentMessage`s, `effort`, and a list
of `AgentToolDefinition`s. The legacy `(model, userMessage[, effort])`
constructors still create one user message; `getUserMessage()` returns the last
user message.

`OpenAiResponse` is built with `success(content)`, `failure(errorMessage)`,
`toolCalls(content, calls)`, or `toolCalls(content, calls, rawContentJson)`.
It exposes `isSuccess()`, `getContent()`, `getErrorMessage()`,
`getToolCalls()`, `hasToolCalls()`, and `getRawAssistantContentJson()`.

### Tool types

```java
AgentToolCall       // id, name, argumentsJson
AgentToolResult     // callId, name, success, content
AgentToolDefinition // name, description, parametersJson (JSON Schema text)
AgentToolExecutor   // definition() + execute(call)
AgentToolRegistry   // getDefinitions() + execute(call)
```

`AgentToolDispatcher` (in `interface_adapter.agent`) implements
`AgentToolRegistry`; it routes by name, rejects duplicates, and converts
executor exceptions into failure results. See `agent-tools.md` for the Notes
tool contract.

### `AgentDataAccessInterface`

```java
void save(Memory memory);
List<Memory> findRelevant(String query);
```

| Implementation | Settings | Notes |
| --- | --- | --- |
| `SqliteMemoryDAO` (used by `AppBuilder`) | `NOTES_DB_PATH` (shared with notes) | Local `memory(id, content, source)` table in `data/notes.db`; substring search over the newest 20 rows. |
| `VectorDataAccess` | `VECTOR_DB_URL`, `VECTOR_DB_API_KEY` | Legacy/optional remote vector store; not used by the default app wiring. |
| `MySqlDataAccess` | `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` | Optional MySQL implementation of the same interface. |

## Entity (`entity`)

### `Memory`

| Field | Meaning |
| --- | --- |
| `id` | Unique id; the interactor uses a random UUID. |
| `content` | Saved text. |
| `source` | `user` or `assistant` for chat turns. |

## Interactor behaviour

`AgentInteractor.execute` trims every input, then:

1. Blank model: fail with `Choose a model.`
2. Blank message: fail with `Enter a message.`
3. `findRelevant(userMessage)`. If it throws: fail with
   `Could not load memories: <reason>` and skip the model.
4. Build messages: a system prompt plus the user prompt. With no memories the
   user prompt is the message alone. Otherwise:
   ```
   Earlier notes that may be relevant:
   - [<source>] <content>

   User message:
   <message>
   ```
5. Call `complete(...)` with the available tool definitions.
6. If the response has tool calls: append the assistant message (including the
   raw Claude content for replay), execute each call through the registry,
   append one grouped tool-results message, and call the model again.
   At most 5 tool rounds; otherwise fail with
   `The agent used too many tool steps.`
7. Tool failures are sent back as result content; the model can explain or
   retry.
8. On the final text, save the user message and the reply as two `Memory`
   objects. If saving throws: fail with the reply followed by
   `This reply was not saved: <reason>`.
9. Success view with the reply and model. If the final text is blank after
   tools succeeded, the reply is `Done.`

## Interface adapters (`interface_adapter.agent`)

| Class | Public methods |
| --- | --- |
| `AgentController` | `execute(String userMessage, String model, String effort)` |
| `AgentPresenter` | Success sets `response`, `model`, and clears `errorMessage`. Fail sets `errorMessage` only. |
| `AgentViewModel` | Getters and setters for `response`, `model`, `errorMessage`, all `""` by default. |
| `AgentToolDispatcher` | `getDefinitions()`, `execute(AgentToolCall)`, constructor with a list of executors. |

## Models and effort

`AgentPanel` offers `gpt-6.1-sol`, `gemini-3.8-flash`, `claude-opus-5-5` and
`deepseek-chat`; the combo is editable, so gateway-specific model ids can be
typed. The effort slider offers Low, Medium, High (default Medium).
`OpenaiClient` picks the protocol from the model id:

| Model | Endpoint | Effort field |
| --- | --- | --- |
| `gpt-6.1-sol` / other | `{root}/v1/chat/completions` | `reasoning_effort` |
| `deepseek-*` | `{root}/v1/chat/completions` | omitted |
| `gemini-3.8-flash` | `{root}/v1beta/models/gemini-3.8-flash:generateContent` | `generationConfig.thinkingConfig.thinkingLevel` (`LOW`/`MEDIUM`/`HIGH`) |
| `claude-opus-5-5` | `{root}/v1/messages` | `output_config.effort`, adaptive thinking, `max_tokens` 16000 |

Every request sends `Authorization: Bearer <key>`. Timeout is 300 seconds.

`OpenaiClient.fromEnvironment()` reads system environment variables first,
then `.env`:

| Setting | Keys, in order |
| --- | --- |
| API key | `OPENAI_ADAPTER_KEY`, `OPENAI_API_KEY` |
| Base URL | `OPENAI_BASE_URL`, `SOL_MODEL_URL`, `OPUS_MODEL_URL`, `GEMINI_MODEL_URL` (default OpenAI) |

## Tool calling details

- OpenAI/DeepSeek: `tools` with function schemas; tool calls are read from
  `choices[0].message.tool_calls`; results are `role:"tool"` messages.
- Gemini: `functionDeclarations`; calls arrive as `functionCall` parts;
  results are `functionResponse` parts in one user content.
- Claude: `tools` with `input_schema`; calls arrive as `tool_use` blocks;
  results are `tool_result` blocks in one user message. The raw assistant
  content array is replayed so thinking blocks survive the loop.

The tool execution path is documented in `agent-tools.md`.
