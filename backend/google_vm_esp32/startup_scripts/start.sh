#! /bin/bash

curl -X GET -o start.sh https://storage.googleapis.com/smart-latch.appspot.com/node_start.sh
sudo chmod +x start.sh
sudo echo "Y Y" | ./start.sh