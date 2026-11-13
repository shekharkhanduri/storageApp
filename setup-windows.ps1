#Requires -Version 5.1
<#
.SYNOPSIS
    StoreIt — Windows Setup Script
    Installs all prerequisites, clones the repo (if needed), configures
    environment files, starts infrastructure (Docker Compose), installs
    Node dependencies, and runs all tests (frontend + backend unit).

.USAGE
    1. Open PowerShell as Administrator
    2. Set-ExecutionPolicy RemoteSigned -Scope CurrentUser   (once)
    3. .\setup-windows.ps1

    Optional flags:
      -SkipPrereqs      Skip Winget/Chocolatey prerequisite installation
      -SkipClone        Skip git clone (use if already in repo folder)
      -SkipInfra        Skip Docker Compose (postgres + minio)
      -RunTests         Run frontend + backend tests after setup
      -RepoUrl          GitHub URL to clone from (default: prompts if missing)
#>

param(
    [switch]$SkipPrereqs,
    [switch]$SkipClone,
    [switch]$SkipInfra,
    [switch]$RunTests,
    [string]$RepoUrl = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ─────────────────────────── helpers ────────────────────────────────────────

function Write-Header([string]$msg) {
    Write-Host ""
    Write-Host "══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "══════════════════════════════════════════════════════" -ForegroundColor Cyan
}

function Write-Step([string]$msg)  { Write-Host "  ► $msg" -ForegroundColor Yellow }
function Write-OK([string]$msg)    { Write-Host "  ✔ $msg" -ForegroundColor Green  }
function Write-Warn([string]$msg)  { Write-Host "  ⚠ $msg" -ForegroundColor DarkYellow }
function Write-Err([string]$msg)   { Write-Host "  ✘ $msg" -ForegroundColor Red; exit 1 }

function Refresh-Path {
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") +
                ";" +
                [System.Environment]::GetEnvironmentVariable("Path","User")
}

function Command-Exists([string]$cmd) {
    return $null -ne (Get-Command $cmd -ErrorAction SilentlyContinue)
}

function Winget-Install([string]$id, [string]$label) {
    Write-Step "Installing $label via winget..."
    winget install --id $id --silent --accept-package-agreements --accept-source-agreements
    Refresh-Path
}

function Assert-Version([string]$cmd, [string]$minLabel) {
    if (-not (Command-Exists $cmd)) {
        Write-Err "$cmd not found after installation. Add it to PATH and re-run."
    }
    $ver = & $cmd --version 2>&1 | Select-Object -First 1
    Write-OK "$cmd found: $ver"
}

# ─────────────────────────── 0. Admin check ─────────────────────────────────

Write-Header "StoreIt Windows Setup"
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Warn "Not running as Administrator. Some installations may fail."
    Write-Warn "Re-launch PowerShell as Administrator for a smooth setup."
}

# ─────────────────────────── 1. Prerequisites ───────────────────────────────

if (-not $SkipPrereqs) {
    Write-Header "Step 1 — Installing Prerequisites"

    # --- Git ---
    if (-not (Command-Exists "git")) {
        Winget-Install "Git.Git" "Git"
    } else { Write-OK "git already installed: $(git --version)" }

    # --- Node.js 20 LTS (includes npm) ---
    if (-not (Command-Exists "node")) {
        Winget-Install "OpenJS.NodeJS.LTS" "Node.js LTS"
    } else {
        $nodeVer = node --version
        Write-OK "node already installed: $nodeVer"
        # Warn if below v20
        $nodeMajor = [int]($nodeVer -replace 'v(\d+).*','$1')
        if ($nodeMajor -lt 20) {
            Write-Warn "Node $nodeVer is below v20. StoreIt needs v20+."
            Write-Warn "Run: winget install OpenJS.NodeJS.LTS"
        }
    }

    # --- Java 21 (Temurin / Eclipse Adoptium) ---
    if (-not (Command-Exists "java")) {
        Winget-Install "EclipseAdoptium.Temurin.21.JDK" "Java 21 (Temurin)"
        # Set JAVA_HOME for current session
        $javaHome = "C:\Program Files\Eclipse Adoptium\jdk-21*"
        $resolved  = Resolve-Path $javaHome -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved) {
            $env:JAVA_HOME = $resolved.Path
            [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $resolved.Path, "User")
            Write-OK "JAVA_HOME set to $($resolved.Path)"
        }
    } else {
        Write-OK "java already installed: $(java --version 2>&1 | Select-Object -First 1)"
    }
    if (-not $env:JAVA_HOME) {
        Write-Warn "JAVA_HOME is not set. Maven wrapper (mvnw.cmd) may fail."
        Write-Warn "Set it manually: [System.Environment]::SetEnvironmentVariable('JAVA_HOME','C:\path\to\jdk-21','User')"
    }

    # --- Docker Desktop (for Docker Compose infra) ---
    if (-not (Command-Exists "docker")) {
        Write-Step "Installing Docker Desktop via winget..."
        winget install --id Docker.DockerDesktop --silent --accept-package-agreements --accept-source-agreements
        Refresh-Path
        Write-Warn "Docker Desktop installed. RESTART your machine, then re-run this script with -SkipPrereqs."
        Write-Warn "Docker requires WSL2 — Windows will configure it automatically on first launch."
        exit 0
    } else {
        Write-OK "docker already installed: $(docker --version)"
    }

} else {
    Write-Step "Skipping prerequisite installation (-SkipPrereqs)"
}

