package mx.edu.itson.potros.gestorgastos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class Principal : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var transaccionesRef: DatabaseReference
    private var nombreUsuario: String = ""
    private var correoUsuario: String = ""
    private val listaTransacciones = mutableListOf<Transaccion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""
        correoUsuario = intent.getStringExtra("correo_usuario") ?: ""

        recyclerView = findViewById(R.id.rv_gastos_por_categoria)
        recyclerView.layoutManager = LinearLayoutManager(this)

        transaccionesRef = FirebaseDatabase.getInstance().getReference("Transacciones")

        cargarTransacciones()

        findViewById<ImageView>(R.id.btn_agregar_presupuesto).setOnClickListener {
            startActivity(Intent(this, Presupuesto::class.java))
        }

        findViewById<ImageView>(R.id.btn_editar_usuario).setOnClickListener {
            val intent = Intent(this, Configuracion::class.java)
            intent.putExtra("nombre_usuario", nombreUsuario)
            intent.putExtra("correo_usuario", correoUsuario)
            startActivityForResult(intent, 1)
        }

        findViewById<ImageView>(R.id.btn_agregar_presupuesto).setOnClickListener {
            val intent = Intent(this, Presupuestos::class.java)
            intent.putExtra("nombre_usuario", nombreUsuario)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_ver_grafica).setOnClickListener {
            startActivity(Intent(this, GraficaGastos::class.java))
        }
    }

    private fun cargarTransacciones() {
        transaccionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaTransacciones.clear()
                var totalGastos = 0.0
                var totalIngresos = 0.0

                for (transSnap in snapshot.children) {
                    try {
                        val transaccion = transSnap.getValue(Transaccion::class.java)
                        transaccion?.id = transSnap.key
                        if (transaccion != null && transaccion.usuario == nombreUsuario) {
                            listaTransacciones.add(transaccion)

                            val monto = transaccion.cantidad?.toDoubleOrNull() ?: 0.0
                            when (transaccion.tipo) {
                                "Gasto" -> totalGastos += monto
                                "Ingreso" -> totalIngresos += monto
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Principal", "Error al procesar transacción: ${e.message}")
                    }
                }

                recyclerView.adapter = GastoAdapter(
                    gastos = listaTransacciones,
                    onEliminarClick = { transaccion ->
                        transaccion.id?.let {
                            transaccionesRef.child(it).removeValue()
                        }
                    },
                    onEditarClick = { transaccion ->
                        editarTransaccion(transaccion)
                    }
                )

                findViewById<TextView>(R.id.tv_gastos_mes).text = "$%.2f".format(totalGastos)
                findViewById<TextView>(R.id.tv_ingresos_mes).text = "$%.2f".format(totalIngresos)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Principal", "Error al leer transacciones: ${error.message}")
            }
        })
    }

    private fun editarTransaccion(transaccion: Transaccion) {
        val intent = Intent(this, RegistroGastoIngreso::class.java)
        intent.putExtra("modo", "editar")
        intent.putExtra("id", transaccion.id)
        intent.putExtra("usuario", transaccion.usuario)
        intent.putExtra("cantidad", transaccion.cantidad)
        intent.putExtra("descipcion", transaccion.descipcion)
        intent.putExtra("tipo", transaccion.tipo)
        intent.putExtra("categoria", transaccion.categoria)
        intent.putExtra("tipo_pago", transaccion.tipo_pago)
        intent.putExtra("fecha", transaccion.fecha)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            val nuevoNombre = data.getStringExtra("nombre_usuario_actualizado")
            val nuevoCorreo = data.getStringExtra("correo_usuario_actualizado")

            if (!nuevoNombre.isNullOrEmpty()) nombreUsuario = nuevoNombre
            if (!nuevoCorreo.isNullOrEmpty()) correoUsuario = nuevoCorreo

            Toast.makeText(this, "Datos actualizados localmente", Toast.LENGTH_SHORT).show()
        }
    }
}

class GastoAdapter(
    private val gastos: List<Transaccion>,
    private val onEliminarClick: (Transaccion) -> Unit,
    private val onEditarClick: (Transaccion) -> Unit
) : RecyclerView.Adapter<GastoAdapter.GastoViewHolder>() {


    inner class GastoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.tv_nombre_gasto)
        val monto: TextView = view.findViewById(R.id.tv_monto_gasto)
        val btnEliminar: ImageView = view.findViewById(R.id.iv_borrar_gasto)
        val btnEditar: ImageView = view.findViewById(R.id.iv_editar_gasto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)
        return GastoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        val gasto = gastos[position]
        holder.nombre.text = gasto.descipcion
        holder.monto.text = "$${gasto.cantidad}"
        holder.btnEliminar.setOnClickListener {
            onEliminarClick(gasto)
        }
        holder.btnEditar.setOnClickListener {
            onEditarClick(gasto)
        }
    }

    override fun getItemCount(): Int = gastos.size
}