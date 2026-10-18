# ==============================================================================
# check_ui_tokens.ps1 - UI Token and Encoding Linter for Euthopiar
# Enforces design token usage, accessibility, and UTF-8 encoding across the app.
# ==============================================================================

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$RepoRoot = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

$allowlistPath = Join-Path $ScriptDir "ui_tokens_allowlist.json"
$allowlist = @()
if (Test-Path $allowlistPath) {
    $rawJson = Get-Content -Raw -Path $allowlistPath | ConvertFrom-Json
    $allowlist = $rawJson.allowlist
}

$violations = @()

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Running Euthopiar UI Tokens & Encoding Verification" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# 1. Encoding Check: scan ALL .kt files for U+FFFD or control chars (0x00-0x08)
# ------------------------------------------------------------------------------
Write-Host "[1/2] Checking UTF-8 file encoding across all Kotlin files..." -ForegroundColor Yellow

$allKtFiles = Get-ChildItem -Path @("app", "core") -Recurse -Filter "*.kt" -File

foreach ($file in $allKtFiles) {
    $relPath = $file.FullName.Substring($RepoRoot.Length).TrimStart('\', '/').Replace('\', '/')
    $content = [System.IO.File]::ReadAllText($file.FullName)
    
    if ($content.IndexOf([char]0xFFFD) -ge 0) {
        $violations += [PSCustomObject]@{
            File = $relPath
            Line = 0
            Rule = "Encoding"
            Detail = "Found replacement character U+FFFD (corrupted UTF-8 character)"
        }
    }
    
    for ($c = 0; $c -le 8; $c++) {
        if ($content.IndexOf([char]$c) -ge 0) {
            $violations += [PSCustomObject]@{
                File = $relPath
                Line = 0
                Rule = "Encoding"
                Detail = ("Found invalid control character (0x{0:X2})" -f $c)
            }
            break
        }
    }
}

# ------------------------------------------------------------------------------
# 2. Token & Accessibility Checks (outside ui/theme/)
# ------------------------------------------------------------------------------
Write-Host "[2/2] Checking UI tokens and accessibility compliance..." -ForegroundColor Yellow

$uiDir = Join-Path $RepoRoot "app/src/main/java/com/euthopiar/ui"
$uiFiles = Get-ChildItem -Path $uiDir -Recurse -Filter "*.kt" -File

$patterns = @(
    @{ Name = "Color(0x)"; Pattern = "Color\(0x" },
    @{ Name = "Color.(White|Black|Gray|Red)"; Pattern = "(?-i)\bColor\.(White|Black|Gray|Red)\b" },
    @{ Name = "RoundedCornerShape("; Pattern = "RoundedCornerShape\(" },
    @{ Name = "fontSize ="; Pattern = "fontSize\s*=" },
    @{ Name = "Raw sp dimension"; Pattern = "\b\d+(\.\d+)?\.sp\b" },
    @{ Name = "MaterialTheme.typography"; Pattern = "MaterialTheme\.typography\b" },
    @{ Name = "Toast.makeText"; Pattern = "Toast\.makeText\b" },
    @{ Name = "bouncyClickable"; Pattern = "(?-i)\bbouncyClickable\b" },
    @{ Name = "Deprecated Color Alias"; Pattern = "(?-i)\b(VelvetBlack|ObsidianDark|GlassSurface|GlassSurfacePill|PureWhite|PureBlack|TextMuted|StarGold)\b" }
)

function IsAllowlisted($relPath, $lineContent) {
    foreach ($entry in $allowlist) {
        $entryFile = $entry.file.Replace("\", "/")
        if ($entryFile -eq $relPath) {
            if ($lineContent -match [regex]::Escape($entry.pattern)) {
                return $true
            }
            # Also allow regex matching if needed
            if ($lineContent -match $entry.pattern) {
                return $true
            }
        }
    }
    return $false
}

foreach ($file in $uiFiles) {
    $relPath = $file.FullName.Substring($RepoRoot.Length).TrimStart('\', '/').Replace('\', '/')
    
    # Skip ui/theme/ (design system token definition sources)
    if ($relPath -like "*/ui/theme/*") {
        continue
    }

    $lines = [System.IO.File]::ReadAllLines($file.FullName)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $lineNum = $i + 1
        $lineText = $lines[$i]

        foreach ($p in $patterns) {
            if ($lineText -match $p.Pattern) {
                if (-not (IsAllowlisted $relPath $lineText)) {
                    $violations += [PSCustomObject]@{
                        File = $relPath
                        Line = $lineNum
                        Rule = $p.Name
                        Detail = $lineText.Trim()
                    }
                }
            }
        }
    }
}

# ------------------------------------------------------------------------------
# 3. Report Results
# ------------------------------------------------------------------------------
Write-Host ""
if ($violations.Count -eq 0) {
    Write-Host "SUCCESS: All UI tokens, encoding, and accessibility checks passed!" -ForegroundColor Green
    Write-Host "Total Kotlin files checked: $($allKtFiles.Count)" -ForegroundColor Green
    exit 0
} else {
    Write-Host "FAILED: Found $($violations.Count) compliance violation(s):" -ForegroundColor Red
    Write-Host ""
    foreach ($v in $violations) {
        Write-Host "  [$($v.Rule)] $($v.File):$($v.Line)" -ForegroundColor Red
        Write-Host "    $($v.Detail)" -ForegroundColor DarkGray
    }
    Write-Host ""
    Write-Host "Please replace violations with design system tokens or add justified exceptions to scripts/ui_tokens_allowlist.json." -ForegroundColor Yellow
    exit 1
}
