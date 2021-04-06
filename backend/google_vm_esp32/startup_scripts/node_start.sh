#!/usr/bin/bash
apt-get update
sudo apt-get install git nodejs npm nginx certbot python-certbot-nginx ca-certificates build-essential supervisor


cd /opt/
git config --global credential.helper gcloud.sh
sudo gcloud source repos clone github_cheffernan087_smart_latch /opt/app/smart-latch

cd /opt/app/smart-latch
sudo git pull origin main
cd backend/google_vm_esp32/

# nginx config
sudo cp etc/nginx/sites-available/default /etc/nginx/sites-available
sudo systemctl restart nginx

# start node server
sudo npm install
npm run start