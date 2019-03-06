@echo off
for /r "./" %%i in (*.ice) do (
	slice2java %%i
	slice2js %%i
)
pause

