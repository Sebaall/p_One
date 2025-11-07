package com.example.p_one.Models

data class Profesor(
    var idProfesor: String? = null,
    var nombreProfesor: String? = null,
    var apellidoProfesor: String? = null,
    var correoProfesor: String? = null,
    var cursosAsignados: List<String> = emptyList()
)
