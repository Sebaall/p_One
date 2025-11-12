package com.example.p_one.Models

data class Rol(
    var idRol: String? = null,
    var nombreRol: String? = null,
    var descripcionRol: String? = null,
    var nivelAcceso: Int? = null,                    // Para organizar jerarquías si lo necesitas
    var permisos: List<String>? = null,              // Ej: ["menu_admin"]
    var fechaCreacion: com.google.firebase.Timestamp? = null,
    var creadoPor: String? = null
)
