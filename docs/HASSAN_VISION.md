# Hassan AI — Vision

Hassan is a **Personal AI Cloud Operating System**.

- **Phone** = Command Center (chat, approvals, projects, files, results)
- **Cloud** = Computer (durable work, builds, agents)
- **Agents** = Workforce (planner, coder, reviewer, tester, …)
- **Radar** = Discovery Engine (free models, agents, tools)
- **Memory** = Long-term intelligence
- **Tools** = Execution layer

## End state

User provides a **goal** from the phone. Hassan decides how to execute it (models, agents, tools, research, coding, build, verify) and returns artifacts — even if the phone is closed.

## Principles

1. **Standalone Android** — no PC, no Desktop Hassan, no pairing
2. **Chat-first** — default intent is CHAT
3. **Hassan Auto** — orchestrator chooses providers
4. **Free-first** — prefer free/open-source/free-tier before paid APIs
5. **Honesty** — no fake AI, no fake tests, clear status labels
6. **Verification** — generated ≠ working; build, test, verify
7. **Candidate / Stable** — experiments in Candidate only

## Architecture target

```
Hassan Android → Hassan Cloud → Chat Router / Orchestrator / Projects
                                      ↓
                              Agents → Workspaces → Artifacts
                                      ↓
                                   Memory + Capability Registry
```
