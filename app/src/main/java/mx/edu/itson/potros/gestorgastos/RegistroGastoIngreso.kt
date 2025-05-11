package mx.edu.itson.potros.gestorgastos

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class RegistroGastoIngreso : AppCompatActivity() {
    private lateinit var nombreUsuario: String
    private val dbRefTransacciones = FirebaseDatabase.getInstance().getReference("Transacciones")
    private val dbRefCategorias = FirebaseDatabase.getInstance().getReference("Categorias")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_gasto_ingreso)

        val radioTipo = findViewById<RadioGroup>(R.id.radio_group_tipo_transaccion)
        val radioPago = findViewById<RadioGroup>(R.id.radio_group_tipo_pago)
        val etMonto = findViewById<EditText>(R.id.et_monto)
        val etDescripcion = findViewById<EditText>(R.id.et_descripcion)
        val etCategoria = findViewById<EditText>(R.id.et_categoria)
        val etFecha = findViewById<EditText>(R.id.et_fecha)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar)

        etFecha.setOnClickListener { showDatePickerDialog() }

        val modo = intent.getStringExtra("modo")
        val idTransaccion = intent.getStringExtra("id")
        nombreUsuario = intent.getStringExtra("nombre_usuario") ?: intent.getStringExtra("usuario") ?: ""

        if (modo == "editar") {
            btnGuardar.text = "Actualizar"
            etMonto.setText(intent.getStringExtra("cantidad"))
            etDescripcion.setText(intent.getStringExtra("descipcion"))
            etCategoria.setText(intent.getStringExtra("categoria"))
            etFecha.setText(intent.getStringExtra("fecha"))

            val tipo = intent.getStringExtra("tipo")
            if (tipo == "Ingreso") {
                radioTipo.check(R.id.radio_ingreso)
            } else {
                radioTipo.check(R.id.radio_gasto)
            }

            val tipoPago = intent.getStringExtra("tipo_pago")
            if (tipoPago == "Efectivo") {
                radioPago.check(R.id.radio_efectivo)
            } else if (tipoPago == "Tarjeta") {
                radioPago.check(R.id.radio_tarjeta)
            }
        }

        btnGuardar.setOnClickListener {
            val tipoSeleccionado = findViewById<RadioButton>(radioTipo.checkedRadioButtonId)?.text.toString()
            val tipoPagoSeleccionado = if (tipoSeleccionado == "Gasto")
                findViewById<RadioButton>(radioPago.checkedRadioButtonId)?.text.toString()
            else null

            val cantidad = etMonto.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val categoria = if (tipoSeleccionado == "Gasto") etCategoria.text.toString().trim() else null
            val fecha = etFecha.text.toString().trim()

            if (cantidad.isEmpty() || descripcion.isEmpty() || fecha.isEmpty() ||
                (tipoSeleccionado == "Gasto" && (categoria.isNullOrEmpty() || tipoPagoSeleccionado.isNullOrEmpty()))) {
                Toast.makeText(this, "Por favor llena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaccion = Transaccion(
                usuario = nombreUsuario,
                cantidad = cantidad,
                descipcion = descripcion,
                tipo = tipoSeleccionado,
                categoria = categoria,
                tipo_pago = tipoPagoSeleccionado,
                fecha = fecha
            )

            if (modo == "editar" && !idTransaccion.isNullOrEmpty()) {
                dbRefTransacciones.child(idTransaccion).setValue(transaccion)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Transacción actualizada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
            } else {
                dbRefTransacciones.push().setValue(transaccion)
                Toast.makeText(this, "Transacción guardada", Toast.LENGTH_SHORT).show()
            }

            if (!categoria.isNullOrEmpty()) {
                dbRefCategorias.orderByChild("nombre").equalTo(categoria)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                dbRefCategorias.push().setValue(Categoria(categoria))
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("RegistroGastoIngreso", "Error al verificar categoría: ${error.message}")
                        }
                    })
            }

            finish()
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
        findViewById<EditText>(R.id.et_fecha).setText("$dia/${mes + 1}/$año")
    }
}
