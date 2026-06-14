# Development Logs

This directory contains development session logs for the AutoCat project. Each log captures the conversation between the developer and Claude Code AI assistant, providing full context for code changes beyond commit messages.

## Purpose

- **Context Preservation**: Full conversation history explains *why* decisions were made
- **Onboarding**: New contributors can understand the development journey
- **Debugging**: Reference past discussions when issues arise
- **Documentation**: Natural language explanation of technical implementation

## File Naming Convention

```
YYYY-MM-DD-session-N.md
```

- `YYYY-MM-DD`: Date of development session
- `session-N`: Session number for that day (if multiple sessions)

**Example**: `2025-11-23-session-1.md`

## Log File Structure

Each log file contains:

1. **Header**: Session metadata (date, commits, files changed)
2. **Summary**: High-level overview of what was accomplished
3. **Commits**: List of commits made during the session
4. **Conversation Transcript**: Full user/assistant dialogue
5. **Next Steps**: Planned future work

## Usage

### When to Create a Log

Create a new log file for each development session where you:
- Make commits to the repository
- Implement new features or fix bugs
- Have significant design discussions
- Make architectural decisions

### How to Create a Log

Logs can be created manually or by asking Claude Code:
```
"Save our conversation to the dev-logs with a summary of what we accomplished"
```

Claude will:
1. Generate a summary of the session
2. List all commits made
3. Include the full conversation transcript
4. Save to `dev-logs/YYYY-MM-DD-session-N.md`

### Viewing Logs

Logs are plain markdown files viewable on GitHub or any text editor. They are:
- ✅ Committed to version control
- ✅ Viewable in GitHub UI
- ❌ NOT included in APK builds (dev-only files)

## Best Practices

1. **End-of-Session**: Create log before ending a development session
2. **Major Milestones**: Create log after completing a significant feature
3. **Context Switching**: Create log before switching to a different feature area
4. **Meaningful Summaries**: Write clear, searchable summaries

## Example Summary Format

```markdown
## Summary

**Session Goal**: Implement Room database foundation for app categorization

**Accomplishments**:
- Created AppCategory and CustomCategory entities
- Implemented CategoryDao with 30+ database operations
- Set up CategoryDatabase singleton with Room
- Added unit tests for entity classes
- Successfully built on GitHub Actions

**Key Decisions**:
- Chose Room over raw SQLite for type safety
- Used Flow for reactive database queries
- Implemented confidence scoring system (0.0-1.0)

**Files Changed**: 6 files, 636 lines added

**Next Session**: Implement rule-based categorizer or AppMetadataProvider
```

## Maintenance

- Logs are kept indefinitely (small text files)
- No automated cleanup (preserve full history)
- Can be referenced via grep/search for specific topics
- Consider tagging logs with feature names in summaries

## Privacy Note

These logs contain development conversations only. Do not include:
- API keys or credentials
- Personal information
- Proprietary code from other projects
- Security vulnerabilities (use private security advisory instead)
