@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo   文档工具箱 - Windows EXE 绿色版打包
echo ========================================

where jpackage >nul 2>&1
if errorlevel 1 (
  echo [错误] 未找到 jpackage 命令，请确认已安装 JDK 17+ 并配置 JAVA_HOME\bin 到 PATH
  exit /b 1
)

echo.
echo [1/3] 构建前端并生成单 JAR...
call "%~dp0build.bat"
if errorlevel 1 exit /b 1

set "STAGE=%~dp0backend\target\jpackage-input"
set "OUT=%~dp0release"
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%"
copy /y "%~dp0backend\target\tool-kit-1.0.0.jar" "%STAGE%\tool-kit-1.0.0.jar" >nul

if exist "%OUT%\DocToolkit" rmdir /s /q "%OUT%\DocToolkit"
if not exist "%OUT%" mkdir "%OUT%"

echo.
echo [2/3] 使用 jpackage 生成免安装 EXE（内嵌精简 JRE，无黑框 + 系统托盘）...
jpackage ^
  --type app-image ^
  --name DocToolkit ^
  --app-version 1.0.0 ^
  --vendor "doc-toolkit" ^
  --description "文档工具箱 - 本地 PDF / 图片 / Word 批量处理工具" ^
  --icon "%~dp0assets\app.ico" ^
  --input "%STAGE%" ^
  --main-jar tool-kit-1.0.0.jar ^
  --dest "%OUT%" ^
  --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.zipfs ^
  --java-options "-Dtoolkit.desktop=true" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --java-options "-Xms128m" ^
  --java-options "-Xmx1024m"

if errorlevel 1 (
  echo [错误] jpackage 打包失败
  exit /b 1
)

echo.
echo [3/3] 压缩为便携分发包 release\DocToolkit-win-x64.zip...
powershell -NoProfile -Command "if(Test-Path '%OUT%\DocToolkit-win-x64.zip'){ Remove-Item '%OUT%\DocToolkit-win-x64.zip' -Force }; Compress-Archive -Path '%OUT%\DocToolkit' -DestinationPath '%OUT%\DocToolkit-win-x64.zip'"

echo.
echo [完成]
echo   绿色版目录：%OUT%\DocToolkit\DocToolkit.exe
echo   便携压缩包：%OUT%\DocToolkit-win-x64.zip
echo   （目标电脑无需安装 Java 或 Node.js，解压后双击 DocToolkit.exe 即可使用）
endlocal

