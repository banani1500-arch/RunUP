package com.runup

import com.runup.model.Marcador
import com.runup.model.EntrenoDay
import com.runup.screens.Zapa

object AppState {
    val entrenosGlobal: MutableList<Marcador> = mutableListOf()
    val zapatillasGlobal: MutableList<Zapa> = mutableListOf()
    val calendarioGlobal: MutableList<EntrenoDay> = mutableListOf()
    var perfilGlobal: MutableMap<String, String> = mutableMapOf()

}