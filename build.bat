@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo   文档工具箱 - 一体化单 JAR 打包脚本
echo ========================================

echo.
echo [1/2] 构建前端资源 (frontend/dist)...
pushd "%~dp0frontend"
if not exist "node_modules" (
  echo 检测到尚未安装前端依赖，正在执行 npm install...
  call npm install
  if errorlevel 1 (
    echo [错误] 前端依赖安装失败
    popd
    exit /b 1
  )
)
call npm run build
if errorlevel 1 (
  echo [错误] 前端构建失败
  popd
  exit /b 1
)
popd

echo.
echo [2/2] 打包后端单 JAR（内嵌前端静态文件）...
pushd "%~dp0backend"
call mvn -DskipTests package
if errorlevel 1 (
  echo [错误] 后端打包失败
  popd
  exit /b 1
)
popd

echo.
echo [完成] 单 JAR 已生成：backend\target\tool-kit-1.0.0.jar
echo 可直接运行 start.bat 或执行：java -jar backend\target\tool-kit-1.0.0.jar
endlocal
