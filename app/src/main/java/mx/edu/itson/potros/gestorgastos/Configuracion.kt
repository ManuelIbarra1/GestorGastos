package mx.edu.itson.potros.gestorgastos

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class Configuracion : AppCompatActivity() {

    private var userKey: String? = null
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        // UI references
        val etNombre = findViewById<EditText>(R.id.et_nombre_config)
        val etCorreo = findViewById<EditText>(R.id.et_correo_config)
        val etContraActual = findViewById<EditText>(R.id.et_contrasena_actual_config)
        val etNuevaContra = findViewById<EditText>(R.id.et_nueva_contrasena_config)
        val etConfirmar = findViewById<EditText>(R.id.et_confirmar_nueva_contrasena_config)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar_config)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar_config)
        val btnCerrarSesion = findViewById<Button>(R.id.btn_cerrar_sesion)

        // Obtener datos del Intent
        val nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val correoUsuario = intent.getStringExtra("correo_usuario") ?: ""

        // Mostrar en los campos
        etNombre.setText(nombreUsuario)
        etCorreo.setText(correoUsuario)

        userRef = FirebaseDatabase.getInstance().getReference("Usuarios")

        btnGuardar.setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            val nuevoCorreo = etCorreo.text.toString().trim()
            val contraActual = etContraActual.text.toString().trim()
            val nuevaContra = etNuevaContra.text.toString()
            val confirmarContra = etConfirmar.text.toString()

            if (nuevoNombre.isEmpty() || nuevoCorreo.isEmpty()) {
                Toast.makeText(this, "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contraActual.isEmpty()) {
                Toast.makeText(this, "Debes ingresar tu contraseña actual", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Buscar al usuario por correo y validar contraseña
            userRef.orderByChild("correo").equalTo(correoUsuario)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(this@Configuracion, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                            return
                        }

                        for (userSnap in snapshot.children) {
                            val usuario = userSnap.getValue(Usuario::class.java)
                            userKey = userSnap.key

                            if (usuario?.contraseña != contraActual) {
                                Toast.makeText(this@Configuracion, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                                return
                            }

                            val contraseñaFinal = if (nuevaContra.isNotEmpty() || confirmarContra.isNotEmpty()) {
                                if (nuevaContra != confirmarContra) {
                                    Toast.makeText(this@Configuracion, "Las nuevas contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                nuevaContra
                            } else {
                                usuario.contraseña ?: ""
                            }

                            val usuarioActualizado = Usuario(
                                nombre = nuevoNombre,
                                correo = nuevoCorreo,
                                contraseña = contraseñaFinal
                            )

                            userKey?.let {
                                userRef.child(it).setValue(usuarioActualizado)
                                    .addOnSuccessListener {
                                        val resultIntent = Intent()
                                        resultIntent.putExtra("nombre_usuario_actualizado", nuevoNombre)
                                        resultIntent.putExtra("correo_usuario_actualizado", nuevoCorreo)
                                        setResult(RESULT_OK, resultIntent)
                                        Toast.makeText(this@Configuracion, "Datos actualizados", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }

                            }

                            break
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@Configuracion, "Error en la base de datos", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, IniciarSesion::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
