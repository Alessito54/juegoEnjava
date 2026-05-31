@echo off
setlocal enabledelayedexpansion
set SRC=src
set OUT=bin
set DIST=dist
set MAIN=org.drgnst.game.Main

if exist %OUT% rmdir /s /q %OUT%
if exist %DIST% rmdir /s /q %DIST%
mkdir %OUT% 2>nul
mkdir %DIST% 2>nul

echo Compiling Java sources...
if exist sources.txt del /q sources.txt
for /r %SRC% %%f in (*.java) do echo %%f>>sources.txt
javac -d %OUT% @sources.txt
del /q sources.txt

echo Creating jar...
jar cfe %DIST%\game.jar %MAIN% -C %OUT% .

for %%d in (image sonidos res levels textures) do (
  if exist %%d (
    jar uf %DIST%\game.jar -C %%d .
  )
)

rem Include top-level wav files
for %%f in (*.wav) do (
  if exist %%f (
    jar uf %DIST%\game.jar %%f
  )
)

echo Build complete: %DIST%\game.jar
