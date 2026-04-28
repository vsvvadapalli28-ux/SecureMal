# ==============================================================================
#  securemal.ps1 - SecureMal Deployment & Management Script
#  Usage: .\securemal.ps1 [setup|run|test|build|db|clean]
# ==============================================================================

# Always run from the project root (where this script lives)
$Script:ROOT = $PSScriptRoot
Set-Location $Script:ROOT

$JAR_NAME  = "securemal-1.0-SNAPSHOT-jar-with-dependencies.jar"
$JAR_PATH  = Join-Path $Script:ROOT "target\$JAR_NAME"
$SCHEMA    = Join-Path $Script:ROOT "db\schema.sql"
$MAVEN     = "C:\Program Files\apache-maven-3.9.15\bin\mvn.cmd"
$MYSQL_BIN = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

# Colour helpers
function Write-Green($msg)  { Write-Host $msg -ForegroundColor Green  }
function Write-Red($msg)    { Write-Host $msg -ForegroundColor Red    }
function Write-Yellow($msg) { Write-Host $msg -ForegroundColor Yellow }
function Write-Cyan($msg)   { Write-Host $msg -ForegroundColor Cyan   }
function Write-White($msg)  { Write-Host $msg -ForegroundColor White  }

function Fail($msg) {
    Write-Red "  [ERROR] $msg"
    exit 1
}

function Step($msg) {
    Write-Cyan ""
    Write-Cyan "  >> $msg"
}

# Prerequisite checker
function Check-Prerequisites {
    Step "Checking prerequisites..."

    # Java 17+
    try {
        $jv = (& java -version 2>&1) -join " "
        if ($jv -match '"(\d+)') {
            $major = [int]$Matches[1]
            if ($major -lt 17) { Fail "Java 17+ required. Found version $major" }
            Write-Green "    [OK] Java $major detected"
        } else {
            Fail "Could not determine Java version. Output: $jv"
        }
    } catch {
        Fail "Java not found. Install JDK 17 and add to PATH."
    }

    # Maven
    if (-not (Test-Path $MAVEN)) {
        Fail "Maven not found at: $MAVEN"
    }
    $mv = (& $MAVEN --version 2>&1) | Select-Object -First 1
    Write-Green "    [OK] $mv"

    # Python 3
    try {
        $pv = (& python --version 2>&1) -join ""
        Write-Green "    [OK] $pv"
    } catch {
        Fail "Python 3 not found. Install Python 3.11+ and add to PATH."
    }

    # MySQL
    if (-not (Test-Path $MYSQL_BIN)) {
        Fail "MySQL not found at: $MYSQL_BIN"
    }
    Write-Green "    [OK] MySQL Server found"
}

# SETUP
function Invoke-Setup {
    Write-White ""
    Write-White "  ============================================"
    Write-White "   SecureMal - First-time Setup"
    Write-White "  ============================================"

    Check-Prerequisites

    # 1. Python packages
    Step "Installing Python packages..."
    & python -m pip install --quiet python-magic pefile bcrypt python-evtx python-magic-bin
    if ($LASTEXITCODE -ne 0) { Fail "pip install failed." }
    Write-Green "    [OK] Python packages installed"

    # 2. Create directories
    Step "Creating uploads/, reports/, and test_samples/ directories..."
    foreach ($d in @("uploads", "reports", "test_samples")) {
        $dir = Join-Path $Script:ROOT $d
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir | Out-Null
            Write-Green "    [OK] Created $d"
        } else {
            Write-Yellow "    [--] $d already exists"
        }
    }

    # 3. Database setup
    Step "Setting up MySQL database..."
    $mysqlPass = Read-Host "    Enter MySQL root password" -AsSecureString
    $plainPass = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($mysqlPass)
    )

    $schemaContent = Get-Content $SCHEMA -Raw
    if ($plainPass -ne "") {
        $result = ($schemaContent | & $MYSQL_BIN -u root "-p$plainPass" 2>&1)
    } else {
        $result = ($schemaContent | & $MYSQL_BIN -u root 2>&1)
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Yellow "    [WARN] MySQL returned non-zero. Check credentials if tables were not created."
        Write-Yellow "    Output: $result"
    } else {
        Write-Green "    [OK] Database schema applied"
    }

    # 4. Maven build
    Step "Building JAR (mvn clean package)..."
    & $MAVEN clean package -q
    if ($LASTEXITCODE -ne 0) { Fail "Maven build failed." }
    $sizeMB = [math]::Round((Get-Item $JAR_PATH).Length / 1MB, 2)
    Write-Green "    [OK] JAR built: $JAR_NAME"
    Write-Green "    [OK] Size: $sizeMB MB"

    Write-White ""
    Write-Green "  [DONE] SecureMal setup complete."
    Write-Green "         Run: .\securemal.ps1 run"
    Write-White ""
}

