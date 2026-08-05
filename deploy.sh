#!/bin/bash
set -e

echo "=========================================="
echo "   Radion Production Deployment Script    "
echo "=========================================="

# 1. Update and install dependencies
echo "=> Installing Docker and Git..."
sudo apt-get update -y
sudo apt-get install -y git ca-certificates curl gnupg

# 2. Install Docker if not present
if ! command -v docker &> /dev/null; then
    echo "=> Installing Docker Engine..."
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker $USER
    echo "=> Docker installed. Note: You may need to log out and log back in for 'docker' group to apply."
else
    echo "=> Docker is already installed."
fi

# 3. Setup Project
if [ ! -d "Radion" ]; then
    echo "=> Cloning repository..."
    git clone https://github.com/ayaan-cse/Radion.git
    cd Radion
else
    echo "=> Repository found, pulling latest changes..."
    cd Radion
    git pull origin main
fi

# 4. Check for .env file
if [ ! -f ".env" ]; then
    echo "=========================================="
    echo " CRITICAL: Missing .env file!"
    echo "=========================================="
    echo "Please copy .env.example to .env and fill in all your secrets:"
    echo "  cp .env.example .env"
    echo "  nano .env"
    echo "Then re-run this script."
    exit 1
fi

# 5. Build and deploy
echo "=> Building and starting services..."
sudo docker compose up -d --build

echo "=========================================="
echo " Deployment successful!                   "
echo " Caddy is acquiring SSL certificates in the background."
echo " Note: Make sure ports 80 and 443 are open in your AWS Security Group."
echo "=========================================="
