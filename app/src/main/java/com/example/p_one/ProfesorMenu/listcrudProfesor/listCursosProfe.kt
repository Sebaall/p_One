package com.example.p_one

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Main.Login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class listCursosProfe : AppCompatActivity() {

    private lateinit var lvListaCursos: ListView
    private lateinit var btnVolver: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_cursos_profe)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        lvListaCursos = findViewById(R.id.lvListaCursos)
        btnVolver = findViewById(R.id.btnVolver)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        // HEADER PERSONALIZADO: C - CURSO - LETRA - PROFESOR
        val header = layoutInflater.inflate(R.layout.header_list_cursos, lvListaCursos, false)
        lvListaCursos.addHeaderView(header)

        btnVolver.setOnClickListener { finish() }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val i = Intent(this, Login::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }

        cargarCursos()
    }

    private fun cargarCursos() {
        db.collection("Cursos")
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    Toast.makeText(this, "Sin cursos registrados", Toast.LENGTH_SHORT).show()
                } else {

                    val filas = mutableListOf<Map<String, String>>()
                    var pendientes = snap.size()

                    snap.documents.forEachIndexed { index, doc ->

                        val idCurso = doc.getString("idCurso") ?: doc.id
                        val nombreCurso = doc.getString("nombreCurso") ?: "Sin nombre"
                        val nivel = doc.getString("nivel") ?: "Sin nivel"
                        val profesorId = doc.getString("profesorId") ?: ""

                        if (profesorId.isNotEmpty()) {

                            db.collection("users").document(profesorId)
                                .get()
                                .addOnSuccessListener { profDoc ->

                                    val nombreProfe =
                                        "${profDoc.getString("nombre") ?: ""} ${profDoc.getString("apellido") ?: ""}".trim()

                                    filas.add(
                                        mapOf(
                                            "posicion" to (index + 1).toString(),
                                            "apodo" to nombreCurso,
                                            "porcentaje" to nivel,
                                            "correctas" to nombreProfe.ifEmpty { "Sin profesor" }
                                        )
                                    )

                                    pendientes--
                                    if (pendientes == 0) {
                                        mostrarLista(filas)
                                    }
                                }

                        } else {
                            filas.add(
                                mapOf(
                                    "posicion" to (index + 1).toString(),
                                    "apodo" to nombreCurso,
                                    "porcentaje" to nivel,
                                    "correctas" to "Sin profesor"
                                )
                            )

                            pendientes--
                            if (pendientes == 0) {
                                mostrarLista(filas)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarLista(filas: List<Map<String, String>>) {

        val adapter = object : ArrayAdapter<Map<String, String>>(
            this,
            R.layout.item_ranking,
            filas
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: layoutInflater.inflate(
                    R.layout.item_ranking,
                    parent,
                    false
                )

                val item = getItem(position)!!

                v.findViewById<TextView>(R.id.tvPosicion).text = item["posicion"]
                v.findViewById<TextView>(R.id.tvApodo).text = item["apodo"]        // Curso
                v.findViewById<TextView>(R.id.tvPorcentaje).text = item["porcentaje"] // Letra / nivel
                v.findViewById<TextView>(R.id.tvCorrectas).text = item["correctas"]   // Profesor

                return v
            }
        }

        lvListaCursos.adapter = adapter
    }
}
