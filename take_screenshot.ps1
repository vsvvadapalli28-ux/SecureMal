Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Start-Sleep -Seconds 1
$bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$bmp = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
$outPath = "C:\Users\lalit\.gemini\antigravity\brain\d45634f0-f4f7-4ca7-b6d6-f630edab3a25\desktop_screenshot.png"
$bmp.Save($outPath)
$g.Dispose()
$bmp.Dispose()
Write-Host "Screenshot saved to: $outPath"
