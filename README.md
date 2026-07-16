# Recetario - Instrucciones de instalación y ejecución

## Requisitos

Para ejecutar el proyecto se necesita:

- Android Studio instalado.
- JDK compatible con la versión de Android Studio.
- Conexión a Internet.
- Dispositivo Android físico o emulador.
- Proyecto abierto desde Android Studio.

## Instalación

1. Descargar o descomprimir el proyecto.
2. Abrir Android Studio.
3. Seleccionar la opción **Open**.
4. Buscar la carpeta principal del proyecto y abrirla.
5. Esperar a que Android Studio sincronice las dependencias de Gradle.
6. Verificar que el archivo de configuración de Firebase esté incluido en la carpeta correspondiente del proyecto.
7. Ejecutar la aplicación en un emulador o dispositivo físico.

## Configuración necesaria

La aplicación utiliza servicios remotos para su funcionamiento:

- **Firebase Authentication** para el registro, inicio de sesión y cambio de contraseña.
- **Firebase Firestore** como base de datos remota para usuarios, recetas, ingredientes, pasos y recetas guardadas.
- **Supabase Storage** para almacenar imágenes de perfil y fotos de recetas.

El bucket utilizado en Supabase es:

```text
image
