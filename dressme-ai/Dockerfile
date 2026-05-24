# 1. Usamos una imagen ligera de Python
FROM python:3.11-slim

# 2. Establecemos el directorio de trabajo dentro del contenedor
WORKDIR /app

# 3. Copiamos el archivo de dependencias
COPY requirements.txt .

# 4. Instalamos las librerías
RUN pip install --no-cache-dir -r requirements.txt

# 5. Copiamos el resto del código de la carpeta local al contenedor
COPY . .

# 6. Exponemos el puerto que usará FastAPI
EXPOSE 8080

# 7. Comando para arrancar la app usando Uvicorn
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]
