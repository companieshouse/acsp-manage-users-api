#!/bin/bash
#
# Start script for accounts-association-service

PORT=8080

exec java -jar -Dserver.port="${PORT}" "acsp-manage-users-api.jar"