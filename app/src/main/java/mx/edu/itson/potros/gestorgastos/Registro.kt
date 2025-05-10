package mx.edu.itson.potros.gestorgastos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Registro : AppCompatActivity() {
    private val userRef = FirebaseDatabase.getInstance().getReference("Usuarios")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val nombre: EditText = findViewById(R.id.et_nombre)
        val correo: EditText = findViewById(R.id.et_correo)
        val contraseña: EditText = findViewById(R.id.et_contraseña)
        val confirmarContraseña: EditText = findViewById(R.id.et_confirmar_contraseña)
        val error: TextView = findViewById(R.id.tv_error)
        val btnRegistrate: Button = findViewById(R.id.btn_registrate)
        val btnVolver: Button = findViewById(R.id.btn_volver)
        error.visibility = View.INVISIBLE

        btnRegistrate.setOnClickListener {
            if (correo.text.isEmpty() || contraseña.text.isEmpty() || confirmarContraseña.text.isEmpty()) {
                error.text = "Todos los campos deben de ser llenados"
                error.visibility = View.VISIBLE
            } else if (!contraseña.text.toString().equals(confirmarContraseña.text.toString())) {
                error.text = "Las contraseñas no coinciden"
                error.visibility = View.VISIBLE
            } else {
                error.visibility = View.INVISIBLE
                signIn(nombre.text.toString(), correo.text.toString(), contraseña.text.toString())
            }
        }

        btnVolver.setOnClickListener {
            val intent = Intent(this, IniciarSesion::class.java)
            startActivity(intent)
        }
    }

    private fun signIn(nombre: String, correo: String, contraseña: String) {
        val error: TextView = findViewById(R.id.tv_error)

        userRef.orderByChild("correo").equalTo(correo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        error.text = "Ya existe una cuenta con ese correo"
                        error.visibility = View.VISIBLE
                    } else {
                        val usuario = Usuario(nombre, correo, contraseña)
                        userRef.push().setValue(usuario)
                            .addOnSuccessListener {
                                val intent = Intent(this@Registro, IniciarSesion::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                error.text = "Error al registrar el usuario"
                                error.visibility = View.VISIBLE
                            }
                    }
                }

                override fun onCancelled(errorDB: DatabaseError) {
                    error.text = "Error al verificar el correo"
                    error.visibility = View.VISIBLE
                }
            })
    }
}