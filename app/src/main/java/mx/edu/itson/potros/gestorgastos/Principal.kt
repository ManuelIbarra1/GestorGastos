package mx.edu.itson.potros.gestorgastos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class Principal : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var transaccionesRef: DatabaseReference
    private lateinit var nombreUsuario: String
    private val listaGastos = mutableListOf<Transaccion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Obtener nombre del usuario
        nombreUsuario = intent.getStringExtra("nombre_usuario") ?: ""

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.rv_gastos_por_categoria)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Referencia a Firebase
        transaccionesRef = FirebaseDatabase.getInstance().getReference("Transacciones")

        cargarGastos()

        // Navegación con botones
        findViewById<ImageView>(R.id.btn_agregar_presupuesto).setOnClickListener {
            startActivity(Intent(this, Presupuesto::class.java))
        }

        findViewById<ImageView>(R.id.btn_editar_usuario).setOnClickListener {
            startActivity(Intent(this, Configuracion::class.java))
        }

        findViewById<Button>(R.id.btn_agregar_transaccion).setOnClickListener {
            val intent = Intent(this, RegistroGastoIngreso::class.java)
            intent.putExtra("nombre_usuario", nombreUsuario) // lo pasas para usarlo allá
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_ver_grafica).setOnClickListener {
            //startActivity(Intent(this, GraficaGastos::class.java))
        }
    }

    private fun cargarGastos() {
        transaccionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaGastos.clear()
                for (transSnap in snapshot.children) {
                    try {
                        val transaccion = transSnap.getValue(Transaccion::class.java)
                        if (transaccion != null &&
                            transaccion.tipo == "Gasto" &&
                            transaccion.usuario == nombreUsuario
                        ) {
                            listaGastos.add(transaccion)
                        }
                    } catch (e: Exception) {
                        Log.e("Principal", "Error al procesar transacción: ${e.message}")
                    }
                }
                recyclerView.adapter = GastoAdapter(listaGastos)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Principal", "Error al leer transacciones: ${error.message}")
            }
        })
    }
}

class GastoAdapter(private val gastos: List<Transaccion>) :
    RecyclerView.Adapter<GastoAdapter.GastoViewHolder>() {

    inner class GastoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.tv_nombre_gasto)
        val monto: TextView = view.findViewById(R.id.tv_monto_gasto)
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
    }

    override fun getItemCount(): Int = gastos.size
}