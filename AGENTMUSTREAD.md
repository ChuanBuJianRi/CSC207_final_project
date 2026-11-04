# AGENTMUSTREAD

Any agent working in this folder must read this document before starting a task, and must place files according to the purposes below.

## What this project is

This folder is the workspace for the CSC207H1F 20269 (All Sections) Software Design team project. The full course plan is in `task_requirements/task.md`. That file is an implementation plan. It does not mean the product is already built, and it does not replace the root `README.md`.

The course requires a **Java program with a Java Swing GUI**. A web app will not be approved. JavaFX is allowed only after the course team explicitly approves a request. The program must attempt to follow Clean Architecture and SOLID, use at least one external API, and persist data (through an API, a file, or a database). There must be at least two domain entities. Each team member must complete at least one full user story and lead at least one MVP use case, including most of that use case’s code.

The product itself is not chosen yet. Team name, members, domain, user stories, entities, API, persistence method, JDK, and build tool are still unconfirmed. Do not invent them. Record real decisions in the root README when the team makes them.

## Process

Work from `task_requirements/task.md`. The planned stages are:

1. **P0.** Confirm the official team repository, members, GitHub identities, and course dates.
2. **P1.** Agree on a domain and a short list of user stories, sketch the initial UI, and update the README.
3. **P2.** Prove one external API and one persistence path with a small Java proof of concept.
4. **P3.** Turn stories into use cases, mark the MVP, assign one MVP use case to each member, and define at least two entities.
5. **P4–P5.** Implement one complete flow at a time (UI, use case, data or API, UI update), add unit tests, integrate shared views, and refactor toward Clean Architecture and SOLID.
6. **P6–P7.** Have a teammate reproduce the app from the README, then prepare the group demonstration.

For each story, repeat this loop: confirm scope, plan the UI, write the use case, agree on shared interfaces, implement and unit-test, open a pull request, review, then verify the integrated result. Development must use pull requests and code review throughout, not one upload at the end. The course staff check git history. Each person must have a fair share of real code, and nobody should write another member’s assigned use case for them.

## Push and authorship

The course grades individual contribution from git logs, pull requests, and code review. Authorship must stay with the human team member who owns the work.

- Before every commit and push, the git author and committer must be that human teammate. Never set the author, committer, or GitHub contributor to Cursor, Codex, ChatGPT, Copilot, or any other agent or AI.
- Do not add `Co-authored-by`, `Signed-off-by`, or any trailer that names an agent or AI.
- Do not push unless a teammate asks for that push. Do not force-push, and do not rewrite history in a way that removes a teammate’s authorship.
- Put the change on a branch and open a pull request. The description must name the story or use case, the behavior change, how it was verified, and whether the README or diagrams changed.
- A teammate reviews the pull request. The author of the use case must have written most of its code.
- Do not commit secrets. `.env` and API keys stay out of the repository.

## Main folders

| Folder | Purpose |
| --- | --- |
| `script` | Scripts |
| `code` | The main program |
| `doc` | Documentation |
| `dataset` | Datasets |
| `task_requirements` | Course PDFs, the README template, and the implementation plan. Do not put the program here. |

If a new main folder is added later, record its purpose in this document before putting files in it.
