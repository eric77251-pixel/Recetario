package com.example.recetario.utils

import android.app.Activity
import android.content.Intent
import com.example.recetario.activities.MainActivity
import com.example.recetario.activities.ProfileActivity
import com.example.recetario.activities.CreateRecipeActivity

object NavigationHelper {

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

        if (activity !is ProfileActivity) {

            activity.startActivity(
                Intent(activity, ProfileActivity::class.java)
            )

            activity.finish()

            return true
        }

        return false
    }

    fun irPublicacion(activity: Activity): Boolean {

        if (activity !is CreateRecipeActivity) {

            activity.startActivity(
                Intent(activity, CreateRecipeActivity::class.java)
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