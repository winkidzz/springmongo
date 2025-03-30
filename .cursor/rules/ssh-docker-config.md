# SSH and Docker Configuration Rules

## SSH Configuration

### Local Windows Setup
1. SSH Config Location: `C:\Users\<username>\.ssh\config` (PERSISTENT - Only needs to be set up once)
2. SSH Key Location: `C:\Users\<username>\.ssh\ubuntu_vm_key` (PERSISTENT - Only needs to be set up once)
3. SSH Config Content:
```config
Host ubuntu-vm
    HostName 192.168.1.198
    Port 22
    IdentityFile ~/.ssh/ubuntu_vm_key
    StrictHostKeyChecking no
```

> **Note**: The SSH configuration is persistent and does not need to be recreated when Cursor is restarted. These settings are stored in your local SSH config file and will be available for all future sessions.

### Ubuntu VM Setup
1. SSH Key Location: `~/.ssh/authorized_keys` (PERSISTENT - Only needs to be set up once)
2. Docker Container Ports:
   - MongoDB: 27017
   - Elasticsearch: 9200

## Docker Configuration

### Container Setup
1. MongoDB Container:
   - Port: 27017
   - Database: demo-product-service
   - Connection String: `mongodb://localhost:27017/demo-product-service`

2. Elasticsearch Container:
   - Port: 9200
   - Default Settings

### Application Configuration
1. MongoDB Connection Settings:
```properties
spring.data.mongodb.host=192.168.1.198
spring.data.mongodb.port=27017
spring.data.mongodb.database=demo-product-service
```

## Connection Steps
1. Start Ubuntu VM in VirtualBox
2. Find the VM's IP address (bridged network):
   ```bash
   # On the Ubuntu VM
   ip addr show
   # Look for inet 192.168.1.xxx
   ```
3. SSH into VM:
   ```bash
   # Using the bridged IP
   ssh -i ~/.ssh/ubuntu_vm_key godzilla@192.168.1.198
   ```
4. Start Docker containers:
   ```bash
   docker-compose up -d
   ```
5. Verify containers:
   ```bash
   docker ps
   ```

## Troubleshooting
1. If SSH connection fails:
   - Check VM is running
   - Verify port forwarding (2222) in VirtualBox
   - Check SSH key permissions
2. If Docker containers fail:
   - Check Docker service status
   - Verify port conflicts
   - Check container logs 