# fix_supa_connection.ps1
# Script to fix common connectivity issues for Supabase on Localhost

Write-Host "🔍 Checking Windows Firewall status for Supabase (Port 54321)..." -ForegroundColor Cyan

$port = 54321
$ruleName = "Allow Supabase 54321"

# 1. Check if port is open
$existingRule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue

if ($existingRule) {
    Write-Host "✅ Firewall rule '$ruleName' already exists." -ForegroundColor Green
} else {
    Write-Host "⚠️ Firewall rule not found. Creating it now..." -ForegroundColor Yellow
    try {
        New-NetFirewallRule -DisplayName $ruleName `
                            -Direction Inbound `
                            -LocalPort $port `
                            -Protocol TCP `
                            -Action Allow `
                            -Profile Any
        Write-Host "✅ Successfully created inbound rule for port $port" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to create rule. Please run this script as Administrator!" -ForegroundColor Red
        Write-Host "   Right-click the script -> 'Run with PowerShell' as Admin"
        exit
    }
}

# 2. Check Network Profile
Write-Host "`n🔍 Checking Network Profile..." -ForegroundColor Cyan
$connection = Get-NetConnectionProfile
foreach ($conn in $connection) {
    Write-Host "   Interface: $($conn.InterfaceAlias)"
    Write-Host "   Network Category: $($conn.NetworkCategory)"
    
    if ($conn.NetworkCategory -eq "Public") {
        Write-Host "   ⚠️ WARNING: Your network is set to 'Public'." -ForegroundColor Red
        Write-Host "      This blocks incoming connections even if firewall is open."
        Write-Host "      Run this command as Admin to change to Private: "
        Write-Host "      Set-NetConnectionProfile -InterfaceIndex $($conn.InterfaceIndex) -NetworkCategory Private" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ Network is Private (Good for local development)" -ForegroundColor Green
    }
}

# 3. Check Docker status
Write-Host "`n🔍 Checking Docker Container..." -ForegroundColor Cyan
if (Get-Command docker -ErrorAction SilentlyContinue) {
    docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Status}}" | Select-String "54321"
    if ($?) {
        Write-Host "✅ Docker container hosting Supabase seems to be running." -ForegroundColor Green
    } else {
        Write-Host "❌ No Docker container found listening on port 54321." -ForegroundColor Red
    }
} else {
    Write-Host "⚠️ Docker command not found." -ForegroundColor Yellow
}

Write-Host "`n📢 INSTRUCTIONS:" -ForegroundColor White
Write-Host "1. If you saw 'Failed to create rule', run this script as ADMINISTRATOR."
Write-Host "2. If your network is PUBLIC, change it to PRIVATE."
Write-Host "3. Try navigating to http://$((Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.InterfaceAlias -match 'Wi-Fi' -or $_.InterfaceAlias -match 'Ethernet'}).IPAddress[0]):54321 in your PHONE browser."
Write-Host "`nPress Enter to exit..."
Read-Host
