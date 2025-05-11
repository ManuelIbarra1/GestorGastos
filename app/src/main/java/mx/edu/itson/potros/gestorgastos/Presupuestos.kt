package mx.edu.itson.potros.gestorgastos

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class Presupuestos : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var nombreUsuario: String
    private var presupuestoExistenteKey: String? = null
    private val categoriasAsignadas = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presupuestos)

        // Referencias UI
        val etTotal = findViewById<EditText>(R.id.et_presupuesto_total)
        val rgTipo = findViewById<RadioGroup>(R.id.radio_group_tipo)
        val rgMetodo = findViewById<RadioGroup>(R.id.radio_group_metodo)
        val spinner = findViewById<Spinner>(R.id.spinner_categoria)
        val etValorCategoria = findViewById<EditText>(R.id.et_valor_categoria)
        val btnAgregarCategoria = findViewById<Button>(R.id.btn_agregar_categoria)
        val layoutCategorias = findViewById<LinearLayout>(R.id.layout_categorias_agregadas)
        val etAlerta = findViewById<EditText>(R.id.et_porcentaje_alerta)
        val switchAlerta = findViewById<Switch>(R.id.switch_activar_alerta)
        val btnGuardar = findViewById<Button>(R.id.btn_guardar_presupuesto)
        val btnVolver = findViewById<Button>(R.id.btn_volver_presupuesto)
        val containerDistribucion = findViewById<LinearLayout>(R.id.container_distribucion)

        // Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("Presupuestos")
        nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""

        // Cargar categorías al spinner
        FirebaseDatabase.getInstance().getReference("Categorias")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categorias = snapshot.children.mapNotNull {
                        it.child("nombre").getValue(String::class.java)
                    }
                    val adapter = ArrayAdapter(this@Presupuestos, android.R.layout.simple_spinner_item, categorias)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Mostrar u ocultar sección de distribución
        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            containerDistribucion.visibility = if (checkedId == R.id.radio_categorias) View.VISIBLE else View.GONE
        }

        rgMetodo.setOnCheckedChangeListener { _, _ ->
            layoutCategorias.removeAllViews()
            categoriasAsignadas.clear()
        }

        // Cargar presupuesto existente
        dbRef.orderByChild("usuario").equalTo(nombreUsuario)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        presupuestoExistenteKey = snap.key

                        val data = snap.value as? Map<*, *> ?: return
                        val total = data["total"]?.toString()
                        val distribucion = data["distribucion"]?.toString()
                        val metodoDistribucion = data["metodo_distribucion"]?.toString()
                        val alerta = data["alerta"]?.toString()
                        val categorias = data["categorias"] as? Map<*, *>

                        etTotal.setText(total ?: "")

                        if (distribucion == "Distribuido") {
                            rgTipo.check(R.id.radio_categorias)
                            containerDistribucion.visibility = View.VISIBLE

                            if (metodoDistribucion == "Porcentaje") {
                                rgMetodo.check(R.id.radio_porcentaje)
                            } else {
                                rgMetodo.check(R.id.radio_cantidad)
                            }

                            categorias?.forEach { (key, value) ->
                                val categoria = key.toString()
                                val cantidad = value.toString()
                                categoriasAsignadas[categoria] = cantidad
                                agregarCategoriaVisual(layoutCategorias, categoria, cantidad)
                            }
                        } else {
                            rgTipo.check(R.id.radio_total)
                        }

                        if (!alerta.isNullOrEmpty()) {
                            switchAlerta.isChecked = true
                            etAlerta.setText(alerta)
                        }

                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Presupuestos, "Error al cargar presupuesto", Toast.LENGTH_SHORT).show()
                }
            })

        // Agregar categoría manualmente
        btnAgregarCategoria.setOnClickListener {
            val categoria = spinner.selectedItem?.toString()?.trim() ?: return@setOnClickListener
            val valor = etValorCategoria.text.toString().trim()
            if (valor.isEmpty()) return@setOnClickListener

            val metodoDistribucion = when (rgMetodo.checkedRadioButtonId) {
                R.id.radio_cantidad -> "Cantidad"
                R.id.radio_porcentaje -> "Porcentaje"
                else -> "Cantidad"
            }

            val valorNumerico = valor.toDoubleOrNull()
            if (valorNumerico == null || valorNumerico < 0) {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Eliminar categoría si ya estaba, para actualizar valor
            categoriasAsignadas.remove(categoria)
            layoutCategorias.removeAllViews() // limpia para volver a mostrar todo actualizado

            // Verificar suma total
            val sumaExistente = categoriasAsignadas.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
            val limite = if (metodoDistribucion == "Cantidad") {
                etTotal.text.toString().toDoubleOrNull() ?: 0.0
            } else 100.0

            if (sumaExistente + valorNumerico > limite) {
                Toast.makeText(this, "Excede el límite del presupuesto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Agregar al mapa
            categoriasAsignadas[categoria] = valor

            // Redibujar categorías
            categoriasAsignadas.forEach { (cat, valStr) ->
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL

                val textView = TextView(this)
                textView.text = "$cat: $valStr"
                textView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                val deleteBtn = Button(this)
                deleteBtn.text = "X"
                deleteBtn.setOnClickListener {
                    layoutCategorias.removeView(row)
                    categoriasAsignadas.remove(cat)
                }

                row.addView(textView)
                row.addView(deleteBtn)
                layoutCategorias.addView(row)
            }

            etValorCategoria.text.clear()
        }


        btnGuardar.setOnClickListener {
            val total = etTotal.text.toString().trim()
            if (total.isEmpty()) {
                Toast.makeText(this, "Ingresa el presupuesto total", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipoDistribucion = if (rgTipo.checkedRadioButtonId == R.id.radio_categorias) "Distribuido" else "Fija"
            val metodoDistribucion = if (rgMetodo.checkedRadioButtonId == R.id.radio_porcentaje) "Porcentaje" else "Cantidad"

            val alerta = if (switchAlerta.isChecked) etAlerta.text.toString().trim() else null

            val presupuestoMap = mutableMapOf<String, Any?>(
                "usuario" to nombreUsuario,
                "total" to total,
                "distribucion" to tipoDistribucion,
                "metodo_distribucion" to metodoDistribucion
            )

            if (tipoDistribucion == "Distribuido") {
                presupuestoMap["categorias"] = categoriasAsignadas
            }

            if (!alerta.isNullOrEmpty()) {
                presupuestoMap["alerta"] = alerta
            }

            if (presupuestoExistenteKey != null) {
                dbRef.child(presupuestoExistenteKey!!).setValue(presupuestoMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Presupuesto actualizado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } else {
                dbRef.push().setValue(presupuestoMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Presupuesto guardado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }

        btnVolver.setOnClickListener { finish() }
    }

    private fun agregarCategoriaVisual(layout: LinearLayout, categoria: String, valor: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        val textView = TextView(this)
        textView.text = "$categoria: $valor"
        textView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val deleteBtn = Button(this)
        deleteBtn.text = "X"
        deleteBtn.setOnClickListener {
            layout.removeView(row)
            categoriasAsignadas.remove(categoria)
        }

        row.addView(textView)
        row.addView(deleteBtn)
        layout.addView(row)
    }

    private fun esValorPermitido(valor: String, metodo: String, total: String, asignadas: Map<String, String>): Boolean {
        val nuevoValor = valor.toDoubleOrNull() ?: return false
        return if (metodo == "Porcentaje") {
            val sumaActual = asignadas.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
            (sumaActual + nuevoValor) <= 100.0
        } else {
            val totalDouble = total.toDoubleOrNull() ?: return false
            val sumaActual = asignadas.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
            (sumaActual + nuevoValor) <= totalDouble
        }
    }
}
