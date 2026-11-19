$ErrorActionPreference = 'Stop'
$jdk = 'C:\Program Files\Java\jdk-17\bin'
if (-not (Test-Path (Join-Path $jdk 'javac.exe'))) { throw 'Java 17 JDK is required. Install JDK 17 and update build.ps1 if it is installed elsewhere.' }
$lib = @(Get-ChildItem (Join-Path $PSScriptRoot 'lib') -Filter '*.jar' | ForEach-Object { $_.FullName })
if ($lib.Count -lt 5) { throw 'Required libraries are missing from lib/. Restore the bundled Jackson and jBCrypt JARs before building.' }
$cp = $lib -join ';'
$classes = Join-Path $PSScriptRoot 'build/classes'
if (Test-Path $classes) { Remove-Item -LiteralPath $classes -Recurse -Force }
New-Item -ItemType Directory -Force $classes | Out-Null
$sources = @(Get-ChildItem (Join-Path $PSScriptRoot 'src/main/java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
if (-not $sources.Count) { throw 'No Java sources found.' }
& (Join-Path $jdk 'javac.exe') --release 17 --add-modules jdk.httpserver -encoding UTF-8 -cp $cp -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
$public = Join-Path $classes 'public'
if (Test-Path $public) { Remove-Item -LiteralPath $public -Recurse -Force }
New-Item -ItemType Directory -Force $public | Out-Null
Get-ChildItem (Join-Path $PSScriptRoot 'src/main/webapp') | Copy-Item -Destination $public -Recurse -Force
Write-Host 'Build succeeded with Java 17.'
