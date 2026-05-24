# ─── Etapa única: imagen de producción ───────────────────────────────────────
#
# Usamos python:3.11-slim porque:
#   - slim elimina herramientas de compilación innecesarias (~200MB menos)
#   - 3.11 es la versión mínima requerida por pydantic-settings 2.x y
#     la sintaxis list[float] en type hints que usamos en los schemas
#
FROM python:3.11-slim

# ── Variables de construcción ─────────────────────────────────────────────────
# PYTHONDONTWRITEBYTECODE: evita crear archivos .pyc innecesarios en el contenedor
# PYTHONUNBUFFERED:        fuerza que los logs de Python salgan en tiempo real
#                          en lugar de quedar en buffer — imprescindible para
#                          ver los logs de Uvicorn en docker compose logs
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app  

# ── Directorio de trabajo ─────────────────────────────────────────────────────
WORKDIR /app

# ── Dependencias del sistema ──────────────────────────────────────────────────
# libpq-dev: requerida por psycopg2 para conectarse a PostgreSQL
# gcc:       compilador necesario para algunas dependencias de numpy en slim
# Las eliminamos después de instalar las dependencias Python para reducir
# el tamaño final de la imagen (patrón common en imágenes de producción)
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libpq-dev \
        gcc \
    && rm -rf /var/lib/apt/lists/*

# ── Dependencias Python ───────────────────────────────────────────────────────
# Copiamos requirements.txt ANTES que el resto del código.
# Docker cachea las capas en orden: si el código cambia pero requirements.txt
# no cambia, Docker reutiliza la capa de pip install sin reinstalar todo.
# Esto hace los rebuilds durante desarrollo mucho más rápidos.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ── Código fuente ─────────────────────────────────────────────────────────────
COPY . .

# ── Puerto expuesto ───────────────────────────────────────────────────────────
# Documenta que el contenedor escucha en 8000.
# El docker-compose.yml mapea este puerto al 8083 del host.
EXPOSE 8000

# ── Comando de arranque ───────────────────────────────────────────────────────
# --host 0.0.0.0: escucha en todas las interfaces de red del contenedor,
#                 no solo en localhost. Sin esto, el servicio no es accesible
#                 desde otros contenedores en la red de Docker.
# --port 8000:    explícito para que coincida con el EXPOSE y el compose.
# Sin --reload:   el reload automático es solo para desarrollo local.
#                 En Docker el contenedor entero se reconstruye cuando hay cambios.
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]