package com.example.p_one.MenuAlumno

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Main.Login
import com.example.p_one.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Ranking : AppCompatActivity() {

    private lateinit var tvTituloRanking: TextView
    private lateinit var lvRanking: ListView
    private lateinit var btnVolverJugar: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var db: FirebaseFirestore

    private var apodoAlumno: String = "Invitado"

    // ------- ALERTA -------
    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }
    // ------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ranking)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        tvTituloRanking = findViewById(R.id.tvTituloRanking)
        lvRanking = findViewById(R.id.lvRanking)
        btnVolverJugar = findViewById(R.id.btnVolverJugar)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        apodoAlumno = intent.getStringExtra("apodoAlumno") ?: "Invitado"

        btnVolverJugar.setOnClickListener {
            val intentQuiz = Intent(this, mathQuiz::class.java)
            intentQuiz.putExtra("apodoAlumno", apodoAlumno)
            intentQuiz.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intentQuiz)
            finish()
        }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intentLogin = Intent(this, Login::class.java)
            intentLogin.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intentLogin)
        }

        cargarRanking()
    }

    private fun cargarRanking() {
        db.collection("mathQuizResultados")
            .orderBy("porcentaje", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarAlerta("Aviso", "Sin resultados aún")
                } else {

                    val filas: List<Map<String, String>> = snap.documents.mapIndexed { index, doc ->
                        val apodo = doc.getString("apodo") ?: "Sin apodo"
                        val porcentajeDouble = doc.getDouble("porcentaje") ?: 0.0
                        val porcentajeTexto = "${porcentajeDouble.toInt()}%"
                        val correctasLong = doc.getLong("correctas") ?: 0L
                        val totalLong = doc.getLong("totalPreguntas") ?: 0L
                        val correctasTexto = "$correctasLong/$totalLong"

                        mapOf(
                            "posicion" to (index + 1).toString(),
                            "apodo" to apodo,
                            "porcentaje" to porcentajeTexto,
                            "correctas" to correctasTexto
                        )
                    }

                    val adapter = object : ArrayAdapter<Map<String, String>>(
                        this,
                        R.layout.item_ranking,
                        filas
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = convertView ?: layoutInflater.inflate(
                                R.layout.item_ranking,
                                parent,
                                false
                            )

                            val item = getItem(position) ?: emptyMap()

                            view.findViewById<TextView>(R.id.tvPosicion).text = item["posicion"].orEmpty()
                            view.findViewById<TextView>(R.id.tvApodo).text = item["apodo"].orEmpty()
                            view.findViewById<TextView>(R.id.tvPorcentaje).text = item["porcentaje"].orEmpty()
                            view.findViewById<TextView>(R.id.tvCorrectas).text = item["correctas"].orEmpty()

                            return view
                        }
                    }

                    lvRanking.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al cargar ranking: ${e.message}")
            }
    }
}
