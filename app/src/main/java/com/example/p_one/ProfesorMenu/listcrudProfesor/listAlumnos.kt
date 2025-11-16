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

class listAlumnos : AppCompatActivity() {

    private lateinit var lvListaAlumnos: ListView
    private lateinit var btnVolver: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_alumnos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        lvListaAlumnos = findViewById(R.id.lvListaAlumnos)
        btnVolver = findViewById(R.id.btnVolver)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        // HEADER PERSONALIZADO (COD - NOMBRE - CURSO - LETRA)
        val header = layoutInflater.inflate(R.layout.header_list, lvListaAlumnos, false)
        lvListaAlumnos.addHeaderView(header)

        btnVolver.setOnClickListener { finish() }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val i = Intent(this, Login::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }

        cargarAlumnos()
    }

    private fun cargarAlumnos() {

        db.collection("Cursos")
            .get()
            .addOnSuccessListener { cursosSnap ->

                val mapaNombreCurso = mutableMapOf<String, String>()
                val mapaNivelCurso = mutableMapOf<String, String>()

                for (cursoDoc in cursosSnap.documents) {
                    val idCursoKey = cursoDoc.getString("idCurso") ?: cursoDoc.id
                    val nombreCurso = cursoDoc.getString("nombreCurso") ?: "Sin curso"
                    val nivel = cursoDoc.getString("nivel") ?: "Sin nivel"

                    mapaNombreCurso[idCursoKey] = nombreCurso
                    mapaNivelCurso[idCursoKey] = nivel
                }

                db.collection("users")
                    .whereEqualTo("rol", "Alumno")
                    .get()
                    .addOnSuccessListener { snap ->

                        if (snap.isEmpty) {
                            Toast.makeText(this, "Sin alumnos registrados", Toast.LENGTH_SHORT).show()
                        }

                        val filas = snap.documents.mapIndexed { index, doc ->

                            val nombre = doc.getString("nombre") ?: ""
                            val apellido = doc.getString("apellido") ?: ""
                            val nombreCompleto = "$nombre $apellido"

                            val idCursoAlumno = doc.getString("idCurso") ?: ""

                            val nombreCurso = mapaNombreCurso[idCursoAlumno] ?: "Sin curso"
                            val nivelCurso = mapaNivelCurso[idCursoAlumno] ?: "Sin nivel"

                            mapOf(
                                "posicion" to (index + 1).toString(),
                                "nombreCompleto" to nombreCompleto,
                                "nombreCurso" to nombreCurso,
                                "nivelCurso" to nivelCurso
                            )
                        }

                        val adapter = object : ArrayAdapter<Map<String, String>>(
                            this,
                            0,
                            filas
                        ) {
                            override fun getView(
                                position: Int,
                                convertView: View?,
                                parent: ViewGroup
                            ): View {
                                val v = convertView ?: layoutInflater.inflate(
                                    R.layout.item_ranking,
                                    parent,
                                    false
                                )

                                val item = getItem(position)!!

                                v.findViewById<TextView>(R.id.tvPosicion).text = item["posicion"]
                                v.findViewById<TextView>(R.id.tvApodo).text = item["nombreCompleto"]
                                v.findViewById<TextView>(R.id.tvPorcentaje).text = item["nombreCurso"]
                                v.findViewById<TextView>(R.id.tvCorrectas).text = item["nivelCurso"]

                                return v
                            }
                        }

                        lvListaAlumnos.adapter = adapter
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando cursos: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
