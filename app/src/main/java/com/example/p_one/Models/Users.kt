package com.example.p_one.Models

data class Users(
    // Identificación
    var uidAuth: String? = null,
    var rol: String? = null,
    var activo: Boolean = true,
    // Datos comunes
    var nombre: String? = null,
    var apellido: String? = null,
    var correo: String? = null,
    // Datos exclusivos de Alumno
    var idAlumno: String? = null,
    var apodoAlumno: String? = null,
    var edadAlumno: Int? = null,
    var idCurso: String? = null,
    var numAlumno: Long? = null,
    // Datos exclusivos de Profesor
    var idProfesor: String? = null,
    var cursosAsignados: List<String>? = null,
    //  Datos exclusivos de Administrador
    var idAdmin: String? = null,
    // Roles y permisos
    var roles: List<String>? = null,
    var nivelAcceso: Int? = 1,
    // Auditoría
    var emailVerificado: Boolean = false,
    var createdAt: Long? = System.currentTimeMillis(),
    var updatedAt: Long? = null
)
