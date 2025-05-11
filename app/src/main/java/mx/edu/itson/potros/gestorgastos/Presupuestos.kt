package mx.edu.itson.potros.gestorgastos

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class Presupuestos : AppCompatActivity() {

    private val dbRef = FirebaseDatabase.getInstance().getReference("Presupuestos")
    private lateinit var nombreUsuario: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presupuestos)

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

        nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        val categoriasAsignadas = mutableMapOf<String, String>()
        var presupuestoExistenteKey: String? = null

        val categoriasRef = FirebaseDatabase.getInstance().getReference("Categorias")
        categoriasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<String>()
                for (snap in snapshot.children) {
                    snap.child("nombre").getValue(String::class.java)?.let { lista.add(it) }
                }
                val adapter = ArrayAdapter(this@Presupuestos, android.R.layout.simple_spinner_item, lista)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            containerDistribucion.visibility = if (checkedId == R.id.radio_categorias) View.VISIBLE else View.GONE
        }

        rgMetodo.setOnCheckedChangeListener { _, _ ->
            layoutCategorias.removeAllViews()
            categoriasAsignadas.clear()
        }

        btnAgregarCategoria.setOnClickListener {
            val categoria = spinner.selectedItem?.toString()?.trim() ?: return@setOnClickListener
            val valor = etValorCategoria.text.toString().trim()
            if (valor.isEmpty()) return@setOnClickListener

            val existente = layoutCategorias.findViewWithTag<LinearLayout>(categoria)
            if (existente != null) layoutCategorias.removeView(existente)

            categoriasAsignadas[categoria] = valor

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.tag = categoria

            val textView = TextView(this)
            textView.text = "$categoria: $valor"
            textView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val deleteBtn = Button(this)
            deleteBtn.text = "X"
            deleteBtn.setOnClickListener {
                layoutCategorias.removeView(row)
                categoriasAsignadas.remove(categoria)
            }

            row.addView(textView)
            row.addView(deleteBtn)
            layoutCategorias.addView(row)
            etValorCategoria.text.clear()
        }

        dbRef.orderByChild("usuario").equalTo(nombreUsuario)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        val map = snap.value as? Map<*, *> ?: continue
                        presupuestoExistenteKey = snap.key

                        val total = map["total"]?.toString() ?: "0"
                        val distribucion = map["distribucion"]?.toString() ?: "Fija"
                        val alerta = map["alerta"]?.toString() ?: ""

                        etTotal.setText(total)
                        if (distribucion == "Fija") {
                            rgTipo.check(R.id.radio_total)
                        } else {
                            rgTipo.check(R.id.radio_categorias)
                            containerDistribucion.visibility = View.VISIBLE
                            rgMetodo.check(R.id.radio_cantidad)

                            val catMap = map["categorias"] as? Map<*, *> ?: continue
                            for ((key, value) in catMap) {
                                val cat = key.toString()
                                val valCat = value.toString()
                                categoriasAsignadas[cat] = valCat

                                val row = LinearLayout(this@Presupuestos)
                                row.orientation = LinearLayout.HORIZONTAL
                                row.tag = cat

                                val textView = TextView(this@Presupuestos)
                                textView.text = "$cat: $valCat"
                                textView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                                val deleteBtn = Button(this@Presupuestos)
                                deleteBtn.text = "X"
                                deleteBtn.setOnClickListener {
                                    layoutCategorias.removeView(row)
                                    categoriasAsignadas.remove(cat)
                                }

                                row.addView(textView)
                                row.addView(deleteBtn)
                                layoutCategorias.addView(row)
                            }
                        }

                        if (alerta.isNotEmpty()) {
                            switchAlerta.isChecked = true
                            etAlerta.setText(alerta)
                        }

                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        btnGuardar.setOnClickListener {
            val total = etTotal.text.toString().trim()
            if (total.isEmpty()) {
                Toast.makeText(this, "Ingresa el presupuesto total", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipoDistribucion = when (rgTipo.checkedRadioButtonId) {
                R.id.radio_total -> "Fija"
                R.id.radio_categorias -> "Distribuido"
                else -> "Fija"
            }

            val categoriasFinal = if (tipoDistribucion == "Distribuido" && categoriasAsignadas.isNotEmpty())
                categoriasAsignadas else null

            val alertaFinal = if (switchAlerta.isChecked) {
                etAlerta.text.toString().trim().takeIf { it.isNotEmpty() }
            } else null

            val dataMap = mutableMapOf<String, Any>(
                "usuario" to nombreUsuario,
                "total" to total,
                "distribucion" to tipoDistribucion
            )

            categoriasFinal?.let { dataMap["categorias"] = it }
            alertaFinal?.let { dataMap["alerta"] = it }

            val ref = if (presupuestoExistenteKey != null)
                dbRef.child(presupuestoExistenteKey!!)
            else
                dbRef.push()

            ref.setValue(dataMap).addOnSuccessListener {
                Toast.makeText(this, "Presupuesto guardado", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}