# BUILD
function Invoke-Build {
    Write-White ""
    Write-White "  ============================================"
    Write-White "   SecureMal - Build"
    Write-White "  ============================================"
    Step "Running mvn clean package..."
    & $MAVEN clean package -q
    if ($LASTEXITCODE -ne 0) { Fail "Maven build failed." }
    $sizeMB = [math]::Round((Get-Item $JAR_PATH).Length / 1MB, 2)
    Write-Green "    [OK] JAR: $JAR_PATH"
    Write-Green "    [OK] Size: $sizeMB MB"
}

# RUN
function Invoke-Run {
    Write-White ""
    Write-White "  ============================================"
    Write-White "   SecureMal - Launch"
    Write-White "  ============================================"

    if (-not (Test-Path $JAR_PATH)) {
        Write-Yellow "    JAR not found. Running build first..."
        Invoke-Build
    }

    Write-Green "  Launching SecureMal..."
    Start-Process java -ArgumentList "-jar", $JAR_PATH -WorkingDirectory $Script:ROOT
    Write-Green "  App window opening. Done."
}

# TEST
function Invoke-Test {
    Write-White ""
    Write-White "  ============================================"
    Write-White "   SecureMal - Tests"
    Write-White "  ============================================"

    $javaFailed   = $false
    $pythonFailed = $false

    # Java tests
    Step "Running Java tests (mvn test)..."
    $mvnOut = & $MAVEN test 2>&1
    if ($LASTEXITCODE -ne 0) {
        $javaFailed = $true
        Write-Red "    [FAIL] Java tests failed:"
        $mvnOut | Select-String -Pattern "FAIL|ERROR|Tests run" | ForEach-Object { Write-Red "      $_" }
    } else {
        $summary = $mvnOut | Select-String "Tests run:" | Select-Object -Last 1
        Write-Green "    [OK] Java: $summary"
    }

    # Python tests
    Step "Running Python tests (pytest)..."
    $pytestDir = Join-Path $Script:ROOT "analysis_engine"
    if (Test-Path $pytestDir) {
        $pytestOut = & python -m pytest $pytestDir -v 2>&1
        if ($LASTEXITCODE -ne 0) {
            $pythonFailed = $true
            Write-Red "    [FAIL] Python tests failed:"
            $pytestOut | Select-String "FAILED|ERROR" | ForEach-Object { Write-Red "      $_" }
        } else {
            $pySummary = $pytestOut | Select-String "passed" | Select-Object -Last 1
            Write-Green "    [OK] Python: $pySummary"
        }
    } else {
        Write-Yellow "    [SKIP] No Python test directory found at analysis_engine/"
    }

    Write-White ""
    if ($javaFailed -or $pythonFailed) {
        Write-Red "  RESULT: Some tests FAILED."
    } else {
        Write-Green "  RESULT: All tests PASSED."
    }
}

