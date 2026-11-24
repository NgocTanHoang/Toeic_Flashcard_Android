param(
    [string]$Choice = ''
)

[Console]::InputEncoding  = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$env:ANDROID_AVD_HOME = 'D:\AndroidAVD'
$SdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_SDK_ROOT = $SdkRoot
$EmulatorDir = Join-Path $SdkRoot 'emulator'
$EmulatorExe = Join-Path $EmulatorDir 'emulator.exe'
$AdbExe = Join-Path $SdkRoot 'platform-tools\adb.exe'
if (-not (Test-Path $AdbExe)) { $AdbExe = 'adb' }

$ProjectRoot = Split-Path -Parent $PSScriptRoot

$requiredPaths = @(
    $EmulatorDir,
    (Join-Path $EmulatorDir 'lib64'),
    (Join-Path $EmulatorDir 'lib64\qt\lib'),
    (Join-Path $SdkRoot 'platform-tools')
)
$env:PATH = (($requiredPaths -join ';') + ';' + $env:PATH)

$AvdCatalog = [ordered]@{
    '1' = @{ Name = 'TECNO_CLA5_Phone';     Scale = 0.34; Label = 'Điện thoại chuẩn 20.3:9   (1080 x 2436 • 420 dpi)' }
    '2' = @{ Name = 'Xiaomi_Redmi_Pad_2';   Scale = 0.32; Label = 'Tablet 11" 16:10         (1600 x 2560 • 274 dpi)' }
    '3' = @{ Name = 'OPPO_Pad_SE';          Scale = 0.34; Label = 'Tablet OPPO Pad SE 7:5   (1600 x 2240 • 280 dpi)' }
    '4' = @{ Name = 'Galaxy_Z_Fold_Inner';  Scale = 0.36; Label = 'Màn hình gập trong 20:18 (1968 x 2184 • 374 dpi)' }
    '5' = @{ Name = 'Galaxy_Z_Fold_Cover';  Scale = 0.35; Label = 'Màn hình phụ hẹp 22:9    (968 x 2376  • 420 dpi)' }
    '6' = @{ Name = 'Xiaomi_Pad_8';         Scale = 0.26; Label = 'Tablet 11.2" 3:2         (2136 x 3200 • 345 dpi)' }
    '7' = @{ Name = 'Galaxy_Tab_16_9';      Scale = 0.42; Label = 'Tablet truyền thống 16:9 (1080 x 1920 • 213 dpi)' }
}

$QuickProfiles = [ordered]@{
    'Q1' = @{ Size = '1080x2436'; Density = '420'; Label = 'Điện thoại dọc 20.3:9      (1080 x 2436 • 420 dpi)' }
    'Q2' = @{ Size = '2436x1080'; Density = '420'; Label = 'Điện thoại xoay ngang 2-pane(2436 x 1080 • 420 dpi)' }
    'Q3' = @{ Size = '968x2376';  Density = '420'; Label = 'Z Fold Cover siêu hẹp 22:9 (968 x 2376  • 420 dpi)' }
    'Q4' = @{ Size = '1968x2184'; Density = '374'; Label = 'Z Fold Inner 20:18 (2 cột) (1968 x 2184 • 374 dpi)' }
    'Q5' = @{ Size = '1600x2240'; Density = '280'; Label = 'OPPO Pad SE 7:5 (2 cột)    (1600 x 2240 • 280 dpi)' }
    'Q6' = @{ Size = '1600x2560'; Density = '274'; Label = 'Redmi Pad 2 dọc 16:10      (1600 x 2560 • 274 dpi)' }
    'Q7' = @{ Size = '2560x1600'; Density = '274'; Label = 'Redmi Pad 2 ngang (3 cột)  (2560 x 1600 • 274 dpi)' }
    'Q8'  = @{ Size = '2136x3200'; Density = '345'; Label = 'Xiaomi Pad 8 dọc 3:2       (2136 x 3200 • 345 dpi)' }
    'Q9'  = @{ Size = '1080x1218'; Density = '420'; Label = 'Split-Screen chia đôi      (1080 x 1218 • 420 dpi)' }
    'Q10' = @{ Size = '1248x1972'; Density = '420'; Label = 'Z Fold 8 Màn phụ 5.5"      (1248 x 1972 • 420 dpi)' }
    'Q11' = @{ Size = '1848x2448'; Density = '370'; Label = 'Z Fold 8 Màn chính 7.6"    (1848 x 2448 • 370 dpi)' }
}

