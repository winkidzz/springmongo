# Spring Boot Management Script
param(
    [switch]$Start,
    [switch]$Stop,
    [switch]$Restart,
    [switch]$Status
)

$port = 8081
$healthCheckUrl = "http://localhost:$port/actuator/health"
$maxAttempts = 60  # 60 attempts * 2 seconds = 120 seconds maximum wait time
$waitTimeBetweenAttempts = 2  # seconds
$minimumStartupTime = 20  # seconds

function Test-SpringBootHealth {
    try {
        $response = Invoke-RestMethod -Uri $healthCheckUrl -Method Get -ErrorAction Stop
        $responseContent = $response | ConvertTo-Json -Compress
        $healthStatus = $responseContent | ConvertFrom-Json
        
        if ($healthStatus.status -eq "UP") {
            Write-Host "Application is healthy"
            return $true
        } else {
            Write-Host "Application is not healthy. Status: $($healthStatus.status)"
            return $false
        }
    } catch {
        Write-Host "Health check failed - Error: $($_.Exception.Message)"
        return $false
    }
}

function Wait-ForSpringBootHealth {
    Write-Host "Waiting minimum startup time of $minimumStartupTime seconds..."
    Start-Sleep -Seconds $minimumStartupTime
    
    Write-Host "Starting health checks..."
    $attempts = 0
    
    while ($attempts -lt $maxAttempts) {
        if (Test-SpringBootHealth) {
            Write-Host "Spring Boot application is healthy!" -ForegroundColor Green
            return $true
        }
        
        $attempts++
        $remainingTime = ($maxAttempts - $attempts) * $waitTimeBetweenAttempts
        Write-Host "Health check attempt $attempts of $maxAttempts. Waiting $waitTimeBetweenAttempts seconds... (Remaining time: $remainingTime seconds)" -ForegroundColor Yellow
        Start-Sleep -Seconds $waitTimeBetweenAttempts
    }
    
    Write-Host "Spring Boot application failed to become healthy within the allotted time ($($maxAttempts * $waitTimeBetweenAttempts) seconds)." -ForegroundColor Red
    return $false
}

function Stop-SpringBoot {
    Write-Host "Stopping Spring Boot application on port $port..."
    $process = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like "*$port*" }
    
    if ($process) {
        $process | Stop-Process -Force
        Write-Host "Spring Boot application stopped successfully." -ForegroundColor Green
    }
    else {
        Write-Host "No Spring Boot application found running on port $port." -ForegroundColor Yellow
    }
}

function Start-SpringBoot {
    Write-Host "Starting Spring Boot application..."
    Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -NoNewWindow
    
    if (Wait-ForSpringBootHealth) {
        Write-Host "Spring Boot application started successfully!" -ForegroundColor Green
    }
    else {
        Write-Host "Spring Boot application failed to start properly." -ForegroundColor Red
        exit 1
    }
}

function Get-SpringBootStatus {
    $process = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like "*$port*" }
    
    if ($process) {
        Write-Host "Spring Boot application is running on port $port." -ForegroundColor Green
        if (Test-SpringBootHealth) {
            Write-Host "Application is healthy." -ForegroundColor Green
        }
        else {
            Write-Host "Application is running but not healthy." -ForegroundColor Yellow
        }
    }
    else {
        Write-Host "Spring Boot application is not running on port $port." -ForegroundColor Red
    }
}

# Main execution
if ($Start) {
    Start-SpringBoot
}
elseif ($Stop) {
    Stop-SpringBoot
}
elseif ($Restart) {
    Stop-SpringBoot
    Start-Sleep -Seconds 5  # Wait for the process to fully stop
    Start-SpringBoot
}
elseif ($Status) {
    Get-SpringBootStatus
}
else {
    Write-Host "Please specify an action: -Start, -Stop, -Restart, or -Status"
    exit 1
} 