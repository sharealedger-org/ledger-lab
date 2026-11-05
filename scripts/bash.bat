@echo off
rem scripts\bash.bat — invoke Git Bash (not WSL) from PowerShell or cmd.
rem
rem USAGE (from repo root):
rem   scripts\bash.bat scripts/run_pipeline.sh [args...]
rem   scripts\bash.bat scripts/run_repo_fixture_smoke_test.sh
rem
rem ONE-TIME PROFILE SETUP — add to PowerShell $PROFILE so `bash` resolves here:
rem
rem   $env:PATH = "C:\Users\0D8297897\Workspace\Sharealedger\ledger-lab\scripts;" + $env:PATH
rem
rem After that one setup, every `bash scripts/....sh ...` call in this repo
rem goes to Git Bash, never WSL.

set "GITBASH=C:\Program Files\Git\bin\bash.exe"

if not exist "%GITBASH%" (
    echo ERROR: Git Bash not found at %GITBASH%. Install Git for Windows.
    exit /b 1
)

rem Pass the script and all arguments directly — no -c, so each arg is a
rem separate token and paths with spaces are handled correctly.
"%GITBASH%" -l %*