# Validate the core tools are on PATH before continuing
foreach ($tool in @("git","node","npm","java","docker")) {
    if (-not (Command-Exists $tool)) {
        Write-Err "'$tool' not found on PATH. Install it and re-run."
    }
}

# ─────────────────────────── 2. Clone repository ────────────────────────────

Write-Header "Step 2 — Repository"

if (-not $SkipClone) {
    if ($RepoUrl -eq "") {
        $RepoUrl = Read-Host "Enter the GitHub repository URL (e.g. https://github.com/user/storage_management_solution)"
    }
    $repoName = ($RepoUrl -split "/")[-1] -replace "\.git$",""
    $cloneTarget = Join-Path (Get-Location) $repoName

    if (Test-Path $cloneTarget) {
        Write-Warn "Directory '$cloneTarget' already exists — skipping clone."
        Write-Warn "Delete it or run with -SkipClone if already inside the repo."
    } else {
        Write-Step "Cloning $RepoUrl ..."
        git clone $RepoUrl $cloneTarget
        Write-OK "Cloned to $cloneTarget"
    }
    Set-Location $cloneTarget
} else {
    Write-Step "Skipping clone (-SkipClone). Using current directory: $(Get-Location)"
}

# Confirm we are in the repo root (package.json + backend/ must exist)
if (-not (Test-Path "package.json")) { Write-Err "package.json not found. Are you inside the repo root?" }
if (-not (Test-Path "backend"))       { Write-Err "backend/ folder not found. Are you inside the repo root?" }
Write-OK "Repo root confirmed: $(Get-Location)"

# ─────────────────────────── 3. Environment files ───────────────────────────

Write-Header "Step 3 — Environment Configuration"

# --- Frontend .env.local ---
if (-not (Test-Path ".env.local")) {
    Write-Step "Creating .env.local from .env.example ..."
    Copy-Item ".env.example" ".env.local"
    Write-Warn ".env.local created. Fill in your Appwrite credentials before running the app:"
    Write-Warn "  NEXT_PUBLIC_APPWRITE_PROJECT, DATABASE, USERS_COLLECTION, FILES_COLLECTION, BUCKET, KEY"
    Write-Warn "  File: $(Resolve-Path '.env.local')"
} else {
    Write-OK ".env.local already exists — not overwritten."
}

# Check NEXT_PUBLIC_API_URL is present
$envContent = Get-Content ".env.local" -Raw
if ($envContent -notmatch "NEXT_PUBLIC_API_URL") {
    Add-Content ".env.local" "`r`nNEXT_PUBLIC_API_URL=http://localhost:8080"
    Write-OK "Added NEXT_PUBLIC_API_URL=http://localhost:8080 to .env.local"
}

# --- Backend application.yml reminder ---
Write-Warn "Backend config is in backend/src/main/resources/application.yml"
Write-Warn "Default settings (localhost postgres/minio) work as-is after Step 4."

# ─────────────────────────── 4. Infrastructure (Docker Compose) ─────────────

