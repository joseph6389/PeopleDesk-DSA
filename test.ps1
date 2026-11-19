$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$lib = @((Get-ChildItem (Join-Path $PSScriptRoot 'lib') -Filter '*.jar' | ForEach-Object { $_.FullName }) + (Get-ChildItem (Join-Path $PSScriptRoot 'lib/test') -Filter '*.jar' | ForEach-Object { $_.FullName }))
$cp = (@((Join-Path $PSScriptRoot 'build/classes')) + $lib) -join ';'
$tests = Join-Path $PSScriptRoot 'build/test-classes'
New-Item -ItemType Directory -Force $tests | Out-Null
$sources = @(Get-ChildItem (Join-Path $PSScriptRoot 'src/test/java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& 'C:\Program Files\Java\jdk-17\bin\javac.exe' --release 17 -encoding UTF-8 -cp $cp -d $tests $sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
& 'C:\Program Files\Java\jdk-17\bin\javac.exe' --release 17 -encoding UTF-8 -cp $cp -d $tests (Join-Path $PSScriptRoot 'scripts/TestRunner.java')
if ($LASTEXITCODE -ne 0) { throw 'Test runner compilation failed.' }
$temp = Join-Path $PSScriptRoot 'build/test-tmp'
New-Item -ItemType Directory -Force $temp | Out-Null
& 'C:\Program Files\Java\jdk-17\bin\java.exe' "-Djava.io.tmpdir=$temp" -cp ((@($tests, (Join-Path $PSScriptRoot 'build/classes')) + $lib) -join ';') TestRunner
exit $LASTEXITCODE
