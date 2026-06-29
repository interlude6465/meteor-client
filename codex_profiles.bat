@echo off
:: --- FORCE ADMIN RIGHTS START ---
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo Requesting administrative privileges...
    goto UACPrompt
) else ( goto gotAdmin )
:UACPrompt
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    echo UAC.ShellExecute "%~s0", "", "", "runas", 1 >> "%temp%\getadmin.vbs"
    "%temp%\getadmin.vbs"
    exit /B
:gotAdmin
    if exist "%temp%\getadmin.vbs" del "%temp%\getadmin.vbs"
    pushd "%~dp0"
:: --- FORCE ADMIN RIGHTS END ---

set "DAT=%~dp0codex_profiles.dat"

:menu
cls
echo ===================================================
echo CURRENTLY ACTIVE CODEX PROFILE: %LABEL%
echo ===================================================
echo.
echo Select a new Codex Key Profile [ADMIN]:
echo ---------------------------------------------------

powershell -NoProfile -Command "$dat='%DAT%'; $tz8=[TimeZoneInfo]::CreateCustomTimeZone('UTC+8',[TimeSpan]::FromHours(8),'',''); $melbTz=[TimeZoneInfo]::FindSystemTimeZoneById('Aus Eastern Standard Time'); $lines=@(); if(Test-Path $dat){ $lines=Get-Content $dat }; function Get-Limit($vname){ foreach($l in $lines){ if($l -match ('^'+[regex]::Escape($vname)+'=(.+)$')){ return $Matches[1].Trim() } }; return $null }; function Clear-Limit($vname){ $script:lines = $script:lines | Where-Object { $_ -notmatch ('^'+[regex]::Escape($vname)+'=') }; $script:lines | Set-Content $dat }; function Show-Profile($label,$vname){ $ts=Get-Limit $vname; if(-not $ts){ Write-Host ($label+' [READY]') -ForegroundColor Green; return }; try{ $t=[datetime]::ParseExact($ts,'yyyy-MM-dd h:mm tt',[Globalization.CultureInfo]::InvariantCulture) }catch{ try{ $t=[datetime]::ParseExact($ts,'yyyy-MM-dd hh:mm tt',[Globalization.CultureInfo]::InvariantCulture) }catch{ Clear-Limit $vname; Write-Host ($label+' [READY]') -ForegroundColor Green; return } }; $utc=[TimeZoneInfo]::ConvertTimeToUtc($t,$tz8); $melb=[TimeZoneInfo]::ConvertTimeFromUtc($utc,$melbTz); $now=[TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow,$melbTz); $diff=$melb-$now; if($diff.TotalSeconds -le 0){ Clear-Limit $vname; Write-Host ($label+' [READY]') -ForegroundColor Green }else{ $h=[Math]::Floor($diff.TotalHours); $cd=($h.ToString().PadLeft(2,'0')+'h '+$diff.Minutes.ToString().PadLeft(2,'0')+'m'); Write-Host ($label+' [LOCKED - '+$cd+' left -> '+$melb.ToString('h:mm tt')+' Melb]') -ForegroundColor Yellow } }; Show-Profile '1. OPERA 810         ' 'LIMIT_OPERA'; Show-Profile '2. EDGE 6465         ' 'LIMIT_EDGE'; Show-Profile '3. CHROME martion    ' 'LIMIT_CHROME_M'; Show-Profile '4. CHROME paid       ' 'LIMIT_CHROME_P'; Show-Profile '5. UNLIMITED SURF    ' 'LIMIT_UNLISURF'; Show-Profile '6. glyph paid        ' 'LIMIT_GLYPH_PAID'; Show-Profile '7. nigle798 paid     ' 'LIMIT_NIGLE798_PAID'; Show-Profile '8. AERO 810          ' 'LIMIT_AERO_810'; Show-Profile '9. AERO martion      ' 'LIMIT_AERO_MARTION'; Show-Profile '10. AERO glyph345    ' 'LIMIT_AERO_GLYPH'; Show-Profile '11. AERO interlude6465' 'LIMIT_AERO_INTERLUDE'; Show-Profile '12. AERO nigle794    ' 'LIMIT_AERO_NIGLE'; Show-Profile '13. AERO aeroapi1    ' 'LIMIT_AERO_AEROAPI1'; Show-Profile '14. HOMELANDER unlim ' 'LIMIT_HOMELANDER'; Show-Profile '15. freemodel4 paid  ' 'LIMIT_FM4'; Show-Profile '16. freemodel3 paid  ' 'LIMIT_FM3'; Show-Profile '17. freemodel2 paid  ' 'LIMIT_FM2'; Show-Profile '18. freemodel1 paid  ' 'LIMIT_FM1'; Show-Profile '19. freemodel7 paid  ' 'LIMIT_FM7'; Show-Profile '20. freemodel8 paid  ' 'LIMIT_FM8'; Show-Profile '21. freemodel9 paid  ' 'LIMIT_FM9'; Show-Profile '22. freemodel10 paid ' 'LIMIT_FM10'; Show-Profile '23. freemodel11 paid ' 'LIMIT_FM11'; $dynLines=$lines | Where-Object { $_ -match '^DYN_PROFILE=' }; $idx=23; foreach($dl in $dynLines){ $idx++; $parts=($dl -replace '^DYN_PROFILE=','') -split '\|'; if($parts.Length -lt 3){ continue }; Show-Profile ($idx.ToString()+'. '+$parts[1].PadRight(17)) $parts[2] }"

