@echo off
chcp 65001 >nul
setlocal

set "JAR=%~dp0backend\target\tool-kit-1.0.0.jar"
set "URL=http://localhost:8080"

if not exist "%JAR%" (
  echo 未找到可执行 JAR，正在自动执行 build.bat 构建...
  call "%~dp0build.bat"
  if errorlevel 1 (
    pause
    exit /b 1
  )
)

echo 正在启动文档工具箱（%URL%）...
echo 保持此窗口运行；关闭此窗口将停止服务。
echo.

:: 后台轮询健康检查，就绪后自动打开默认浏览器
start "" /b powershell -NoProfile -Command "for($i=0;$i -lt 30;$i++){ try { $r = Invoke-WebRequest '%URL%/api/health' -UseBasicParsing -TimeoutSec 2; if($r.StatusCode -eq 200){ Start-Process '%URL%'; break } } catch {}; Start-Sleep -Milliseconds 500 }"

java -jar "%JAR%"
endlocal
