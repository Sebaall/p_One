package com.example.p_one

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.crudAdmin.crudAdministrador
import com.example.p_one.crudAdmin.crudAlumno
import com.example.p_one.crudAdmin.crudCursos
import com.example.p_one.crudAdmin.crudProfesor
import com.example.p_one.crudAdmin.crudRoles

class menuAdmin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_admin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    fun curdAdmin(view: View){
        startActivity(Intent(this, crudAdministrador::class.java))
    }
    fun curdAlumno(view: View){
        startActivity(Intent(this, crudAlumno::class.java))
    }
    fun curdProfesor(view: View){
        startActivity(Intent(this, crudProfesor::class.java))
    }
    fun curdRoles(view: View){
        startActivity(Intent(this, crudRoles::class.java))
    }
    fun curdCursos(view: View){
        startActivity(Intent(this, crudCursos::class.java))
    }
}