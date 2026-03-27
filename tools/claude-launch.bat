@echo off
REM Claude Code Launcher for AutoCat
REM This batch file starts Claude Code in the AutoCat project directory

echo Starting Claude Code for AutoCat...

REM Navigate to project root
cd /d "%~dp0\.."

REM Check if claude command exists
claude --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Claude Code not found. Please install with:
    echo npm install -g @anthropic-ai/claude-code
    pause
    exit /b 1
)

REM Set environment variables for better integration
set CLAUDE_PROJECT=AutoCat
set CLAUDE_WORKSPACE=%CD%

REM Launch Claude Code
echo Launching Claude Code in %CD%...
claude

pause