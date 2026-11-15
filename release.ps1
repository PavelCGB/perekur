# Выпуск новой версии: .\release.ps1 "Что нового"
# Поднимает versionCode, собирает APK, заливает код и создаёт релиз vN на GitHub.
# Приложение у пользователя увидит релиз при следующем запуске и предложит обновиться.
param([Parameter(Mandatory)][string]$Notes)
$ErrorActionPreference = "Continue"  # git/gradle пишут предупреждения в stderr — ошибки проверяем по коду выхода
Set-Location $PSScriptRoot
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")

# 1. интерфейс -> assets
$head = "<!doctype html>`n<html lang=`"ru`"><head><meta charset=`"utf-8`"><meta name=`"viewport`" content=`"width=device-width, initial-scale=1, viewport-fit=cover`">`n"
$utf8 = New-Object Text.UTF8Encoding $false
[IO.File]::WriteAllText("$PWD\android\app\src\main\assets\index.html", $head + [IO.File]::ReadAllText("$PWD\perekur.html") + "`n</html>`n", $utf8)

# 2. версия +1
$gradleFile = "$PWD\android\app\build.gradle"
$g = [IO.File]::ReadAllText($gradleFile)
$code = [int]([regex]::Match($g, 'versionCode (\d+)').Groups[1].Value) + 1
$g = $g -replace 'versionCode \d+', "versionCode $code" -replace 'versionName "[^"]*"', "versionName `"1.$($code - 1)`""
[IO.File]::WriteAllText($gradleFile, $g, $utf8)

# 3. сборка (подпись ключом этого компьютера — собирать обновления только здесь)
Push-Location android; cmd /c ".\gradlew.bat assembleDebug --console=plain -q"; $ok = $LASTEXITCODE -eq 0; Pop-Location  # cmd /c: иначе предупреждения Gradle в stderr роняют скрипт
if (-not $ok) { throw "Сборка не удалась" }
Copy-Item android\app\build\outputs\apk\debug\app-debug.apk Perekur.apk -Force

# 4. GitHub
git add -A; git commit -q -m "Версия 1.$($code - 1): $Notes"; git push -q
if ($LASTEXITCODE -ne 0) { throw "git push не удался" }
gh release create "v$code" Perekur.apk --title "1.$($code - 1)" --notes $Notes
if ($LASTEXITCODE -ne 0) { throw "Релиз не создан" }
Write-Host "Готово: v$code"
