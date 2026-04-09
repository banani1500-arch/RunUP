package com.runup.model

import java.time.LocalDate

data class EntrenoDay(
    val id: Int = 0,
    val fecha: LocalDate,
    val tipo: String,
    val descripcion: String
)