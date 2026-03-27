# Development Acceleration

## Google Developer Knowledge MCP

This environment now includes a local Codex plugin for Google's Developer Knowledge MCP endpoint.

- Plugin root: `C:\Users\theja\plugins\google-developer-knowledge`
- Codex config entry: `C:\Users\theja\.codex\config.toml`
- Local plugin marketplace: `C:\Users\theja\.agents\plugins\marketplace.json`

To finish activation:

1. Create and restrict a Developer Knowledge API key in Google Cloud.
2. Set `GOOGLE_DEV_KNOWLEDGE_API_KEY` as a user environment variable.
3. Restart Codex.

The plugin uses `mcp-remote` so no full Google Cloud SDK install is required just to expose the MCP in Codex.

## Faster Android iteration

Use the repo helper below instead of manually exporting environment variables each session:

```powershell
.\\tools\\dev-autocat.ps1
```

Useful variants:

```powershell
.\\tools\\dev-autocat.ps1 -NoBuild
.\\tools\\dev-autocat.ps1 -GradleArgs @(':app:compileLawnWithQuickstepGithubDebugKotlin')
.\\tools\\dev-autocat.ps1 -GradleArgs @('assembleLawnWithQuickstepGithubDebug') -UseConfigurationCache
```

## Recommended workflow

- Default to `--no-configuration-cache` until the root build logic is made configuration-cache safe.
- Reuse `.gradle-user-home` inside the repo to avoid polluting global caches and to keep this workspace reproducible.
- Prefer narrow tasks first, such as Kotlin compile tasks or module-level assemble tasks, before full APK builds.
