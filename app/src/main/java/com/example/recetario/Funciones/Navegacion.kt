package com.example.recetario.Funciones

import android.app.Activity
import android.content.Intent
import com.example.recetario.Actividades.MainActivity
import com.example.recetario.Actividades.Perfil
import com.example.recetario.Actividades.Publicacion

object Navegacion {

    fun irRecetas(activity: Activity): Boolean {

        if (activity !is MainActivity) {

            val intent = Intent(activity, MainActivity::class.java).apply {
                putExtra("FORZAR_FRAGMENTO", "Recetas")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            activity.startActivity(intent)
            activity.finish()

            return true
        }

        return false
    }

    fun irPerfil(activity: Activity): Boolean {

        if (activity !is Perfil) {

            activity.startActivity(
                Intent(activity, Perfil::class.java)
            )

            activity.finish()

            return true
        }

        return false
    }

    fun irPublicacion(activity: Activity): Boolean {

        if (activity !is Publicacion) {

            activity.startActivity(
                Intent(activity, Publicacion::class.java)
            )

            activity.finish()

            return true
        }

        return false
    }


    fun volverARecetas(activity: Activity) {

        val intent = Intent(activity, MainActivity::class.java).apply {

            putExtra("FORZAR_FRAGMENTO", "Recetas")

            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        activity.startActivity(intent)
        activity.finish()
    }
}