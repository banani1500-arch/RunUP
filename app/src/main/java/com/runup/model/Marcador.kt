package com.runup.model

import com.google.android.gms.maps.model.LatLng

data class Marcador(
    val id: String,
    val nombre: String,
    val tiempo: String,
    val tipoEntreno: String,
    val sensaciones: String,
    val kilometros: Double,
    val posicion: LatLng
) {
    companion object {
        fun fromJson(json: Map<String, Any?>): Marcador {
            return Marcador(
                id = json["id"]?.toString() ?: "",
                nombre = json["nombre"] as? String ?: "",
                tiempo = json["tiempo"] as? String ?: "",
                sensaciones = json["sensaciones"] as? String ?: "",
                tipoEntreno = json["tipoEntreno"] as? String ?: "",
                kilometros = json["km"]?.toString()?.toDoubleOrNull() ?: 0.0,
                posicion = LatLng(
                    json["lat"]?.toString()?.toDoubleOrNull() ?: 0.0,
                    json["lng"]?.toString()?.toDoubleOrNull() ?: 0.0
                )
            )
        }
    }

    fun toJson(): Map<String, Any> = mapOf(
        "id" to id,
        "nombre" to nombre,
        "tiempo" to tiempo,
        "tipoEntreno" to tipoEntreno,
        "kilometros" to kilometros,
        "sensaciones" to sensaciones,
        "lat" to posicion.latitude,
        "lng" to posicion.longitude
    )
}