echo ===================================================
echo  A. Add new profile
echo  D. Delete a profile
echo ===================================================
echo.
set /p choice="Enter profile number, A to add, D to delete, or R to refresh: "
if /i "%choice%"=="R" goto menu
if /i "%choice%"=="A" goto addProfile
if /i "%choice%"=="D" goto deleteProfile
if "%choice%"=="1" goto profile1
if "%choice%"=="2" goto profile2
if "%choice%"=="3" goto profile3
if "%choice%"=="4" goto profile4
if "%choice%"=="5" goto profile5
if "%choice%"=="6" goto profile6
if "%choice%"=="7" goto profile7
if "%choice%"=="8" goto profile8
if "%choice%"=="9" goto profile9
if "%choice%"=="10" goto profile10
if "%choice%"=="11" goto profile11
if "%choice%"=="12" goto profile12
if "%choice%"=="13" goto profile13
if "%choice%"=="14" goto profile14
if "%choice%"=="15" goto profile15
if "%choice%"=="16" goto profile16
if "%choice%"=="17" goto profile17
if "%choice%"=="18" goto profile18
if "%choice%"=="19" goto profile19
if "%choice%"=="20" goto profile20
if "%choice%"=="21" goto profile21
if "%choice%"=="22" goto profile22
if "%choice%"=="23" goto profile23


set /a dyn_check=%choice% 2>nul
if %dyn_check% GEQ 24 goto dynamicProfile

echo Invalid selection.
pause
goto menu

:: ================== BUILT-IN PROFILES ==================
:: --- FreeModel profiles (need the freemodel base URL or they 401) ---
:profile1
set "KEY=Fe_oa_21c1b7de25a706e33d10799155198662a5bc87bce7e755b6"
set "LABEL=OPERA 810"
set "VAR_NAME=LIMIT_OPERA"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile2
set "KEY=fe_oa_b903284c61f7067112c95ab357740242165236232cfd1713"
set "LABEL=EDGE 6465"
set "VAR_NAME=LIMIT_EDGE"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile3
set "KEY=fe_oa_5ce5855f8f05e7f83c8ae916b1c94227ccde896178a77d88"
set "LABEL=CHROME martion"
set "VAR_NAME=LIMIT_CHROME_M"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile4
set "KEY=fe_oa_ee93abcdd6a5695572549da141a7f1a0499d8b7774987599"
set "LABEL=CHROME paid"
set "VAR_NAME=LIMIT_CHROME_P"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile5
set "KEY=ua_IJU92bMbPaaqP6fwVOJKl8aUD19M7dTB"
set "LABEL=UNLIMITED SURF"
set "VAR_NAME=LIMIT_UNLISURF"
set "BASE_URL=https://unlimited.surf"
goto apply

