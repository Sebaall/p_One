package com.example.p_one.Models

data class Administrador(
    var idAdmin: String? = null,
    var nombreAdmin: String? = null,
    var apellidoAdmin: String? = null,
    var correoAdmin: String? = null,
    var activo: Boolean = true
)
