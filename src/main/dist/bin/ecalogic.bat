@echo off

setlocal enableextensions

pushd %~dp0..
set _ECALOGIC_HOME=%CD%
popd

rem We keep in _JAVA_ARGS all -J-prefixed and -D-prefixed arguments
set _JAVA_ARGS=
set _PROGRAM_ARGS=

:param_loop

if "%~1" == "" goto param_afterloop

set _TEST_PARAM=%~1
if "%_TEST_PARAM:~0,2%" == "-J" (
  set _JAVA_ARGS=%_JAVA_ARGS% %_TEST_PARAM:~2%
) else if "%_TEST_PARAM:~0,2%" == "-D" (
  rem test if this was double-quoted property "-Dprop=42"
  for /f "delims== tokens=1-2" %%G in ("%_TEST_PARAM%") DO (
    if not "%%G" == "%_TEST_PARAM%" (
      rem double quoted: "-Dprop=42" -> -Dprop="42"
      set _JAVA_ARGS=%_JAVA_ARGS% %%G="%%H"
    ) else if not "%2" == "" (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      set _JAVA_ARGS=%_JAVA_ARGS% %_TEST_PARAM%=%2
      shift
    )
  )
) else (
  set _PROGRAM_ARGS=%_PROGRAM_ARGS% %1
)
shift
goto param_loop

:param_afterloop

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if not defined _JAVACMD (
  if not "%JAVA_HOME%" == "" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%" == "" set _JAVACMD=java

rem We use the value of the JAVA_OPTS environment variable if defined
set _JAVA_OPTS=%JAVA_OPTS%
if not defined _JAVA_OPTS set _JAVA_OPTS=-Xmx256M -Xms32M

rem We append _JAVA_ARGS java arguments to JAVA_OPTS if necessary
if defined _JAVA_ARGS set _JAVA_OPTS=%_JAVA_OPTS%%_JAVA_ARGS%

set _CLASSPATH=%_ECALOGIC_HOME%\conf
for %%f in ("%_ECALOGIC_HOME%\lib\*") do call :add_cpath "%%f"
for /d %%f in ("%_ECALOGIC_HOME%\lib\*") do call :add_cpath "%%f"

rem echo "%_JAVACMD%" %_JAVA_OPTS% -Decalogic.home="%_ECALOGIC_HOME%" -cp "%_CLASSPATH%" nl.ru.cs.ecalogic.ECALogic %_PROGRAM_ARGS%
"%_JAVACMD%" %_JAVA_OPTS% -Decalogic.home="%_ECALOGIC_HOME%" -cp "%_CLASSPATH%" nl.ru.cs.ecalogic.ECALogic %_PROGRAM_ARGS%
goto end

:add_cpath
  set _CLASSPATH=%_CLASSPATH%;%~1
goto :eof

:end
endlocal

REM exit code fix, see http://stackoverflow.com/questions/4632891/exiting-batch-with-exit-b-x-where-x-1-acts-as-if-command-completed-successfu
@"%COMSPEC%" /C exit %ERRORLEVEL% > nul