:profile6
set "KEY=fe_oa_66deae5b28cb20b7d7e586f1b7f1c9143288eccf3b512066"
set "LABEL=glyph paid"
set "VAR_NAME=LIMIT_GLYPH_PAID"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile7
set "KEY=fe_oa_7158ca01a6b18ec1ac6ef9e6f60a6b311b3c9148ec415a2b"
set "LABEL=nigle798 paid"
set "VAR_NAME=LIMIT_NIGLE798_PAID"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:: --- Aero (capi.aerolink.lat) profiles ---
:profile8
set "KEY=aero_live_9tq2QVapUQ21hghmK9ByrkXZvg9-DoW6H03ynDDs_FM"
set "LABEL=AERO aubreymartin810"
set "VAR_NAME=LIMIT_AERO_810"
set "BASE_URL=https://capi.aerolink.lat"
set "MODEL=gpt-5.5"
goto apply

:profile9
set "KEY=aero_live_C3CQaRWuqhyACLf5YxhIpbmfGzJYetsRSPhvkbsbvGY"
set "LABEL=AERO? Aubreymartion"
set "VAR_NAME=LIMIT_AERO_MARTION"
set "BASE_URL=https://capi.aerolink.lat"
set "MODEL=gpt-5.5"
goto apply

:profile10
set "KEY=aero_live_u7bOu2EkACIUEyUKYwTegP9G868dQkJTsHEaNQlHGzE"
set "LABEL=AERO? glyph345"
set "VAR_NAME=LIMIT_AERO_GLYPH"
set "BASE_URL=https://capi.aerolink.lat"
set "MODEL=gpt-5.5"
goto apply

:profile11
set "KEY=aero_live_Dtfgv0zXnDjU0pX8DXWxTxrVwJAA5B4HD0xv0ipufiE"
set "LABEL=AERO interlude6465"
set "VAR_NAME=LIMIT_AERO_INTERLUDE"
set "BASE_URL=https://capi.aerolink.lat"
set "MODEL=gpt-5.5"
goto apply

:profile12
set "KEY=aero_live_uYYu9B2IbZSsnMomlIvZsZ7jxlXjbwwCb7Vh_bdLhqY"
set "LABEL=AERO nigle794"
set "VAR_NAME=LIMIT_AERO_NIGLE"
set "BASE_URL=https://capi.aerolink.lat"
set "MODEL=gpt-5.5"
goto apply

:profile13
set "KEY=sk-96R0YtLXtmqOsPNnLEKnuAtUncDLU6lj5698PUBYDNyZ6be8"
set "LABEL=AERO? aeroapi1"
set "VAR_NAME=LIMIT_AERO_AEROAPI1"
set "BASE_URL=https://api.iamhc.cn"
set "MODEL=glm-5.2"
goto apply

:: --- Homelander ---
:profile14
set "KEY=homelander"
set "LABEL=HOMELANDER unlimited"
set "VAR_NAME=LIMIT_HOMELANDER"
set "BASE_URL=https://homelander.ca"
set "MODEL=gpt-5.5"
goto apply

