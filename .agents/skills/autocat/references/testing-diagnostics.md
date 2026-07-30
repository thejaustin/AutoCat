# AutoCat Testing & Diagnostics

This guide covers local verification, inspecting LLM logs, testing database integrity, and utilizing diagnostic tools.

## 🔌 Verifying LLM Providers

Ensure that all API integrations are active and verified.

- **Check API Key Setup**: Providers load API keys from `PreferenceManager2`. Ensure you have configured the appropriate keys in Settings or the config file.
- **Connection Test**: Use the `testConnection()` method in `LLMProvider` implementations to verify network connectivity and key validity for Google AI, Claude, OpenAI, and Perplexity.

## 📜 Inspecting LLM Logs

AutoCat records all requests, responses, and errors in an in-memory buffer:

- **Logs Location**: Access logs via `LLMLogger.kt`.
- **Exporting Logs**: You can export the logs for analysis by calling `LLMLogger.exportLogs(context)`. This creates a text file of the structured logs.
- **Troubleshooting HTTP 429**: Exponential backoff retry logic is implemented in `LLMUtils.withRetry`. Check logs to ensure retry loops are triggered during rate limiting.

## 🗄️ Inspecting the Room Database

A utility script is provided in `AutoCat/tools/print_db.py` to view, inspect, and debug the local Room databases on a connected device or environment:

```bash
# Print contents of categories and folders tables
python3 tools/print_db.py
```

## 📱 Developer Diagnostics UI

AutoCat contains a **Developer Mode** with a dedicated diagnostics UI in the Launcher Settings:
- Go to **Launcher Settings** -> **Smart Discovery** -> **Developer Diagnostics**.
- The diagnostics screen displays API connection status, LLM response statistics, Room DB sizes, and allows manually running the categorization pipeline or exporting logs.
