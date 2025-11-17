package com.example.p_one.Models

data class Rol(
    var idRol: String? = null,
    var nombreRol: String? = null,
    var descripcionRol: String? = null,
    var nivelAcceso: Int? = null,
    var permisos: List<String>? = null,
    var fechaCreacion: com.google.firebase.Timestamp? = null,
    var creadoPor: String? = null
)
