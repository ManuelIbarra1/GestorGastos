package mx.edu.itson.potros.gestorgastos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class IniciarSesion : AppCompatActivity() {
    private val userRef = FirebaseDatabase.getInstance().getReference("Usuarios")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciar_sesion)

        val correo: EditText = findViewById(R.id.et_correo)
        val contraseña: EditText = findViewById(R.id.et_contraseña)
        val error: TextView = findViewById(R.id.tv_error)
        val btnIniciarSesion: Button = findViewById(R.id.btn_iniciar_sesion)
        val btnRegistrate: Button = findViewById(R.id.btn_registrate)
        error.visibility = View.INVISIBLE

        btnIniciarSesion.setOnClickListener {
            if (correo.text.isEmpty() || contraseña.text.isEmpty()) {
                error.text = "Todos los campos deben de ser llenados"
                error.visibility = View.VISIBLE
            } else {
                error.visibility = View.INVISIBLE
                login(correo.text.toString(), contraseña.text.toString(), error)
            }
        }

        btnRegistrate.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }
    }

    private fun login(correo: String, contraseña: String, errorTextView: TextView) {
        userRef.orderByChild("correo").equalTo(correo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var usuarioLogueado: Usuario? = null
                        for (userSnapshot in snapshot.children) {
                            val usuario = userSnapshot.getValue(Usuario::class.java)
                            if (usuario != null && usuario.contraseña == contraseña) {
                                usuarioLogueado = usuario
                                break
                            }
                        }
                        if (usuarioLogueado != null) {
                            val intent = Intent(this@IniciarSesion, Principal::class.java)
                            intent.putExtra("nombre_usuario", usuarioLogueado.nombre)
                            startActivity(intent)
                            finish()
                        } else {
                            errorTextView.text = "Contraseña incorrecta"
                            errorTextView.visibility = View.VISIBLE
                        }
                    } else {
                        errorTextView.text = "Correo no registrado"
                        errorTextView.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    errorTextView.text = "Error en la base de datos"
                    errorTextView.visibility = View.VISIBLE
                }
            })
    }
}