# DB RESET
function Invoke-DB {
    Write-White ""
    Write-White "  ============================================"
    Write-White "   SecureMal - Database Reset"
    Write-White "  ============================================"
    Write-Yellow "    WARNING: This will drop and recreate all tables."
    Write-Yellow "    All existing data will be lost."
    $confirm = Read-Host "    Continue? (y/n)"
    if ($confirm -ne "y") {
        Write-White "    Aborted."
        return
    }

    $mysqlPass = Read-Host "    Enter MySQL root password" -AsSecureString
    $plainPass = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($mysqlPass)
    )

    Step "Running schema.sql..."
    $schemaContent = Get-Content $SCHEMA -Raw
    if ($plainPass -ne "") {
        $result = ($schemaContent | & $MYSQL_BIN -u root "-p$plainPass" 2>&1)
    } else {
        $result = ($schemaContent | & $MYSQL_BIN -u root 2>&1)
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Red "    [ERROR] Database reset failed:"
        $result | ForEach-Object { Write-Red "      $_" }
    } else {
        Write-Green "    [OK] Database reset complete."
    }
}

# CLEAN
function Invoke-Clean {
    Write-White ""
    Write-White "  ============================================"
    Write-White "   SecureMal - Clean"
    Write-White "  ============================================"

    # Delete target/
    $targetDir = Join-Path $Script:ROOT "target"
    if (Test-Path $targetDir) {
        Step "Deleting target/ folder..."
        Remove-Item $targetDir -Recurse -Force
        Write-Green "    [OK] target/ deleted"
    } else {
        Write-Yellow "    [--] target/ does not exist"
    }

    # Clear uploads/ contents (keep folder)
    $uploadsDir = Join-Path $Script:ROOT "uploads"
    if (Test-Path $uploadsDir) {
        Step "Clearing uploads/ contents..."
        $files = Get-ChildItem $uploadsDir -File
        $count = $files.Count
        if ($count -gt 0) { $files | Remove-Item -Force }
        Write-Green "    [OK] Removed $count file(s) from uploads/"
    }

    # Clear reports/ contents (keep folder)
    $reportsDir = Join-Path $Script:ROOT "reports"
    if (Test-Path $reportsDir) {
        Step "Clearing reports/ contents..."
        $files = Get-ChildItem $reportsDir -File
        $count = $files.Count
        if ($count -gt 0) { $files | Remove-Item -Force }
        Write-Green "    [OK] Removed $count file(s) from reports/"
    }

    Write-White ""
    Write-Green "  Clean complete."
}

# INTERACTIVE MENU
function Show-Menu {
    while ($true) {
        Write-White ""
        Write-White "  +-----------------------------------+"
        Write-White "  |      SecureMal Launcher           |"
        Write-White "  +-----------------------------------+"
        Write-White "  |  [1] First-time setup             |"
        Write-White "  |  [2] Launch app                   |"
        Write-White "  |  [3] Run tests                    |"
        Write-White "  |  [4] Build JAR                    |"
        Write-White "  |  [5] Reset database               |"
        Write-White "  |  [6] Clean project                |"
        Write-White "  |  [Q] Quit                         |"
        Write-White "  +-----------------------------------+"
        $choice = Read-Host "  Enter choice"

        switch ($choice.ToUpper()) {
            "1" { Invoke-Setup }
            "2" { Invoke-Run   }
            "3" { Invoke-Test  }
            "4" { Invoke-Build }
            "5" { Invoke-DB    }
            "6" { Invoke-Clean }
            "Q" { Write-White "  Goodbye."; exit 0 }
            default { Write-Yellow "  Unknown option. Please enter 1-6 or Q." }
        }
    }
}

# ENTRY POINT
$cmd = if ($args.Count -gt 0) { $args[0].ToLower() } else { "" }

switch ($cmd) {
    "setup" { Invoke-Setup }
    "run"   { Invoke-Run   }
    "test"  { Invoke-Test  }
    "build" { Invoke-Build }
    "db"    { Invoke-DB    }
    "clean" { Invoke-Clean }
    default { Show-Menu    }
}
