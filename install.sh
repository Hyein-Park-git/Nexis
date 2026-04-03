#!/bin/bash

SERVICE_NAME=nexis-agent
INSTALL_DIR=/opt/nexis-agent
JAR_NAME=agent-1.0.jar

echo "===== Nexis Agent Installation Start ====="

# root 권한 체크
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo)"
  exit 1
fi

echo "Creating install directory..."
mkdir -p $INSTALL_DIR
mkdir -p $INSTALL_DIR/logs

echo "Copying files..."
cp $JAR_NAME $INSTALL_DIR/
cp agent.properties $INSTALL_DIR/

echo "Creating systemd service..."

cat <<EOF > /etc/systemd/system/$SERVICE_NAME.service
[Unit]
Description=Nexis Monitoring Agent
After=network.target

[Service]
User=root
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/java -jar $INSTALL_DIR/$JAR_NAME
SuccessExitStatus=143
Restart=always
RestartSec=5
StandardOutput=append:$INSTALL_DIR/logs/agent.log
StandardError=append:$INSTALL_DIR/logs/agent-error.log

[Install]
WantedBy=multi-user.target
EOF

echo "Reloading systemd..."
systemctl daemon-reload

echo "Enabling service..."
systemctl enable $SERVICE_NAME

echo "Starting service..."
systemctl start $SERVICE_NAME

echo "===== Installation Complete ====="
echo "Check status: systemctl status $SERVICE_NAME"