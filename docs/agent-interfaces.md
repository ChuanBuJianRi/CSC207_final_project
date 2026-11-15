# Agent interfaces

The agent use case sends one user message to a chosen model, adds relevant memories to the prompt, and saves the turn.

## Flow

```
AgentPanel -> AgentController -> AgentInputBoundary (AgentInteractor)
AgentInteractor -> AgentDataAccessInterface   (findRelevant, save)
AgentInteractor -> OpenAiGateway              (complete)
AgentInteractor -> AgentOutputBoundary (AgentPresenter) -> AgentViewModel -> AgentPanel
```

`AppBuilder` creates every object. `AgentInteractor` depends only on the three interfaces in `use_case.agent`.

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
| `model` | Model id, for example `gpt-6.1-sol`. |
| `effort` | `low`, `medium`, `high`, or `""` for the model default. |

Constructors: `AgentInputData(userMessage, model)` sets effort to `""`; `AgentInputData(userMessage, model, effort)`.

### `AgentOutputBoundary`

```java
void prepareSuccessView(AgentOutputData agentOutputData);
void prepareFailView(String errorMessage);
```

Implemented by `AgentPresenter`.

### `AgentOutputData`

| Field | Meaning |
| --- | --- |
| `response` | Model reply. |
| `model` | Model that produced the reply. |
| `errorMessage` | `null` on success. |

`isSuccess()` returns true when `errorMessage` is null or blank.

### `OpenAiGateway`

```java
OpenAiResponse complete(OpenAiRequest request);
```

Implemented by `data_access.OpenaiClient`. It must not throw; failures come back as `OpenAiResponse.failure(...)`.

`OpenAiRequest` holds `model`, `userMessage` (the full prompt, memories included), and `effort`.
`OpenAiResponse` is built with `success(content)` or `failure(errorMessage)` and exposes `isSuccess()`, `getContent()`, `getErrorMessage()`.

### `AgentDataAccessInterface`

```java
void save(Memory memory);
List<Memory> findRelevant(String query);
```

Implementations may throw `RuntimeException`; `AgentInteractor` turns it into a failed view.

| Implementation | Settings | Notes |
| --- | --- | --- |
| `VectorDataAccess` (used by `AppBuilder`) | `VECTOR_DB_URL`, `VECTOR_DB_API_KEY` | `save` posts to `{url}/memories`; `findRelevant` posts `{"query": ...}` to `{url}/memories/search`. Placeholder endpoints until the team picks a vector database. |
| `MySqlDataAccess` | `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` | Table `memory(id, content, source)`. `findRelevant` matches the id or a `LIKE` on content. Also has `findById(String id)`. |

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
3. `findRelevant(userMessage)`. If it throws: fail with `Could not load memories: <reason>` and skip the model.
4. Build the prompt. With no memories it is the message alone. Otherwise:
   ```
   Earlier notes that may be relevant:
   - [<source>] <content>

   User message:
   <message>
   ```
5. `complete(...)`. On failure: fail with the gateway error.
6. Save the user message and the reply as two `Memory` objects. If saving throws: fail with the reply followed by `This reply was not saved: <reason>`.
7. Success view with the reply and model.

## Interface adapters (`interface_adapter.agent`)

| Class | Public methods |
| --- | --- |
| `AgentController` | `execute(String userMessage, String model, String effort)` |
| `AgentPresenter` | Success sets `response`, `model`, and clears `errorMessage`. Fail sets `errorMessage` only. |
| `AgentViewModel` | Getters and setters for `response`, `model`, `errorMessage`, all `""` by default. |

## Models and effort

`AgentPanel` offers three models and an effort slider (Low, Medium, High; default Medium). `OpenaiClient` picks the protocol from the model id:

| Model | Endpoint | Effort field |
| --- | --- | --- |
| `gpt-6.1-sol` | `{root}/v1/chat/completions` | `reasoning_effort` |
| `gemini-3.8-flash` | `{root}/v1beta/models/gemini-3.8-flash:generateContent` | `generationConfig.thinkingConfig.thinkingLevel` (`LOW`/`MEDIUM`/`HIGH`) |
| `claude-opus-5-5` | `{root}/v1/messages` | `output_config.effort`, with adaptive thinking and `max_tokens` 16000 |

Every request sends `Authorization: Bearer <key>`. Timeout is 300 seconds.

`OpenaiClient.fromEnvironment()` reads system environment variables first, then `.env`:

| Setting | Keys, in order |
| --- | --- |
| API key | `OPENAI_ADAPTER_KEY`, `OPENAI_API_KEY` |
| Base URL | `OPENAI_BASE_URL`, `SOL_MODEL_URL`, `OPUS_MODEL_URL`, `GEMINI_MODEL_URL` (default OpenAI) |

## Not built yet

`AgentToolCall`, `AgentToolResult`, `AgentToolExecutor`, and `AgentToolDispatcher` are empty. They wait for the other modules' input boundaries; see `agent-tools.md`.
