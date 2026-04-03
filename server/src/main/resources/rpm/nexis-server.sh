#!/bin/bash
# Nexis Server start script
INSTALL_DIR="/opt/nexis-server"
PROP="$INSTALL_DIR/server.properties"

# server.properties 읽기
get_prop() { grep "^$1=" "$PROP" | cut -d'=' -f2; }

DB_TYPE=$(get_prop "db.type")
DB_HOST=$(get_prop "db.host")
DB_PORT=$(get_prop "db.port")
DB_NAME=$(get_prop "db.name")
DB_USER=$(get_prop "db.username")
DB_PASS=$(get_prop "db.password")
DDL_AUTO=$(get_prop "jpa.ddl-auto")

# DB URL 조합
case "$DB_TYPE" in
    postgresql) DRIVER="org.postgresql.Driver"
                URL="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME" ;;
    mariadb)    DRIVER="org.mariadb.jdbc.Driver"
                URL="jdbc:mariadb://$DB_HOST:$DB_PORT/$DB_NAME" ;;
    *)          DRIVER="com.mysql.cj.jdbc.Driver"
                URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME" ;;
esac

exec java -jar "$INSTALL_DIR/Nexis_Server.jar" \
  --spring.datasource.url="$URL" \
  --spring.datasource.username="$DB_USER" \
  --spring.datasource.password="$DB_PASS" \
  --spring.datasource.driver-class-name="$DRIVER" \
  --spring.jpa.hibernate.ddl-auto="$DDL_AUTO" \
  "$@"