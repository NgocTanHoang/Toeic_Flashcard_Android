@echo off
chcp 65001 >nul
set "ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_AVD_HOME=D:\AndroidAVD"
set "EMU_DIR=%ANDROID_SDK_ROOT%\emulator"
set "PATH=%EMU_DIR%;%EMU_DIR%\lib64;%EMU_DIR%\lib64\qt\lib;%ANDROID_SDK_ROOT%\platform-tools;%PATH%"
title TOEIC Flashcard - Multi-Device AVD Launcher
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\launch_toeic_avd.ps1"
