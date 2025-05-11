package mx.edu.itson.potros.gestorgastos

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class GraficaGastos : AppCompatActivity() {

    private lateinit var nombreUsuario: String
    private lateinit var database: DatabaseReference

    private lateinit var barChart: BarChart
    private lateinit var spinnerCategoria: Spinner
    private lateinit var etFechaInicio: EditText
    private lateinit var etFechaFin: EditText
    private lateinit var radioFecha: RadioButton
    private lateinit var radioCategoria: RadioButton
    private lateinit var filtroFechaLayout: LinearLayout
    private lateinit var btnActualizar: Button
    private lateinit var btnSalir: Button

    private val coloresDisponibles = ColorTemplate.MATERIAL_COLORS.toMutableList()
    private val coloresPorCategoria = mutableMapOf(
        "Alimentos" to Color.rgb(46, 204, 113),
        "Transporte" to Color.rgb(52, 152, 219),
        "Entretenimiento" to Color.rgb(241, 196, 15),
        "Salud" to Color.rgb(231, 76, 60),
        "Otros" to Color.rgb(155, 89, 182)
    )

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grafica_gastos)

        nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        database = FirebaseDatabase.getInstance().getReference("Transacciones")

        barChart = findViewById(R.id.barChart)
        spinnerCategoria = findViewById(R.id.spinner_filtro_categoria)
        etFechaInicio = findViewById(R.id.et_fecha_inicio)
        etFechaFin = findViewById(R.id.et_fecha_fin)
        radioFecha = findViewById(R.id.radio_por_fecha)
        radioCategoria = findViewById(R.id.radio_por_categoria)
        filtroFechaLayout = findViewById(R.id.filtro_fecha_layout)
        btnActualizar = findViewById(R.id.btn_actualizar_grafica)
        btnSalir = findViewById(R.id.btn_salir)

        etFechaInicio.setOnClickListener { mostrarDatePicker(etFechaInicio) }
        etFechaFin.setOnClickListener { mostrarDatePicker(etFechaFin) }

        radioFecha.setOnCheckedChangeListener { _, isChecked ->
            filtroFechaLayout.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            spinnerCategoria.visibility = if (isChecked) Spinner.GONE else Spinner.VISIBLE
        }

        cargarCategoriasEnSpinner()

        btnActualizar.setOnClickListener {
            if (radioCategoria.isChecked) {
                generarGraficaPorCategoria()
            } else {
                generarGraficaPorFecha()
            }
        }

        btnSalir.setOnClickListener { finish() }
    }

    private fun mostrarDatePicker(et: EditText) {
        val calendario = Calendar.getInstance()
        val picker = DatePickerDialog(this, { _, año, mes, dia ->
            val fecha = String.format("%02d/%02d/%d", dia, mes + 1, año)
            et.setText(fecha)
        }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH))
        picker.show()
    }

    private fun cargarCategoriasEnSpinner() {
        val refCat = FirebaseDatabase.getInstance().getReference("Categorias")
        val categorias = mutableListOf("Todas las categorías")
        refCat.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val nombre = snap.child("nombre").getValue(String::class.java)
                    if (!nombre.isNullOrEmpty()) categorias.add(nombre)
                }
                val adapter = ArrayAdapter(this@GraficaGastos, android.R.layout.simple_spinner_item, categorias)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategoria.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun generarGraficaPorCategoria() {
        val categoriaSeleccionada = spinnerCategoria.selectedItem.toString()
        val datos = mutableMapOf<String, Float>()

        database.orderByChild("usuario").equalTo(nombreUsuario)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        val trans = snap.getValue(Transaccion::class.java)
                        if (trans?.tipo == "Gasto") {
                            val categoria = trans.categoria ?: continue
                            if (categoriaSeleccionada == "Todas las categorías" || categoria == categoriaSeleccionada) {
                                val monto = trans.cantidad?.toFloatOrNull() ?: 0f
                                datos[categoria] = datos.getOrDefault(categoria, 0f) + monto
                            }
                        }
                    }

                    if (datos.isEmpty()) {
                        Toast.makeText(this@GraficaGastos, "No hay datos para mostrar", Toast.LENGTH_SHORT).show()
                        barChart.clear()
                        return
                    }

                    val labels = datos.keys.toList()
                    val entries = datos.entries.mapIndexed { index, entry ->
                        BarEntry(index.toFloat(), entry.value)
                    }

                    val colors = labels.map { categoria ->
                        coloresPorCategoria.getOrPut(categoria) {
                            coloresDisponibles.removeFirstOrNull() ?: Color.GRAY
                        }
                    }

                    val dataSet = BarDataSet(entries, "Gastos por categoría")
                    dataSet.colors = colors
                    dataSet.setDrawValues(true)

                    barChart.data = BarData(dataSet)
                    barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    barChart.xAxis.granularity = 1f
                    barChart.xAxis.setDrawGridLines(false)
                    barChart.axisLeft.axisMinimum = 0f
                    barChart.axisRight.isEnabled = false
                    barChart.description.isEnabled = false

                    barChart.legend.isEnabled = true
                    barChart.legend.setCustom(colors.mapIndexed { index, color ->
                        LegendEntry(labels[index], Legend.LegendForm.DEFAULT, 10f, 2f, null, color)
                    })

                    barChart.invalidate()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun generarGraficaPorFecha() {
        val fechaInicioStr = etFechaInicio.text.toString()
        val fechaFinStr = etFechaFin.text.toString()

        if (fechaInicioStr.isEmpty() || fechaFinStr.isEmpty()) {
            Toast.makeText(this, "Selecciona ambas fechas", Toast.LENGTH_SHORT).show()
            return
        }

        val f1 = formatoFecha.parse(fechaInicioStr)
        val f2 = formatoFecha.parse(fechaFinStr)
        if (f1 == null || f2 == null || f1.after(f2)) {
            Toast.makeText(this, "Rango de fechas inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val lista = mutableListOf<BarEntry>()
        val colores = mutableListOf<Int>()
        val etiquetas = mutableListOf<String>()
        val leyendaPorCategoria = mutableMapOf<String, Int>()

        database.orderByChild("usuario").equalTo(nombreUsuario)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var index = 0
                    for (snap in snapshot.children) {
                        val trans = snap.getValue(Transaccion::class.java)
                        if (trans?.tipo == "Gasto") {
                            val fecha = trans.fecha ?: continue
                            try {
                                val fechaTrans = formatoFecha.parse(fecha)
                                if (fechaTrans != null && !fechaTrans.before(f1) && !fechaTrans.after(f2)) {
                                    val monto = trans.cantidad?.toFloatOrNull() ?: continue
                                    val categoria = trans.categoria ?: "Otros"
                                    val color = coloresPorCategoria.getOrPut(categoria) {
                                        coloresDisponibles.removeFirstOrNull() ?: Color.GRAY
                                    }

                                    lista.add(BarEntry(index.toFloat(), monto))
                                    etiquetas.add(trans.descipcion ?: "")
                                    colores.add(color)
                                    leyendaPorCategoria[categoria] = color
                                    index++
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (lista.isEmpty()) {
                        Toast.makeText(this@GraficaGastos, "No hay datos para mostrar", Toast.LENGTH_SHORT).show()
                        barChart.clear()
                        return
                    }

                    val dataSet = BarDataSet(lista, "Gastos por fecha")
                    dataSet.colors = colores
                    val data = BarData(dataSet)

                    barChart.data = data
                    barChart.xAxis.valueFormatter = IndexAxisValueFormatter(etiquetas)
                    barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    barChart.xAxis.granularity = 1f
                    barChart.xAxis.setDrawGridLines(false)
                    barChart.axisLeft.axisMinimum = 0f
                    barChart.axisRight.isEnabled = false
                    barChart.description.isEnabled = false

                    barChart.legend.isEnabled = true
                    barChart.legend.setCustom(leyendaPorCategoria.map { (categoria, color) ->
                        LegendEntry(categoria, Legend.LegendForm.DEFAULT, 10f, 2f, null, color)
                    })

                    barChart.invalidate()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
