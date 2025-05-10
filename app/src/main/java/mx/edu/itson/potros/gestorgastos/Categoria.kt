package mx.edu.itson.potros.gestorgastos

data class Categoria(var nombre: String? = null) {
    override fun toString() = nombre.toString()
}