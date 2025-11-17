import com.google.firebase.Timestamp

data class MathQuizResultado(
    var uidAuth: String? = null,
    var apodo: String? = null,
    var correctas: Int? = null,
    var incorrectas: Int? = null,
    var totalPreguntas: Int? = null,
    var porcentaje: Double? = null,
    var fechaUltimoJuego: Timestamp? = null
)
