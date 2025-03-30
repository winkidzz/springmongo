# Project Configuration and Environment Setup

## Persistent Configurations
The following configurations are persistent and do not need to be redone when Cursor is restarted:
1. SSH Configuration
   - SSH keys and config files
   - VM port forwarding settings
2. Docker Configuration
   - Container network settings
   - Port mappings
3. Application Properties
   - MongoDB connection string
   - Server port
   - Log levels

## Environment Details
- OS: Windows 11
- Java Version: 21.0.2+13
- Maven Version: 3.9.6
- Project Path: C:\projects\spring upgrade

## Virtual Machine Configuration
- VM Name: godzilla-VirtualBox
- SSH Port: 2222
- SSH User: godzilla
- SSH Key Path: ~/.ssh/ubuntu_vm_key
- Docker Network: godzilla_app-network

## Docker Containers
- MongoDB:
  - Container Name: mongodb
  - IP: 172.18.0.3
  - Port: 27017
  - Network: godzilla_app-network
- Elasticsearch:
  - Container Name: elasticsearch
  - IP: 172.18.0.2
  - Port: 9200
  - Network: godzilla_app-network

## Command Patterns

### Local PowerShell Commands
```powershell
# Set Java Home (TEMPORARY - Needs to be set each session)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.2+13'

# Run Maven
mvn spring-boot:run

# Test Network Connection
Test-NetConnection -ComputerName localhost -Port 27017
```

### VM SSH Commands
```bash
# Basic SSH Connection (PERSISTENT - Uses existing SSH config)
ssh ubuntu-vm
# or
ssh -i ~/.ssh/ubuntu_vm_key godzilla@localhost -p 2222

# Docker Commands
docker ps
docker logs mongodb
docker exec mongodb mongosh --eval 'db.runCommand({ping:1})'

# Network Commands
nc -zv 172.18.0.3 27017
```

### Best Practices
1. Keep commands simple and focused
2. Avoid complex piping in PowerShell when dealing with SSH commands
3. Use proper quoting and escaping
4. Break down complex operations into multiple steps
5. Always verify command output before proceeding
6. Use consistent command patterns for similar operations
7. Use persistent configurations where possible (SSH, Docker, Application Properties)
8. Only set temporary configurations when necessary (JAVA_HOME, environment variables)

## Application Configuration
- Server Port: 8081
- MongoDB Connection: mongodb://localhost:27017/demo-product-service
- Log Level: INFO
- Actuator Endpoints: health, info

## Troubleshooting Steps
1. Verify VM is running
2. Check SSH connection
3. Verify Docker containers are running
4. Test network connectivity
5. Check application logs
6. Verify port forwarding in VirtualBox 