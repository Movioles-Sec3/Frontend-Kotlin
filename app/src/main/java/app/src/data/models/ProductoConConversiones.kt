package app.src.data.models

import com.google.gson.annotations.SerializedName

data class ProductoConConversiones(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String,

    @SerializedName("imagen_url")
    val imagenUrl: String,

    @SerializedName("precio_original")
    val precioOriginal: Double,

    @SerializedName("moneda_original")
    val monedaOriginal: String,

    @SerializedName("conversiones")
    val conversiones: Map<String, Double>,

    @SerializedName("fecha_actualizacion")
    val fechaActualizacion: String
)
