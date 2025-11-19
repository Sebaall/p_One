# Númerix

P-One es una aplicación educativa para estudiantes de educación básica. Su objetivo principal es reforzar las matemáticas mediante ejercicios simples, retroalimentación inmediata y un sistema de progreso accesible tanto para alumnos como para profesores y administradores.

---

## Descripción general

La app permite que los estudiantes practiquen operaciones matemáticas básicas a través del módulo MathQuiz.  
Los profesores pueden revisar resultados y administrar cursos, mientras que los administradores gestionan usuarios, roles y la estructura completa del sistema.

---

## Características

- Módulo MathQuiz con sumas, restas, multiplicaciones y divisiones.  
- Resultados guardados automáticamente en Firebase.  
- Ranking de desempeño por curso.  
- Inicio de sesión y registro con validaciones.  
- Menús diferenciados para Alumno, Profesor y Administrador.  
- CRUD completo para gestión de usuarios, cursos y roles.  
- Recuperación de contraseña y verificación de cuenta.  
- Interfaz clara y amigable para estudiantes.

---

## Tecnologías utilizadas

- Kotlin (Android)  
- Firebase Authentication  
- Firebase Firestore  
- Material Design  
- Android Studio
- Render

---

## Estructura del proyecto

- `models/` – Data classes para usuarios, cursos, roles y resultados  
- `layouts/` – Pantallas XML  
- `activities/` – Lógica principal de la app  
- `firebase/` – Conexiones con Firestore y Auth  

---

## Base de datos

Colecciones principales:

- `users`  
- `cursos`  
- `roles`  
- `mathQuizResultados`  
- `puntuaciones`  

---

## Roles del sistema

**Alumno:** realiza ejercicios, recibe resultados y revisa su ranking.  
**Profesor:** administra cursos y supervisa el avance de los alumnos.  
**Administrador:** gestiona usuarios, roles y estructura general del sistema.

---

## Instalación

### Opción 1: Ejecutar el proyecto desde Android Studio
1. Clonar este repositorio.  
2. Abrir el proyecto en Android Studio.  
3. Agregar el archivo `google-services.json` de Firebase en `app/`.  
4. Ejecutar en un dispositivo físico o emulador.

### Opción 2: Instalar el APK
Puedes descargar el archivo APK compilado y ejecutarlo directamente en tu dispositivo Android.  
Solo instala el archivo y podrás usar la aplicación sin necesidad de Android Studio.

---

## Autor

Desarrollado por Seba.