if (-not $SkipInfra) {
    Write-Header "Step 4 — Starting Infrastructure (PostgreSQL + MinIO)"

    # Ensure Docker daemon is running
    $dockerRunning = docker info 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Docker daemon is not running. Start Docker Desktop and re-run."
    }

    Push-Location "backend"
    Write-Step "Pulling images and starting containers..."
    docker compose up -d --pull always
    if ($LASTEXITCODE -ne 0) { Write-Err "docker compose up failed." }

    Write-Step "Waiting for PostgreSQL to be healthy..."
    $maxWait = 60; $waited = 0
    do {
        Start-Sleep 3; $waited += 3
        $health = docker inspect --format "{{.State.Health.Status}}" storeit-postgres 2>&1
    } while ($health -ne "healthy" -and $waited -lt $maxWait)

    if ($health -ne "healthy") {
        Write-Warn "PostgreSQL did not become healthy within ${maxWait}s. Check: docker logs storeit-postgres"
    } else {
        Write-OK "PostgreSQL is healthy."
    }

    Write-Step "Waiting for MinIO to be healthy..."
    $waited = 0
    do {
        Start-Sleep 3; $waited += 3
        $health = docker inspect --format "{{.State.Health.Status}}" storeit-minio 2>&1
    } while ($health -ne "healthy" -and $waited -lt $maxWait)

    if ($health -ne "healthy") {
        Write-Warn "MinIO did not become healthy within ${maxWait}s. Check: docker logs storeit-minio"
    } else {
        Write-OK "MinIO is healthy."
    }

    # Create MinIO bucket (storeit-files) using mc if available, else skip
    if (Command-Exists "mc") {
        Write-Step "Creating MinIO bucket 'storeit-files'..."
        mc alias set local http://localhost:9000 minioadmin minioadmin --quiet
        mc mb --ignore-existing local/storeit-files
        Write-OK "Bucket 'storeit-files' ready."
    } else {
        Write-Warn "MinIO Client (mc) not on PATH — bucket creation skipped."
        Write-Warn "Open http://localhost:9001 (user: minioadmin / pass: minioadmin) and create bucket 'storeit-files' manually."
    }

    Pop-Location
} else {
    Write-Step "Skipping infrastructure start (-SkipInfra)"
}

# ─────────────────────────── 5. Frontend — npm install ──────────────────────

Write-Header "Step 5 — Frontend Dependencies (npm install)"

Write-Step "Running npm install..."
npm install --prefer-online --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { Write-Err "npm install failed." }
Write-OK "Frontend dependencies installed."

# ─────────────────────────── 6. Backend — Maven build ───────────────────────

Write-Header "Step 6 — Backend Build (Maven)"

Push-Location "backend"

# Use the bundled Maven wrapper
$mvnCmd = if (Test-Path "mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }
Write-Step "Building backend (skipping tests during build)..."
& $mvnCmd clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Err "Maven build failed. Run '.\mvnw.cmd clean package -DskipTests' in backend/ for details." }
Write-OK "Backend JAR built successfully."

Pop-Location

# ─────────────────────────── 7. Tests (optional) ────────────────────────────

if ($RunTests) {
    Write-Header "Step 7 — Running Tests"

    # --- Frontend tests (Vitest) ---
    Write-Step "Running frontend tests (Vitest)..."
    npm test
    if ($LASTEXITCODE -ne 0) {
        Write-Warn "Some frontend tests failed. Review output above."
    } else {
        Write-OK "All frontend tests passed."
    }

    # --- Backend unit tests (Maven Surefire, excludes @Tag('integration')) ---
    Push-Location "backend"
    Write-Step "Running backend unit tests (Maven Surefire, excluding integration)..."
    & $mvnCmd test -DexcludedGroups=integration
    if ($LASTEXITCODE -ne 0) {
        Write-Warn "Some backend unit tests failed. Review output above."
    } else {
        Write-OK "All backend unit tests passed."
    }
    Pop-Location

} else {
    Write-Step "Skipping tests. Re-run with -RunTests to execute them."
    Write-Step "  Frontend : npm test"
    Write-Step "  Backend  : cd backend && .\mvnw.cmd test -DexcludedGroups=integration"
}

# ─────────────────────────── 8. Summary ─────────────────────────────────────

Write-Header "Setup Complete!"
Write-Host ""
Write-Host "  Next steps:" -ForegroundColor White
Write-Host ""
Write-Host "  1. Fill in .env.local with your Appwrite credentials" -ForegroundColor Gray
Write-Host "     (copy the IDs from your Appwrite console)" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Start the Java backend (new terminal):" -ForegroundColor Gray
Write-Host "     cd backend" -ForegroundColor Green
Write-Host "     .\mvnw.cmd spring-boot:run" -ForegroundColor Green
Write-Host "     → Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  3. Start the Next.js frontend (another terminal):" -ForegroundColor Gray
Write-Host "     npm run dev" -ForegroundColor Green
Write-Host "     → App: http://localhost:3000" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  4. Services running:" -ForegroundColor Gray
Write-Host "     PostgreSQL  → localhost:5432" -ForegroundColor DarkGray
Write-Host "     MinIO S3    → localhost:9000" -ForegroundColor DarkGray
Write-Host "     MinIO UI    → http://localhost:9001  (minioadmin / minioadmin)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  Stop infrastructure:  cd backend && docker compose down" -ForegroundColor DarkYellow
Write-Host ""
