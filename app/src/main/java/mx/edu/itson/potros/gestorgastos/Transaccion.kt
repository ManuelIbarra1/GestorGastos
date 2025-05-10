package mx.edu.itson.potros.gestorgastos

data class Transaccion(var usuario: String? = null,
                       var cantidad: String? = null,
                       var descipcion: String? = null,
                       var tipo: String? = null,
                       var categoria: String? = null,
                       var tipo_pago: String? = null,
                       var fecha: String? = null) {
    override fun toString() = usuario + "\t" +
            cantidad + "\t" +
            descipcion + "\t" +
            tipo + "\t" +
            categoria + "\t" +
            tipo_pago + "\t" +
            fecha
}