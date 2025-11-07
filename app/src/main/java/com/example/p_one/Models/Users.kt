package com.example.p_one.Models

import com.google.firebase.Timestamp

data class Users(
    var idUsuario: String? = null,
    var correo: String? = null,
    var nombre: String? = null,
    var apellido: String? = null,

    // 🔹 Ahora roles es una lista (puede haber más de un rol)
    var roles: List<String>? = null,

    // 🔹 nivelAcceso (1 alumno, 2 profesor, 3 admin)
    var nivelAcceso: Int? = 1,

    var idPerfil: String? = null,
    var activo: Boolean = true,
    var emailVerificado: Boolean = false,

    // 🔹 Fecha exacta tipo Timestamp para Firestore
    var createdAt: Long? = System.currentTimeMillis(),
    var updatedAt: Timestamp? = null
)
