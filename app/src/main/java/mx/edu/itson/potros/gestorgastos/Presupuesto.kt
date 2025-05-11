package mx.edu.itson.potros.gestorgastos

data class Presupuesto(var usuario: String,
                       var total: String,
                       var distribucion: String,
                       var metodo_distribucion: String,
                       var categorias: String,
                       var alerta: String) {
    override fun toString() = usuario + "\t" +
            total + "\t" +
            distribucion + "\t" +
            metodo_distribucion + "\t" +
            categorias + "\t" +
            alerta
}