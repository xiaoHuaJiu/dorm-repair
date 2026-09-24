param(
    [Parameter(Mandatory=$true)][string]$InputPath,
    [Parameter(Mandatory=$true)][string]$OutputPath,
    [string]$Title = "测试结果",
    [int]$TailLines = 34
)

Add-Type -AssemblyName System.Drawing
$lines = @(Get-Content -LiteralPath $InputPath -Encoding UTF8 | Select-Object -Last $TailLines)
$display = @("$ $Title", "") + $lines
$font = New-Object System.Drawing.Font('Consolas', 15)
$titleFont = New-Object System.Drawing.Font('Microsoft YaHei UI', 17, [System.Drawing.FontStyle]::Bold)
$width = 1500
$height = [Math]::Max(320, 42 + $display.Count * 25)
$bitmap = New-Object System.Drawing.Bitmap($width, $height)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::FromArgb(30,30,30))
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
$green = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(120,220,150))
$white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230,230,230))
$graphics.DrawString($display[0], $titleFont, $green, 24, 16)
$y = 58
foreach ($line in $display[2..($display.Count-1)]) {
    $graphics.DrawString($line, $font, $white, 24, $y)
    $y += 25
}
$directory = Split-Path -Parent $OutputPath
if (-not (Test-Path -LiteralPath $directory)) { New-Item -ItemType Directory -Path $directory | Out-Null }
$bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose(); $bitmap.Dispose(); $font.Dispose(); $titleFont.Dispose(); $green.Dispose(); $white.Dispose()