:: --- More FreeModel profiles (need the freemodel base URL or they 401) ---
:profile15
set "KEY=fe_oa_08fee66dca337b90fd8539c66d4cd7b74648a14a8fe69087"
set "LABEL=freemodel4 paid"
set "VAR_NAME=LIMIT_FM4"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile16
set "KEY=fe_oa_b46f875e89a52701bffd1161d69737be4a0a4d2e5a6dce4f"
set "LABEL=freemodel3 paid"
set "VAR_NAME=LIMIT_FM3"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile17
set "KEY=fe_oa_4326f39a7c3f208f300f46925776a3286c89707035262cf9"
set "LABEL=freemodel2 paid"
set "VAR_NAME=LIMIT_FM2"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile18
set "KEY=fe_oa_a736e4d34a0e79677ff1a5a3746242e44f445f8a082a7bae"
set "LABEL=freemodel1 paid"
set "VAR_NAME=LIMIT_FM1"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile19
set "KEY=fe_oa_858dd94a6a8e4974677dbe826a5d6d688dbee69924a50a34"
set "LABEL=freemodel7 paid"
set "VAR_NAME=LIMIT_FM7"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile20
set "KEY=fe_oa_dd97bbfde8e72760baccca3be62f9e69bc97a1b9f7d120b1"
set "LABEL=freemodel8 paid"
set "VAR_NAME=LIMIT_FM8"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile21
set "KEY=fe_oa_8bc05ee02dff5975db89c44d60977d073a96cc59fe92ca88"
set "LABEL=freemodel9 paid"
set "VAR_NAME=LIMIT_FM9"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile22
set "KEY=fe_oa_b931cf0bac6c25023c4e28eceb809e8cc918b188587a07ad"
set "LABEL=freemodel10 paid"
set "VAR_NAME=LIMIT_FM10"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:profile23
set "KEY=fe_oa_bcf69caf16c88a872d04099f2b0c01ecdddd7e670904decd"
set "LABEL=freemodel11 paid"
set "VAR_NAME=LIMIT_FM11"
set "BASE_URL=https://api.freemodel.dev"
set "MODEL=gpt-5.5"
goto apply

