#!/bin/bash
# Nexis Agent start script
INSTALL_DIR="/opt/nexis-agent"
exec java -jar "$INSTALL_DIR/Nexis_Agent.jar" "$@"