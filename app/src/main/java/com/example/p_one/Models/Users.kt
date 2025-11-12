package com.example.p_one.Models

data class Users(
    var uidAuth: String? = null,         // ID de Firebase Auth
    var rol: String? = null,             // Alumno / Profesor / Administrador
    var activo: Boolean = true,

    // Datos comunes
    var nombre: String? = null,
    var apellido: String? = null,
    var correo: String? = null,

    // Datos de alumno
    var idAlumno: String? = null,
    var apodoAlumno: String? = null,
    var edadAlumno: Int? = null,
    var idCurso: String? = null,         // curso del alumno

    // Datos de profesor
    var idProfesor: String? = null,
    var cursosAsignados: List<String>? = null,

    // Datos de administrador
    var idAdmin: String? = null,

    // Roles extra si mantienes tu colección de roles
    var roles: List<String>? = null,
    var nivelAcceso: Int? = 1,

    // Auditoría
    var emailVerificado: Boolean = false,
    var createdAt: Long? = System.currentTimeMillis(),
    var updatedAt: Long? = null
)
