package mx.edu.itson.potros.gestorgastos

data class Usuario(var nombre: String? = null,
                   var correo: String? = null,
                   var contraseña: String? = null) {
    override fun toString() = nombre + "\t" +
            correo + "\t" +
            contraseña
}