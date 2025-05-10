package mx.edu.itson.potros.gestorgastos

data class Transaccion(var usuario: String,
                       var cantidad: String,
                       var descipcion: String,
                       var tipo: String,
                       var categoria: String,
                       var tipo_pago: String,
                       var fecha: String) {
    override fun toString() = usuario + "\t" +
            cantidad + "\t" +
            descipcion + "\t" +
            tipo + "\t" +
            categoria + "\t" +
            tipo_pago + "\t" +
            fecha
}