:: ================== DYNAMIC PROFILE SELECTION ==================
:dynamicProfile
set /a dyn_idx=%choice%-24
set "DYN_LINE="
for /f "delims=" %%L in ('powershell -NoProfile -Command ^
  "$dat='%DAT%'; if(-not (Test-Path $dat)){ exit }; $lines=Get-Content $dat | Where-Object { $_ -match '^DYN_PROFILE=' }; $idx=%dyn_idx%; if($idx -lt 0 -or $idx -ge $lines.Count){ exit }; Write-Host ($lines[$idx] -replace '^DYN_PROFILE=','')"') do set "DYN_LINE=%%L"

if "%DYN_LINE%"=="" ( echo Invalid selection. & pause & goto menu )

for /f "tokens=1,2,3 delims=|" %%A in ("%DYN_LINE%") do (
    set "KEY=%%A"
    set "LABEL=%%B"
    set "VAR_NAME=%%C"
)
goto apply

:: ================== ADD PROFILE ==================
:addProfile
cls
echo ===================================================
echo  ADD NEW PROFILE
echo ===================================================
echo.
set "NEW_LABEL="
set "NEW_KEY="
set /p NEW_LABEL="Enter profile name (e.g. FIREFOX work): "
if "%NEW_LABEL%"=="" ( echo Name cannot be empty. & pause & goto menu )
set /p NEW_KEY="Enter API key: "
if "%NEW_KEY%"=="" ( echo Key cannot be empty. & pause & goto menu )

for /f "delims=" %%V in ('powershell -NoProfile -Command ^
  "$n='%NEW_LABEL%' -replace '\s+','_' -replace '[^A-Za-z0-9_]',''; Write-Host ('LIMIT_'+$n.ToUpper())"') do set "NEW_VARNAME=%%V"

echo DYN_PROFILE=%NEW_KEY%^|%NEW_LABEL%^|%NEW_VARNAME%>> "%DAT%"
echo.
echo Profile "%NEW_LABEL%" added successfully!
pause
goto menu

:: ================== DELETE PROFILE ==================
:deleteProfile
cls
echo ===================================================
echo  DELETE A DYNAMIC PROFILE
echo ===================================================
echo.
powershell -NoProfile -Command ^
  "$dat='%DAT%'; if(-not (Test-Path $dat)){ Write-Host 'No dynamic profiles found.' -ForegroundColor Red; exit }; $lines=Get-Content $dat | Where-Object { $_ -match '^DYN_PROFILE=' }; if($lines.Count -eq 0){ Write-Host 'No dynamic profiles found.' -ForegroundColor Red; exit }; $idx=23; foreach($dl in $lines){ $idx++; $parts=($dl -replace '^DYN_PROFILE=','') -split '\|'; Write-Host ($idx.ToString()+'. '+$parts[1]) -ForegroundColor Cyan }"
echo.
set /p del_choice="Enter profile number to delete (24+), or 0 to cancel: "
if "%del_choice%"=="0" goto menu
set /a del_idx=%del_choice%-24
if %del_idx% LSS 0 ( echo Invalid. & pause & goto menu )

powershell -NoProfile -Command ^
  "$dat='%DAT%'; $all=Get-Content $dat; $dynOnly=$all | Where-Object { $_ -match '^DYN_PROFILE=' }; $idx=%del_idx%; if($idx -lt 0 -or $idx -ge $dynOnly.Count){ Write-Host 'Invalid index.' -ForegroundColor Red; exit }; $toRemove=$dynOnly[$idx]; $vname=($toRemove -replace '^DYN_PROFILE=','') -split '\|' | Select-Object -Index 2; $label=($toRemove -replace '^DYN_PROFILE=','') -split '\|' | Select-Object -Index 1; $kept=$all | Where-Object { $_ -ne $toRemove -and $_ -notmatch ('^'+[regex]::Escape($vname)+'=') }; $kept | Set-Content $dat; Write-Host ('Deleted: '+$label) -ForegroundColor Green"

pause
goto menu

:: ================== APPLY + RUN ==================
:apply
if "%MODEL%"=="" set "MODEL=gpt-5.5"
if "%BASE_URL%"=="" set "BASE_URL=https://api.freemodel.dev"
set "CODEX_JS=%APPDATA%\npm\node_modules\@openai\codex\bin\codex.js"

if not exist "%USERPROFILE%\.codex\" mkdir "%USERPROFILE%\.codex"
if not exist "%CODEX_JS%" (
    echo Codex CLI entrypoint not found:
    echo "%CODEX_JS%"
    echo.
    echo Install or repair Codex CLI, then try again.
    pause
    goto menu
)

echo Writing Codex auth.json...
(
echo {
echo   "OPENAI_API_KEY": "%KEY%"
echo }
) > "%USERPROFILE%\.codex\auth.json"

echo Writing Codex config.toml...
(
echo model_provider = "freemodel"
echo model = "%MODEL%"
echo model_reasoning_effort = "xhigh"
echo disable_response_storage = true
echo preferred_auth_method = "apikey"
echo.
echo [model_providers.freemodel]
echo name = "freemodel"
echo base_url = "%BASE_URL%"
echo wire_api = "responses"
) > "%USERPROFILE%\.codex\config.toml"

cls
echo ===================================================
echo Active Profile: %LABEL%
echo ===================================================
echo.
echo Enter a working directory for Codex to launch from
echo (paste a path, or leave blank to use this folder):
set "WORKDIR="
set /p WORKDIR="Working directory: "
:: Strip surrounding quotes if the pasted path was quoted
if defined WORKDIR set "WORKDIR=%WORKDIR:"=%"
if defined WORKDIR (
    if exist "%WORKDIR%\" (
        cd /d "%WORKDIR%"
    ) else (
        echo Directory not found - launching from current folder instead.
        echo "%WORKDIR%"
    )
)

cls
echo ===================================================
echo Active Profile: %LABEL%
echo Working dir:    %CD%
echo ===================================================
echo.
echo Running Codex in a separate window.
echo Use /exit or Ctrl+C in that Codex window to quit.
echo This menu will probe for rate limits after Codex exits.
echo ---------------------------------------------------
echo.

:: Run Codex in a separate console so Ctrl+C does not abort this menu script.
start "Codex - %LABEL%" /wait /D "%CD%" node "%CODEX_JS%"

echo.
echo ---------------------------------------------------
if /i not "%KEY:~0,2%"=="fe" goto skipCodexProbe

echo Codex exited. Probing for FreeModel rate limit...
echo ---------------------------------------------------
echo.

set "PROBE_LOG=%temp%\codex_probe_%RANDOM%.txt"
set "TIME_LOG=%temp%\codex_time_%RANDOM%.txt"
set "RESET_TIME="

powershell -NoProfile -Command ^
  "$script='%CODEX_JS%'; $q=[char]34; $psi=New-Object System.Diagnostics.ProcessStartInfo; $psi.FileName='node.exe'; $psi.Arguments=($q+$script+$q+' exec --skip-git-repo-check hello'); $psi.RedirectStandardOutput=$true; $psi.RedirectStandardError=$true; $psi.UseShellExecute=$false; $psi.CreateNoWindow=$true; try{ $p=[System.Diagnostics.Process]::Start($psi); $out=$p.StandardOutput.ReadToEnd()+$p.StandardError.ReadToEnd(); $done=$p.WaitForExit(60000); if(-not $done){ try{ $p.Kill() }catch{}; $out += [Environment]::NewLine+'Probe timed out.' } }catch{ $out=$_.Exception.Message }; $out | Set-Content '%PROBE_LOG%'"

echo Probe result:
type "%PROBE_LOG%"
echo.

findstr /i /C:"will reset on" "%PROBE_LOG%" > nul 2>&1
if not errorlevel 1 goto parseCodexFreemodel

echo No FreeModel rate limit detected - profile stays READY.
goto cleanupCodexProbe

:parseCodexFreemodel
powershell -NoProfile -Command ^
  "$tz8=[TimeZoneInfo]::CreateCustomTimeZone('UTC+8',[TimeSpan]::FromHours(8),'',''); $now8=[TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow,$tz8); $c=Get-Content '%PROBE_LOG%' -Raw; if($c -match 'will reset on\s+(today|tomorrow)\s+at\s+(\d{1,2}:\d{2}\s*[AP]M)\s*(?:\(UTC\+8\))?'){ $day=$Matches[1].ToLower(); $t=$Matches[2].Trim().ToUpper(); $base=$now8; if($day -eq 'tomorrow'){ $base=$base.AddDays(1) }; ($base.ToString('yyyy-MM-dd')+' '+$t) | Set-Content '%TIME_LOG%' }"
goto haveCodexResetTime

:haveCodexResetTime
if not exist "%TIME_LOG%" (
    echo Could not parse reset time from probe output.
    goto cleanupCodexProbe
)

set /p RESET_TIME=<"%TIME_LOG%"
del "%TIME_LOG%" 2>nul

if "%RESET_TIME%"=="" (
    echo Could not parse reset time from probe output.
    goto cleanupCodexProbe
)

echo RATE LIMITED: resets at %RESET_TIME% UTC+8

set "SAVE_LOG=%temp%\codex_save_%RANDOM%.txt"
echo %RESET_TIME%> "%SAVE_LOG%"
powershell -NoProfile -Command ^
  "$dat='%DAT%'; $vname='%VAR_NAME%'; $time=(Get-Content '%SAVE_LOG%').Trim(); $lines=@(); if(Test-Path $dat){ $lines=Get-Content $dat }; $kept=$lines | Where-Object { $_ -notmatch ('^'+[regex]::Escape($vname)+'=') }; [System.Collections.ArrayList]$arr=@($kept); $arr.Add($vname+'='+$time) | Out-Null; $arr | Set-Content $dat; Write-Host ('Saved '+$vname+'='+$time)"
del "%SAVE_LOG%" 2>nul
goto cleanupCodexProbe

:skipCodexProbe
echo Codex exited. FreeModel rate-limit probe skipped for this key.
echo ---------------------------------------------------

:cleanupCodexProbe
if defined PROBE_LOG if exist "%PROBE_LOG%" del "%PROBE_LOG%" 2>nul
if defined TIME_LOG if exist "%TIME_LOG%" del "%TIME_LOG%" 2>nul
set "BASE_URL="
set "MODEL="
set "KEY="
set "CODEX_JS="
echo.
pause
goto menu

