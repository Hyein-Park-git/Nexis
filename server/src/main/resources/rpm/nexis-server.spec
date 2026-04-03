Name:           nexis-server
Version:        1.0
Release:        1
Summary:        Nexis Server - System Metrics Collection Server
License:        Proprietary
BuildArch:      noarch

%description
Nexis Server receives system metrics from Nexis Agents and stores them in a database.

%install
mkdir -p %{buildroot}/opt/nexis-server
mkdir -p %{buildroot}/etc/systemd/system
mkdir -p %{buildroot}/var/log/nexis-server

install -m 0755 %{_sourcedir}/Nexis_Server.jar        %{buildroot}/opt/nexis-server/Nexis_Server.jar
install -m 0755 %{_sourcedir}/nexis-server.sh          %{buildroot}/opt/nexis-server/nexis-server.sh
install -m 0644 %{_sourcedir}/server.properties        %{buildroot}/opt/nexis-server/server.properties
install -m 0644 %{_sourcedir}/nexis-server.service     %{buildroot}/etc/systemd/system/nexis-server.service

%files
%dir /opt/nexis-server
%dir /var/log/nexis-server
/opt/nexis-server/Nexis_Server.jar
/opt/nexis-server/nexis-server.sh
%config(noreplace) /opt/nexis-server/server.properties
/etc/systemd/system/nexis-server.service

%pre
echo "[nexis-server] Pre-install starting..."
if ! command -v java &> /dev/null; then
    echo "[nexis-server] ERROR: Java is not installed. Please install Java 21 or higher."
    exit 1
fi
echo "[nexis-server] Java check passed."

%post
PROP=/opt/nexis-server/server.properties
YML=/opt/nexis-server/application.yml
CONFIGURED=0

cancel_install() {
    stty echo 2>/dev/null
    echo ""
    echo "[nexis-server] Installation cancelled."
    exit 0
}

trap cancel_install INT TERM

if [ -e /dev/tty ]; then
    exec < /dev/tty

    echo "========================================"
    echo "  Nexis Server Configuration"
    echo "  (Press Ctrl+C to cancel at any time)"
    echo "========================================"

    echo ""
    printf "Server Host [0.0.0.0]: "
    read SERVER_HOST < /dev/tty || cancel_install
    SERVER_HOST=${SERVER_HOST:-0.0.0.0}

    printf "Server TCP Port [9000]: "
    read SERVER_PORT < /dev/tty || cancel_install
    SERVER_PORT=${SERVER_PORT:-9000}

    echo ""
    echo "Select DB type:"
    echo "  1) MySQL"
    echo "  2) MariaDB"
    echo "  3) PostgreSQL"
    printf "Enter choice [1-3] (default: 1): "
    read DB_TYPE_NUM < /dev/tty || cancel_install
    case $DB_TYPE_NUM in
        2) DB_TYPE="mariadb"    ; DEFAULT_PORT="3306" ;;
        3) DB_TYPE="postgresql" ; DEFAULT_PORT="5432" ;;
        *) DB_TYPE="mysql"      ; DEFAULT_PORT="3306" ;;
    esac

    printf "DB Host [127.0.0.1]: "
    read DB_HOST < /dev/tty || cancel_install
    DB_HOST=${DB_HOST:-127.0.0.1}

    printf "DB Port [$DEFAULT_PORT]: "
    read DB_PORT < /dev/tty || cancel_install
    DB_PORT=${DB_PORT:-$DEFAULT_PORT}

    printf "DB Name [nexis]: "
    read DB_NAME < /dev/tty || cancel_install
    DB_NAME=${DB_NAME:-nexis}

    printf "DB Username: "
    read DB_USER < /dev/tty || cancel_install
    if [ -z "$DB_USER" ]; then
        echo "[nexis-server] WARNING: DB Username is empty. Please edit server.properties before starting."
    fi

    printf "DB Password: "
    stty -echo 2>/dev/null
    read DB_PASS < /dev/tty || { stty echo 2>/dev/null; cancel_install; }
    stty echo 2>/dev/null
    echo ""

    printf "Log directory [/var/log/nexis-server/logs]: "
    read LOG_DIR < /dev/tty || cancel_install
    LOG_DIR=${LOG_DIR:-/var/log/nexis-server/logs}

    echo ""
    echo "========================================"
    echo "  Configuration Summary"
    echo "========================================"
    echo "  Server Host : $SERVER_HOST"
    echo "  Server Port : $SERVER_PORT"
    echo "  DB Type     : $DB_TYPE"
    echo "  DB Host     : $DB_HOST"
    echo "  DB Port     : $DB_PORT"
    echo "  DB Name     : $DB_NAME"
    echo "  DB Username : $DB_USER"
    echo "  DB Password : ********"
    echo "  Log Dir     : $LOG_DIR"
    echo "========================================"
    printf "Apply this configuration? [Y/n]: "
    read CONFIRM < /dev/tty || cancel_install
    if [ "$CONFIRM" = "n" ] || [ "$CONFIRM" = "N" ]; then
        cancel_install
    fi

    sed -i "s|^server.host=.*|server.host=$SERVER_HOST|" $PROP
    sed -i "s|^server.port=.*|server.port=$SERVER_PORT|" $PROP
    sed -i "s|^db.type=.*|db.type=$DB_TYPE|"            $PROP
    sed -i "s|^db.host=.*|db.host=$DB_HOST|"            $PROP
    sed -i "s|^db.port=.*|db.port=$DB_PORT|"            $PROP
    sed -i "s|^db.name=.*|db.name=$DB_NAME|"            $PROP
    sed -i "s|^db.username=.*|db.username=$DB_USER|"    $PROP
    sed -i "s|^db.password=.*|db.password=$DB_PASS|"    $PROP
    sed -i "s|^log.dir=.*|log.dir=$LOG_DIR|"            $PROP

    CONFIGURED=1

else
    echo "[nexis-server] No interactive terminal detected."
    echo "[nexis-server] Installed with default configuration."
    echo "[nexis-server] Please edit /opt/nexis-server/server.properties before starting."
    CONFIGURED=1
fi

trap - INT TERM

if [ $CONFIGURED -eq 1 ]; then
    cat > $YML << 'EOF'
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
EOF
    chmod 644 $YML
    echo "[nexis-server] application.yml created."

    LOG_DIR=$(grep "^log.dir=" $PROP | cut -d'=' -f2)
    LOG_DIR=${LOG_DIR:-/var/log/nexis-server/logs}
    mkdir -p $LOG_DIR
    chmod 755 $LOG_DIR
    echo "[nexis-server] Log directory ready: $LOG_DIR"

    systemctl daemon-reload
    systemctl enable nexis-server

    echo ""
    echo "[nexis-server] Configuration applied successfully."
    echo "[nexis-server] Review config : vi /opt/nexis-server/server.properties"
    echo "[nexis-server] To start      : systemctl start nexis-server"
    echo "[nexis-server] To check      : systemctl status nexis-server"
    echo "[nexis-server] To view logs  : tail -f $LOG_DIR/nexis-server.log"
    echo "========================================"
fi

%preun
if [ $1 -eq 0 ]; then
    systemctl stop nexis-server 2>/dev/null || true
    systemctl disable nexis-server 2>/dev/null || true
    systemctl daemon-reload
fi

%postun
# nothing

%changelog
* Thu Jan 01 2026 Nexis <nexis@example.com> - 1.0-1
- Initial release