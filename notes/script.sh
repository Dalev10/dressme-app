#!/bin/bash

# Carpeta raíz del proyecto
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Archivo de salida
OUTPUT_FILE="$(dirname "$0")/backend-code.txt"

# Limpia el archivo si ya existe
> "$OUTPUT_FILE"

# Módulos Java del proyecto
JAVA_MODULES=(
  "$ROOT_DIR/dressme-back/dressme-back/src/main/java"
  "$ROOT_DIR/dressme-database/dressme-database/src/main/java"
  "$ROOT_DIR/dressme-gateway/dressme-gateway/src/main/java"
)

# Módulos Python del proyecto
PYTHON_MODULES=(
  "$ROOT_DIR/dressme-ai"
)

# ============ PROCESAR MÓDULOS JAVA ============
echo "=== CÓDIGO JAVA ===" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

for module_path in "${JAVA_MODULES[@]}"; do
  if [ -d "$module_path" ]; then
    echo "=== Módulo Java: $module_path ===" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    
    # Busca solo archivos .java en src/main/java, excluyendo:
    # - target/ (compilados)
    # - generated-sources/ (código generado)
    # - .class files (compilados)
    find "$module_path" -type f -name "*.java" \
      ! -path "*/target/*" \
      ! -path "*/generated-*/*" \
      ! -path "*/.git/*" | while read -r filepath; do
      echo "Archivo: $filepath" >> "$OUTPUT_FILE"
      echo "" >> "$OUTPUT_FILE"
      cat "$filepath" >> "$OUTPUT_FILE"
      echo -e "\n\n" >> "$OUTPUT_FILE"
    done
  fi
done

# ============ PROCESAR MÓDULOS PYTHON ============
echo -e "\n\n=== CÓDIGO PYTHON ===" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

for module_path in "${PYTHON_MODULES[@]}"; do
  if [ -d "$module_path" ]; then
    echo "=== Módulo Python: $module_path ===" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    
    # Encuentra archivos .py excluyendo:
    # - __pycache__/ (caché de Python)
    # - .egg-info/ (información de instalación)
    # - dist/ (distribución compilada)
    # - build/ (compilación)
    # - .venv/, venv/, env/ (entornos virtuales)
    # - .git/ (repositorio git)
    find "$module_path" -type f -name "*.py" \
      ! -path "*/__pycache__/*" \
      ! -path "*/.egg-info/*" \
      ! -path "*/dist/*" \
      ! -path "*/build/*" \
      ! -path "*/.venv/*" \
      ! -path "*/venv/*" \
      ! -path "*/env/*" \
      ! -path "*/.git/*" | while read -r filepath; do
      echo "Archivo: $filepath" >> "$OUTPUT_FILE"
      echo "" >> "$OUTPUT_FILE"
      cat "$filepath" >> "$OUTPUT_FILE"
      echo -e "\n\n" >> "$OUTPUT_FILE"
    done
    
    # Incluye archivos de configuración del proyecto (solo en la raíz del módulo)
    for config_file in "requirements.txt" "pyproject.toml" "setup.py" "Dockerfile"; do
      if [ -f "$module_path/$config_file" ]; then
        echo "Archivo: $module_path/$config_file" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        cat "$module_path/$config_file" >> "$OUTPUT_FILE"
        echo -e "\n\n" >> "$OUTPUT_FILE"
      fi
    done
  fi
done

echo "Backend code (Java + Python) extraído correctamente en $OUTPUT_FILE"