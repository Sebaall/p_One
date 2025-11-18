P-One – Plataforma educativa para estudiantes de educación básica
P-One es una aplicación pensada para ayudar a niños y niñas de educación básica a reforzar matemáticas de forma clara, entretenida y guiada.
El sistema reúne actividades, seguimiento, ranking, cursos, profesores y administración en un solo lugar, manteniendo todo ordenado y fácil de usar.
Objetivo de la aplicación
Entregar una herramienta educativa donde los estudiantes practiquen matemáticas y puedan ver su progreso, mientras que profesores y administradores gestionan usuarios, cursos y resultados sin complicaciones.
Roles dentro de la app
Alumno
Inicia sesión con su cuenta.
Ve un menú diseñado especialmente para él.
Realiza ejercicios del módulo MathQuiz (sumas, restas, multiplicaciones y divisiones).
Responde mediante alternativas.
Obtiene un resumen al finalizar: correctas, incorrectas y porcentaje general.
Sus resultados se guardan automáticamente en Firestore.
Puede ver un ranking donde compara su rendimiento con otros compañeros del curso.
Profesor
Puede visualizar la lista de alumnos.
Gestiona los cursos que tiene asignados.
Revisa los resultados de cada estudiante.
Accede a un menú con más funciones de organización.
Administrador
Administra toda la estructura del sistema.
Puede crear, editar y eliminar usuarios.
Gestiona roles, profesores y cursos.
Mantiene ordenados los datos de la plataforma.
MathQuiz
El módulo MathQuiz es el corazón de la app. Está diseñado para estudiantes de educación básica:
Generación aleatoria de operaciones.
Cuatro alternativas de respuesta.
Retroalimentación inmediata.
Pantalla de resultados al terminar.
Registro automático en la base de datos.
Ranking que muestra el desempeño general de los alumnos.
Estructura de base de datos
Toda la información se guarda en Firebase Firestore usando una estructura clara:
Colecciones principales:
users
cursos
roles
mathQuizResultados
contadores y puntuaciones de apoyo
El documento de cada usuario utiliza campos como:
uidAuth
rol
nombre
apellido
correo
apodoAlumno
idCurso
cursosAsignados
nivelAcceso
emailVerificado
createdAt y updatedAt
La estructura está diseñada para ser rápida, ordenada y fácil de escalar.
Características principales
Inicio de sesión y registro.
Recuperación de contraseña.
Validaciones seguras.
Menús diferenciados según rol (Alumno, Profesor, Administrador).
Diseño claro y amigable para niños.
Módulo MathQuiz con resultados y retroalimentación.
Ranking general de alumnos.
CRUD completo para administradores.
Gestión de cursos y profesores.
Firebase Auth para usuarios.
Firestore para datos principales.
Diseño con Material Design 3.
Tecnologías utilizadas
Kotlin (Android)
Android Studio
Firebase Authentication
Firebase Firestore
Material Design
ConstraintLayout y ScrollView para la estructura visual
Valor del proyecto
P-One busca que aprender matemáticas sea más accesible y motivante para los estudiantes, mientras profesores y administradores cuentan con un sistema centralizado que facilita su trabajo diario. Es una plataforma que combina educación y tecnología en un formato simple, ordenado y funcional.