if (-not ('ToeicWinPlacement' -as [type])) {
    Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;

public static class ToeicWinPlacement {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [DllImport("user32.dll")]
    public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int nWidth, int nHeight, bool bRepaint);

    [DllImport("user32.dll")]
    public static extern bool SystemParametersInfo(uint uiAction, uint uiParam, out RECT pvParam, uint fWinIni);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    public static bool ClampEmulatorWindows() {
        RECT workArea;
        if (!SystemParametersInfo(0x0030, 0, out workArea, 0)) {
            workArea.Left = 0; workArea.Top = 0; workArea.Right = 1920; workArea.Bottom = 1040;
        }
        int workW = Math.Max(640, workArea.Right - workArea.Left);
        int workH = Math.Max(480, workArea.Bottom - workArea.Top);
        int maxH = (int)(workH * 0.88);
        bool movedAny = false;

        EnumWindows(delegate (IntPtr hWnd, IntPtr lParam) {
            if (!IsWindowVisible(hWnd)) return true;
            StringBuilder sb = new StringBuilder(256);
            GetWindowText(hWnd, sb, 256);
            string title = sb.ToString();
            if (!title.StartsWith("Android Emulator -", StringComparison.OrdinalIgnoreCase)) return true;

            RECT r;
            if (!GetWindowRect(hWnd, out r)) return true;
            int w = r.Right - r.Left;
            int h = r.Bottom - r.Top;
            if (w <= 80 || h <= 80) return true;

            int newH = Math.Min(h, maxH);
            int newW = (h > maxH) ? Math.Max(220, (int)Math.Round(w * (newH / (double)h))) : w;
            if (newW > workW - 40) {
                double ratio = (workW - 40) / (double)newW;
                newW = workW - 40;
                newH = Math.Max(240, (int)Math.Round(newH * ratio));
            }

            int targetX = workArea.Left + Math.Min(120, Math.Max(20, (workW - newW) / 2));
            int targetY = workArea.Top + 40;
            if (r.Top < workArea.Top || r.Left < workArea.Left || r.Bottom > workArea.Bottom || h > maxH || w > workW) {
                MoveWindow(hWnd, targetX, targetY, newW, newH, true);
                movedAny = true;
            }
            SetForegroundWindow(hWnd);
            return true;
        }, IntPtr.Zero);

        return movedAny;
    }
}
"@
}

function Reset-EmulatorWindowIni {
    param(
        [string]$AvdName,
        [double]$Scale
    )

    $avdFolder = Join-Path $env:ANDROID_AVD_HOME "$AvdName.avd"
    if (-not (Test-Path $avdFolder)) { return }

    $iniPath = Join-Path $avdFolder 'emulator-user.ini'
    $scaleText = $Scale.ToString('0.000000', [System.Globalization.CultureInfo]::InvariantCulture)
    $desired = [ordered]@{
        'window.x'     = '120'
        'window.y'     = '40'
        'window.scale' = $scaleText
    }

    $lines = if (Test-Path $iniPath) { @(Get-Content -Path $iniPath -Encoding UTF8) } else { @() }
    $seen = @{}
    $updated = [System.Collections.Generic.List[string]]::new()
    foreach ($line in $lines) {
        $matched = $false
        foreach ($k in $desired.Keys) {
            if ($line -match ('^' + [regex]::Escape($k) + '\s*=')) {
                $seen[$k] = $true
                [void]$updated.Add("$k = $($desired[$k])")
                $matched = $true
                break
            }
        }
        if (-not $matched) {
            [void]$updated.Add($line)
        }
    }
    foreach ($k in $desired.Keys) {
        if (-not $seen.ContainsKey($k)) {
            [void]$updated.Add("$k = $($desired[$k])")
        }
    }
    $updated | Set-Content -Path $iniPath -Encoding ASCII
}

