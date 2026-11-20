# Выпуск новой версии: .\release.ps1 "Что нового"
# Поднимает versionCode, собирает подписанный APK, заливает код и создаёт релиз vN на GitHub.
# Приложение у пользователя увидит релиз при следующем запуске и предложит обновиться.
# ВАЖНО: подпись — ключ из %USERPROFILE%\.perekur (вне репозитория). Другой ключ = телефон не поставит обновление.
param([Parameter(Mandatory)][string]$Notes)
$ErrorActionPreference = "Continue"  # git/gradle пишут предупреждения в stderr — ошибки проверяем по коду выхода
Set-Location $PSScriptRoot
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# 1. ключ подписи на месте? (иначе обновление не встанет поверх старой версии)
$keyDir = "$env:USERPROFILE\.perekur"
if (-not (Test-Path "$keyDir\perekur.jks") -or -not (Test-Path "$keyDir\keystore.properties")) {
  throw "Нет ключа подписи в $keyDir. Без него APK не подписать, а обновление с чужим ключом телефон не установит."
}

# 2. интерфейс -> assets
$head = "<!doctype html>`n<html lang=`"ru`"><head><meta charset=`"utf-8`"><meta name=`"viewport`" content=`"width=device-width, initial-scale=1, viewport-fit=cover`">`n"
$utf8 = New-Object Text.UTF8Encoding $false
[IO.File]::WriteAllText("$PWD\android\app\src\main\assets\index.html", $head + [IO.File]::ReadAllText("$PWD\perekur.html") + "`n</html>`n", $utf8)

# 3. версия +1
$gradleFile = "$PWD\android\app\build.gradle"
$g = [IO.File]::ReadAllText($gradleFile)
$code = [int]([regex]::Match($g, 'versionCode (\d+)').Groups[1].Value) + 1
$g = $g -replace 'versionCode \d+', "versionCode $code" -replace 'versionName "[^"]*"', "versionName `"1.$($code - 1)`""
[IO.File]::WriteAllText($gradleFile, $g, $utf8)

# 4. сборка подписанного release-APK (только здесь: ключ этого компьютера)
Push-Location android; cmd /c ".\gradlew.bat assembleRelease --console=plain -q"; $ok = $LASTEXITCODE -eq 0; Pop-Location  # cmd /c: иначе предупреждения Gradle в stderr роняют скрипт
if (-not $ok) { throw "Сборка не удалась" }
$apkPath = "$PWD\android\app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) { throw "Нет подписанного APK: $apkPath — проверь signingConfigs в build.gradle" }
Copy-Item $apkPath Perekur.apk -Force

# 5. GitHub
git add -A; git commit -q -m "Версия 1.$($code - 1): $Notes"; git push -q
if ($LASTEXITCODE -ne 0) { throw "git push не удался" }
gh release create "v$code" Perekur.apk --title "1.$($code - 1)" --notes $Notes
if ($LASTEXITCODE -ne 0) { throw "Релиз не создан" }
Write-Host "Готово: v$code"
