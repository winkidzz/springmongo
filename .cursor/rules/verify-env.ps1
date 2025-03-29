# Environment Verification Script

Write-Host "Verifying Environment Setup..." -ForegroundColor Green

# Check Java
Write-Host "`nChecking Java..." -ForegroundColor Yellow
$javaVersion = java -version 2>&1
Write-Host "Java Version: $javaVersion"

# Check Maven
Write-Host "`nChecking Maven..." -ForegroundColor Yellow
$mavenVersion = mvn -v
Write-Host "Maven Version: $mavenVersion"

# Check SSH Key
Write-Host "`nChecking SSH Key..." -ForegroundColor Yellow
$sshKeyPath = "~/.ssh/ubuntu_vm_key"
if (Test-Path $sshKeyPath) {
    Write-Host "SSH Key found at: $sshKeyPath" -ForegroundColor Green
} else {
    Write-Host "SSH Key not found at: $sshKeyPath" -ForegroundColor Red
}

# Test VM Connection
Write-Host "`nTesting VM Connection..." -ForegroundColor Yellow
try {
    $vmTest = ssh -i ~/.ssh/ubuntu_vm_key godzilla@localhost -p 2222 "echo 'VM Connection Test Successful'"
    Write-Host $vmTest -ForegroundColor Green
} catch {
    Write-Host "Failed to connect to VM: $_" -ForegroundColor Red
}

# Check Docker Containers
Write-Host "`nChecking Docker Containers..." -ForegroundColor Yellow
try {
    $containers = ssh -i ~/.ssh/ubuntu_vm_key godzilla@localhost -p 2222 "docker ps --format '{{.Names}}: {{.Status}}'"
    Write-Host "Running Containers:" -ForegroundColor Green
    Write-Host $containers
} catch {
    Write-Host "Failed to check Docker containers: $_" -ForegroundColor Red
}

# Test MongoDB Connection
Write-Host "`nTesting MongoDB Connection..." -ForegroundColor Yellow
try {
    $mongoTest = ssh -i ~/.ssh/ubuntu_vm_key godzilla@localhost -p 2222 "docker exec mongodb mongosh --eval 'db.runCommand({ping:1})'"
    Write-Host "MongoDB Connection Test Successful" -ForegroundColor Green
} catch {
    Write-Host "Failed to connect to MongoDB: $_" -ForegroundColor Red
}

# Test Port Forwarding
Write-Host "`nTesting Port Forwarding..." -ForegroundColor Yellow
$portTest = Test-NetConnection -ComputerName localhost -Port 27017 -WarningAction SilentlyContinue
if ($portTest.TcpTestSucceeded) {
    Write-Host "Port 27017 is accessible" -ForegroundColor Green
} else {
    Write-Host "Port 27017 is not accessible" -ForegroundColor Red
}

Write-Host "`nEnvironment Verification Complete" -ForegroundColor Green 