function Move-EmulatorWindowOnScreen {
    param([int]$MaxWaitSeconds = 15)

    for ($i = 0; $i -lt $MaxWaitSeconds; $i++) {
        $clamped = [ToeicWinPlacement]::ClampEmulatorWindows()
        if ($clamped) {
            Write-Host 'Đã căn chỉnh vị trí & kích thước cửa sổ Emulator vào vùng hiển thị (120, 40).' -ForegroundColor Cyan
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Get-LatestProjectApk {
    $candidates = @(
        (Join-Path $ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'),
        (Join-Path $ProjectRoot 'app\build\outputs\apk\release\app-release.apk')
    ) | Where-Object { Test-Path $_ } | ForEach-Object { Get-Item $_ } | Sort-Object LastWriteTime -Descending

    if ($candidates -and $candidates.Count -gt 0) {
        return $candidates[0].FullName
    }
    return $null
}

function Stop-RunningEmulator {
    param([string]$TargetAvdName = '')

    $devicesText = (& $AdbExe devices 2>$null | Out-String)
    if ($devicesText -match 'emulator-5554') {
        Write-Host 'Đang tắt máy ảo cũ (emulator-5554)...' -ForegroundColor Yellow
        & $AdbExe -s emulator-5554 emu kill 2>$null | Out-Null
    }

    for ($w = 0; $w -lt 15; $w++) {
        $q = Get-Process -Name 'qemu-system-x86_64', 'emulator' -ErrorAction SilentlyContinue
        if (-not $q) { break }
        Start-Sleep -Seconds 1
    }

    $still = Get-Process -Name 'qemu-system-x86_64' -ErrorAction SilentlyContinue
    if ($still) {
        $still | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }

    if ($TargetAvdName) {
        $avdFolder = Join-Path $env:ANDROID_AVD_HOME "$TargetAvdName.avd"
        if (Test-Path $avdFolder) {
            Get-ChildItem $avdFolder -Filter '*.lock' -Force -ErrorAction SilentlyContinue |
                Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        }
    } else {
        Get-ChildItem 'D:\AndroidAVD\*.avd\*.lock' -Force -ErrorAction SilentlyContinue |
            Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Wait-EmulatorReadyAndLaunchApp {
    Write-Host 'Đang chờ máy ảo khởi động hoàn tất...' -ForegroundColor Cyan
    & $AdbExe -s emulator-5554 wait-for-device

    $booted = $false
    for ($i = 0; $i -lt 90; $i++) {
        $prop = (& $AdbExe -s emulator-5554 shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        if ($prop -eq '1') {
            $booted = $true
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $booted) {
        Write-Host 'Hết thời gian chờ boot (180s). Kiểm tra cửa sổ máy ảo!' -ForegroundColor Red
        return
    }

    [void](Move-EmulatorWindowOnScreen -MaxWaitSeconds 3)
    Start-Sleep -Seconds 2
    & $AdbExe -s emulator-5554 shell input keyevent 82 2>$null | Out-Null
    & $AdbExe -s emulator-5554 shell wm size reset 2>$null | Out-Null
    & $AdbExe -s emulator-5554 shell wm density reset 2>$null | Out-Null

    $apkPath = Get-LatestProjectApk
    if ($apkPath) {
        Write-Host "Đang cài APK mới nhất: $apkPath" -ForegroundColor Cyan
        $installOut = (& $AdbExe -s emulator-5554 install -r $apkPath 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) {
            if ($installOut -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') {
                Write-Host 'Phát hiện khác chữ ký (debug/release) — gỡ bản cũ và cài lại...' -ForegroundColor Yellow
                & $AdbExe -s emulator-5554 uninstall com.toeicflashcard 2>$null | Out-Null
                $installOut = (& $AdbExe -s emulator-5554 install $apkPath 2>&1 | Out-Null)
            }
        }
        Write-Host $installOut.Trim()
    } else {
        Write-Host 'Chưa tìm thấy app-debug.apk hoặc app-release.apk; mở trực tiếp bản đã cài sẵn.' -ForegroundColor Yellow
    }

    & $AdbExe -s emulator-5554 shell am start -n com.toeicflashcard/.MainActivity 2>$null | Out-Null
    Write-Host 'ĐÃ SẴN SÀNG! Dùng chuột kéo góc cửa sổ để zoom vừa mắt, hoặc chọn Q1-Q9 để đổi kích thước tức thì.' -ForegroundColor Green
}

function Stop-RunningGradleDaemon {
    $daemons = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like '*GradleDaemon*' }
    if (-not $daemons) { return }

    Write-Host 'Phát hiện Gradle Daemon đang chạy — dừng để giải phóng RAM trước khi bật Emulator...' -ForegroundColor Yellow
    $gradlew = Join-Path $ProjectRoot 'gradlew.bat'
    if (Test-Path $gradlew) {
        $p = Start-Process -FilePath $gradlew -WorkingDirectory $ProjectRoot -ArgumentList '--stop' -NoNewWindow -PassThru
        $null = $p.WaitForExit(5000)
        if (-not $p.HasExited) { $p.Kill() }
    }
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like '*GradleDaemon*' } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}

function Start-SelectedAvd {
    param(
        [string]$AvdName,
        [double]$Scale = 0.35
    )

    Stop-RunningGradleDaemon
    Stop-RunningEmulator -TargetAvdName $AvdName
    Reset-EmulatorWindowIni -AvdName $AvdName -Scale $Scale
    Write-Host "Đang mở máy ảo [$AvdName] (cores=2, memory=1152, gpu=swiftshader_indirect, window=(120,40), scale=$Scale)..." -ForegroundColor Green
    $argsList = @('-avd', $AvdName, '-memory', '1152', '-gpu', 'swiftshader_indirect', '-cores', '2', '-no-boot-anim', '-no-audio', '-no-snapshot-save')
    $null = Start-Process -FilePath $EmulatorExe -WorkingDirectory $EmulatorDir -ArgumentList $argsList -PassThru
    [void](Move-EmulatorWindowOnScreen -MaxWaitSeconds 12)
    Wait-EmulatorReadyAndLaunchApp
}

function Apply-QuickAdbProfile {
    param(
        [string]$Size,
        [string]$Density,
        [string]$Label
    )

    $devicesText = (& $AdbExe devices 2>$null | Out-String)
    if ($devicesText -notmatch 'emulator-5554\s+device') {
        $baseAvd = $AvdCatalog['1']
        Write-Host "Chưa có máy ảo đang chạy — tự bật nền [$($baseAvd.Name)] tiết kiệm RAM trước rồi áp dụng hồ sơ..." -ForegroundColor Yellow
        Start-SelectedAvd -AvdName $baseAvd.Name -Scale $baseAvd.Scale
    }

    Write-Host "Áp dụng tức thì: $Label" -ForegroundColor Cyan
    & $AdbExe -s emulator-5554 shell wm size $Size
    & $AdbExe -s emulator-5554 shell wm density $Density
    & $AdbExe -s emulator-5554 shell am start -n com.toeicflashcard/.MainActivity 2>$null | Out-Null
    Write-Host "Đã chuyển sang $Size @ ${Density}dpi! Nhập [R] để reset về mặc định." -ForegroundColor Green
}

function Invoke-MenuChoice {
    param([string]$InputChoice)

    $c = $InputChoice.Trim().ToUpperInvariant()
    if ($c -eq '0' -or $c -eq '') {
        Write-Host 'Đã thoát launcher.'
        return $false
    } elseif ($AvdCatalog.Contains($c)) {
        $picked = $AvdCatalog[$c]
        Start-SelectedAvd -AvdName $picked.Name -Scale $picked.Scale
    } elseif ($QuickProfiles.Contains($c)) {
        $qp = $QuickProfiles[$c]
        Apply-QuickAdbProfile -Size $qp.Size -Density $qp.Density -Label $qp.Label
    } elseif ($c -eq 'R') {
        & $AdbExe -s emulator-5554 shell wm size reset
        & $AdbExe -s emulator-5554 shell wm density reset
        & $AdbExe -s emulator-5554 shell settings put system accelerometer_rotation 1
        Write-Host 'Đã reset wm size & density về mặc định.' -ForegroundColor Green
    } elseif ($c -eq 'W') {
        if (-not (Move-EmulatorWindowOnScreen -MaxWaitSeconds 2)) {
            Write-Host 'Không tìm thấy cửa sổ Android Emulator đang hiển thị trên màn hình.' -ForegroundColor Yellow
        }
    } elseif ($c -eq 'K') {
        Stop-RunningEmulator
        Write-Host 'Đã tắt sạch máy ảo và xóa toàn bộ file .lock để giải phóng RAM.' -ForegroundColor Green
    } else {
        Write-Host "Lựa chọn '$InputChoice' không hợp lệ." -ForegroundColor Red
    }
    return $true
}

if ($Choice -ne '') {
    [void](Invoke-MenuChoice -InputChoice $Choice)
    exit 0
}

while ($true) {
    Write-Host ''
    Write-Host '========================================================================' -ForegroundColor Cyan
    Write-Host '  TOEIC FLASHCARD — LAUNCHER ĐA THIẾT BỊ TIẾT KIỆM RAM (D:\AndroidAVD)  ' -ForegroundColor Cyan
    Write-Host '========================================================================' -ForegroundColor Cyan
    Write-Host '  [KHỞI ĐỘNG AVD NGUYÊN BẢN — 2 cores, 1152 MB RAM]' -ForegroundColor Yellow
    foreach ($key in $AvdCatalog.Keys) {
        $entry = $AvdCatalog[$key]
        Write-Host ("  [{0}]  {1,-21} | {2}" -f $key, $entry.Name, $entry.Label)
    }
    Write-Host ''
    Write-Host '  [CHUYỂN NHANH QUA ADB TRÊN 1 MÁY ẢO ĐANG CHẠY — NỀN TECNO_CLA5_Phone]' -ForegroundColor Yellow
    foreach ($key in $QuickProfiles.Keys) {
        $qp = $QuickProfiles[$key]
        Write-Host ("  [{0,-3}] {1}" -f $key, $qp.Label)
    }
    Write-Host ''
    Write-Host '  [R]  Reset kích thước/mật độ ADB về mặc định của máy ảo đang chạy'
    Write-Host '  [W]  Kéo cửa sổ máy ảo đang chạy về màn hình chính (fix văng mép trên)'
    Write-Host '  [K]  Tắt sạch máy ảo & xóa file .lock (giải phóng RAM)'
    Write-Host '  [0]  Thoát'
    Write-Host '------------------------------------------------------------------------' -ForegroundColor DarkGray

    $userChoice = Read-Host 'Nhập lựa chọn (1-7, Q1-Q11, R, W, K, 0)'
    if ($null -eq $userChoice) { break }
    $keepGoing = Invoke-MenuChoice -InputChoice $userChoice
    if (-not $keepGoing) { break }
}
