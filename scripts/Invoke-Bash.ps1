# scripts/Invoke-Bash.ps1
# ---------------------------------------------------------------------------
# Resolves Git Bash (not WSL) and forwards arguments to a .sh script.
#
# USAGE (from repo root, any PowerShell session):
#
#   . scripts/Invoke-Bash.ps1           # dot-source to load the function
#   Invoke-Bash scripts/run_pipeline.sh --inPath data/VARawFiles ...
#   Invoke-Bash scripts/run_repo_fixture_smoke_test.sh
#
# ONE-TIME PROFILE SETUP  (run once; paste into your $PROFILE):
#
#   $LedgerLab = "C:\Users\0D8297897\Workspace\Sharealedger\ledger-lab"
#   . "$LedgerLab\scripts\Invoke-Bash.ps1"
#   Set-Alias bash (Find-GitBash)   # replaces WSL bash for this session
#
# After profile setup, the normal `bash scripts/run_pipeline.sh ...` syntax
# also works because Set-Alias overwrites the WSL resolution for this session.
# ---------------------------------------------------------------------------

function Find-GitBash {
    <#
    .SYNOPSIS
        Returns the path to Git Bash's bash.exe, never WSL's bash.exe.
    #>
    $candidates = @(
        "C:\Program Files\Git\bin\bash.exe",
        "C:\Program Files\Git\usr\bin\bash.exe",
        "${env:ProgramFiles}\Git\bin\bash.exe",
        "${env:LOCALAPPDATA}\Programs\Git\bin\bash.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    # Fall back: walk PATH entries looking for a git-owned bash
    $pathBash = Get-Command bash -ErrorAction SilentlyContinue |
                    Select-Object -ExpandProperty Source
    if ($pathBash -and $pathBash -notmatch 'system32') { return $pathBash }
    throw "Git Bash not found. Install Git for Windows or set the path in Find-GitBash."
}

function Invoke-Bash {
    <#
    .SYNOPSIS
        Run a Bash script using Git Bash, not WSL.
    .EXAMPLE
        Invoke-Bash scripts/run_pipeline.sh --inPath data/VARawFiles --outPath data/output_validate_fy03 --years 03 --steps ttl,2p,3,5v --curve CCP3 --config validate-1yr
    #>
    param(
        [Parameter(Mandatory, Position=0)]
        [string]$Script,

        [Parameter(ValueFromRemainingArguments)]
        [string[]]$PassThru
    )

    $gitBash = Find-GitBash

    # Convert the script path to a POSIX path that Git Bash understands.
    # cygpath is available inside Git Bash but we need the conversion before launching.
    # Simple approach: replace backslashes and drive letter.
    $posixScript = $Script -replace '\\', '/'
    if ($posixScript -match '^([A-Za-z]):(.+)') {
        $posixScript = '/' + $Matches[1].ToLower() + $Matches[2]
    }

    & $gitBash -l -c "$posixScript $($PassThru -join ' ')"
}
