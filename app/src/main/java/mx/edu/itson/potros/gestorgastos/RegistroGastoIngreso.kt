package mx.edu.itson.potros.gestorgastos

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RegistroGastoIngreso : AppCompatActivity() {
    private lateinit var nombreUsuario: String
    private val dbRefTransacciones = FirebaseDatabase.getInstance().getReference("Transacciones")
    private val dbRefCategorias = FirebaseDatabase.getInstance().getReference("Categorias")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_gasto_ingreso)

        // Obtener usuario desde Intent
        nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""

        val radioTipo = findViewById<RadioGroup>(R.id.radio_group_tipo_transaccion)
        val radioPago = findViewById<RadioGroup>(R.id.radio_group_tipo_pago)
        val etMonto = findViewById<EditText>(R.id.et_monto)
        val etDescripcion = findViewById<EditText>(R.id.et_descripcion)
        val etCategoria = findViewById<EditText>(R.id.et_categoria)
        val etFecha = findViewById<EditText>(R.id.et_fecha)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar)

        etFecha.setOnClickListener { showDatePickerDialog() }

        btnGuardar.setOnClickListener {
            val tipoSeleccionado = findViewById<RadioButton>(radioTipo.checkedRadioButtonId)?.text.toString()
            val tipoPagoSeleccionado = if (tipoSeleccionado == "Gasto")
                findViewById<RadioButton>(radioPago.checkedRadioButtonId)?.text.toString()
            else null

            val cantidad = etMonto.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val categoria = if (tipoSeleccionado == "Gasto") etCategoria.text.toString().trim() else null
            val fecha = etFecha.text.toString().trim()

            // Validación básica
            if (cantidad.isEmpty() || descripcion.isEmpty() || fecha.isEmpty() ||
                (tipoSeleccionado == "Gasto" && (categoria.isNullOrEmpty() || tipoPagoSeleccionado.isNullOrEmpty()))) {
                Toast.makeText(this, "Por favor llena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar transacción
            val transaccion = Transaccion(
                usuario = nombreUsuario,
                cantidad = cantidad,
                descipcion = descripcion,
                tipo = tipoSeleccionado,
                categoria = categoria,
                tipo_pago = tipoPagoSeleccionado,
                fecha = fecha
            )

            dbRefTransacciones.push().setValue(transaccion)

            // Si es Gasto, guarda la categoría también
            if (!categoria.isNullOrEmpty()) {
                dbRefCategorias.orderByChild("nombre").equalTo(categoria)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                // La categoría no existe, se guarda
                                dbRefCategorias.push().setValue(Categoria(categoria))
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("RegistroGastoIngreso", "Error al verificar categoría: ${error.message}")
                        }
                    })
            }

            Toast.makeText(this, "Transacción guardada", Toast.LENGTH_SHORT).show()
            finish() // o regresar a Principal
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { dia, mes, año -> onDateSelected(dia, mes, año) }
        datePicker.show(supportFragmentManager, "datePicker")
    }

    private fun onDateSelected(dia: Int, mes: Int, año: Int) {
        val etFecha = findViewById<EditText>(R.id.et_fecha)
        etFecha.setText("$dia/${mes + 1}/$año") // mes+1 porque Calendar empieza en 0
    }
}