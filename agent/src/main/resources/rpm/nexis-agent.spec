Name:           nexis-agent
Version:        1.0
Release:        1
Summary:        Nexis Agent - System Metrics Collector
License:        Proprietary
BuildArch:      noarch

%description
Nexis Agent collects system metrics and sends them to Nexis Server.

%install
mkdir -p %{buildroot}/opt/nexis-agent
mkdir -p %{buildroot}/etc/systemd/system
mkdir -p %{buildroot}/var/log/nexis-agent

install -m 0755 %{_sourcedir}/Nexis_Agent.jar        %{buildroot}/opt/nexis-agent/Nexis_Agent.jar
install -m 0755 %{_sourcedir}/nexis-agent.sh          %{buildroot}/opt/nexis-agent/nexis-agent.sh
install -m 0644 %{_sourcedir}/agent.properties        %{buildroot}/opt/nexis-agent/agent.properties
install -m 0644 %{_sourcedir}/nexis-agent.service     %{buildroot}/etc/systemd/system/nexis-agent.service

%files
%dir /opt/nexis-agent
%dir /var/log/nexis-agent
/opt/nexis-agent/Nexis_Agent.jar
/opt/nexis-agent/nexis-agent.sh
%config(noreplace) /opt/nexis-agent/agent.properties
/etc/systemd/system/nexis-agent.service

%pre
echo "[nexis-agent] Pre-install starting..."
if ! command -v java &> /dev/null; then
    echo "[nexis-agent] ERROR: Java is not installed. Please install Java 21 or higher."
    exit 1
fi
echo "[nexis-agent] Java check passed."

%post
PROP=/opt/nexis-agent/agent.properties
CONFIGURED=0

cancel_install() {
    stty echo 2>/dev/null
    echo ""
    echo "[nexis-agent] Installation cancelled."
    exit 0
}

trap cancel_install INT TERM

if [ -e /dev/tty ]; then

    echo "========================================"
    echo "  Nexis Agent Configuration"
    echo "  (Press Ctrl+C to cancel at any time)"
    echo "========================================"

    echo ""
    printf "Nexis Server IP: "
    read SERVER_HOST < /dev/tty || cancel_install
    while [ -z "$SERVER_HOST" ]; do
        echo "Server IP cannot be empty."
        printf "Nexis Server IP: "
        read SERVER_HOST < /dev/tty || cancel_install
    done

    printf "Nexis Server Port [9000]: "
    read SERVER_PORT < /dev/tty || cancel_install
    SERVER_PORT=${SERVER_PORT:-9000}

    printf "Log directory [/var/log/nexis-agent]: "
    read LOG_DIR < /dev/tty || cancel_install
    LOG_DIR=${LOG_DIR:-/var/log/nexis-agent}

    echo ""
    echo "========================================"
    echo "  Configuration Summary"
    echo "========================================"
    echo "  Server IP   : $SERVER_HOST"
    echo "  Server Port : $SERVER_PORT"
    echo "  Log Dir     : $LOG_DIR"
    echo "========================================"
    printf "Apply this configuration? [Y/n]: "
    read CONFIRM < /dev/tty || cancel_install
    if [ "$CONFIRM" = "n" ] || [ "$CONFIRM" = "N" ]; then
        cancel_install
    fi

    sed -i "s|^server.host=.*|server.host=$SERVER_HOST|" $PROP
    sed -i "s|^server.port=.*|server.port=$SERVER_PORT|" $PROP
    sed -i "s|^log.dir=.*|log.dir=$LOG_DIR|"            $PROP

    CONFIGURED=1

else
    echo "[nexis-agent] No interactive terminal detected."
    echo "[nexis-agent] Installed with default configuration."
    echo "[nexis-agent] Please edit /opt/nexis-agent/agent.properties before starting."
    CONFIGURED=1
fi

trap - INT TERM

if [ $CONFIGURED -eq 1 ]; then
    LOG_DIR=$(grep "^log.dir=" $PROP | cut -d'=' -f2)
    LOG_DIR=${LOG_DIR:-/var/log/nexis-agent}
    mkdir -p $LOG_DIR
    chmod 755 $LOG_DIR
    echo "[nexis-agent] Log directory ready: $LOG_DIR"

    systemctl daemon-reload
    systemctl enable nexis-agent

    echo ""
    echo "[nexis-agent] Configuration applied successfully."
    echo "[nexis-agent] Review config : vi /opt/nexis-agent/agent.properties"
    echo "[nexis-agent] To start      : systemctl start nexis-agent"
    echo "[nexis-agent] To check      : systemctl status nexis-agent"
    echo "[nexis-agent] To view logs  : tail -f $LOG_DIR/nexis-agent.log"
    echo "========================================"
fi

%preun
if [ $1 -eq 0 ]; then
    systemctl stop nexis-agent 2>/dev/null || true
    systemctl disable nexis-agent 2>/dev/null || true
    systemctl daemon-reload
fi

%postun
# nothing

%changelog
* Thu Jan 01 2026 Nexis <nexis@example.com> - 1.0-1
- Initial release