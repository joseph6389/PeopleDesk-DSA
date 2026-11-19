$ErrorActionPreference = 'Stop'
$port = if ($env:EMPLOYEE_PORT) { $env:EMPLOYEE_PORT } else { '8080' }
$portNumber = 0
if (-not [int]::TryParse($port, [ref]$portNumber) -or $portNumber -lt 1 -or $portNumber -gt 65535) {
    throw 'EMPLOYEE_PORT must be between 1 and 65535.'
}

# Identify an occupied listener before starting Java. Never stop a process here.
$listenerIds = @()
$listenerInspectionSucceeded = $false
try {
    $listenerIds = @(Get-NetTCPConnection -State Listen -LocalPort $portNumber -ErrorAction Stop |
        Select-Object -ExpandProperty OwningProcess -Unique)
    $listenerInspectionSucceeded = $true
} catch {
    $netstat = Join-Path $env:SystemRoot 'System32\netstat.exe'
    if (Test-Path $netstat) {
        $pattern = "^\s*TCP\s+\S+:$portNumber\s+\S+\s+LISTENING\s+(\d+)\s*$"
        $netstatOutput = @(& $netstat -ano -p tcp 2>$null)
        if ($LASTEXITCODE -eq 0) { $listenerInspectionSucceeded = $true }
        $listenerIds = @($netstatOutput | ForEach-Object {
            if ($_ -match $pattern) { [int]$Matches[1] }
        } | Sort-Object -Unique)
    }
}
if (-not $listenerInspectionSucceeded) {
    throw "Could not safely inspect port $portNumber. No server was started and no process was stopped. Inspect the Windows listener manually before retrying."
}
if ($listenerIds.Count) {
    foreach ($listenerId in $listenerIds) {
        $processInfo = $null
        try { $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $listenerId" -ErrorAction Stop } catch { }
        if ($processInfo) {
            $commandLine = [string]$processInfo.CommandLine
            $projectServer = $commandLine -match 'edu\.college\.employee\.service\.LocalServer' -and
                $commandLine.IndexOf($PSScriptRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0
            $classification = if ($projectServer) { 'confirmed PeopleDesk server' } else { 'another/unconfirmed process' }
            Write-Host "Port $portNumber is occupied by $classification (PID $listenerId, $($processInfo.Name)). No process was stopped. Stop a confirmed PeopleDesk instance with Ctrl+C in its owning terminal, or choose a verified-free port with `$env:EMPLOYEE_PORT." -ForegroundColor Yellow
        } else {
            Write-Host "Port $portNumber is occupied by PID $listenerId, but its command line could not be inspected. No process was stopped. Identify it manually or choose a verified-free port with `$env:EMPLOYEE_PORT." -ForegroundColor Yellow
        }
    }
    exit 2
}

& (Join-Path $PSScriptRoot 'build.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$lib = @(Get-ChildItem (Join-Path $PSScriptRoot 'lib') -Filter '*.jar' | ForEach-Object { $_.FullName })
$cp = (@((Join-Path $PSScriptRoot 'build/classes')) + $lib) -join ';'
Write-Host "Opening http://127.0.0.1:$port/ (Ctrl+C stops the server)"
Write-Host 'Employee, account, request, department, and activity changes last until the server stops. Employee Binary Tree records persist in data/employee-tree.json.'
$assets = Join-Path $PSScriptRoot 'build/classes/public'
$data = Join-Path $PSScriptRoot 'data'
& 'C:\Program Files\Java\jdk-17\bin\java.exe' "-Demployee.assets.dir=$assets" "-Demployee.data.dir=$data" --add-modules jdk.httpserver -cp $cp edu.college.employee.service.LocalServer
exit $LASTEXITCODE
