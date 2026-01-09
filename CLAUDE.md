# Out Of Sight, Out Of Mind

Minecraft Fabric mod using the Polymer library.

## Workflow

For complex tasks, use the structured Claude Code workflow defined in `AGENTS.md`.

### Slash Commands

| Command | Description |
|---------|-------------|
| `/workflow [task]` | Full 7-phase workflow (problem -> plan -> implement -> review -> verify) |
| `/plan [task]` | Create implementation plan using Planner subagent (opus) |
| `/review-plan [plan.md]` | Review plan using Plan-Reviewer subagent (opus) |
| `/gather-context [topic]` | Find 10-20 relevant files for a topic |
| `/simplify [file\|staged]` | Simplify code using Code-Simplifier subagent |
| `/review-code [file\|staged]` | Review code for bugs/vulnerabilities (opus) |
| `/verify [problem]` | Verify implementation solves the problem |

### Workflow Phases
1. Problem clarification (requires approval)
2. Context gathering
3. Planning with subagent review (requires approval)
4. Implementation
5. Code quality review
6. Verification and testing

### Subagent Prompts
Detailed prompts for each subagent are in `AGENTS.md` under "Subagent Specifications" section

---

## Project Info

- **Minecraft:** 1.21.10
- **Java:** 21
- **Build:** Gradle with Fabric Loom
- **Dependencies:** Fabric API, Polymer

---

## Commands

```bash
./gradlew build              # Build the mod
./gradlew runClient          # Run Minecraft client with mod
./gradlew runServer          # Run Minecraft server with mod
./gradlew spotlessApply      # Format code (Google Java Format)
./gradlew spotlessCheck      # Check formatting
```

---

## Code Style

- Java 21
- Google Java Format (via Spotless)
- Follow existing patterns in codebase

---

## Important Notes

- NEVER make assumptions about the codebase - use search tools
- Follow existing patterns and conventions
- **Keep this file updated**: When discovering new commands, solutions to problems, or operational knowledge not documented here, add them to this file immediately so the knowledge persists across sessions
- **Keep files consistent**: If requirements change after files were created (e.g., plan.md), those files MUST be updated immediately to reflect the new requirements
- **No context.md files**: Do NOT create context.md files unless the user explicitly asks for one
