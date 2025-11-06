package com.example.p_one.Models

data class Users(
    var idUsuario: String? = null,
    var correo: String? = null,
    var nombre: String? = null,
    var contrasena: String? = null,   // agregado para login simple en Firestore
    var rol: String? = null,          // "alumno" | "profesor" | "admin"
    var idPerfil: String? = null,     // docId en Alumnos/Profesores/Admins
    var activo: Boolean = true,
    var createdAt: Long? = System.currentTimeMillis()
)
