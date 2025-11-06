package com.example.p_one.Models

data class Users(
    var idUsuario: String? = null,
    var correo: String? = null,
    var nombreusuario: String? = null,
    var rol: String? = null,
    var idPerfil: String? = null,
    var activo: Boolean = true,
    var emailVerificado: Boolean = false,
    var createdAt: Long? = System.currentTimeMillis()
)
