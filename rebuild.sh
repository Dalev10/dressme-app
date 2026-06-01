#!/bin/bash
set -e
echo "=== Limpiando contenedores ==="
docker compose down -v
echo "=== Reconstruyendo y levantando ==="
docker compose up --build -d
echo "=== Esperando a que inicie dressme-database ==="
sleep 30
echo "=== Mostrando logs de dressme-database ==="
docker compose logs dressme-database
