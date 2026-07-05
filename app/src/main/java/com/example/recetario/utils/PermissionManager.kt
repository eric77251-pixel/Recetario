package com.example.recetario.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    // Retorna el permiso correcto según la versión de Android
    private val permisoPrincipal: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun permisosMultimedia(codigoSolicitud: Int = 100): Boolean {
        // Evalúa si el permiso ya fue concedido
        if (ContextCompat.checkSelfPermission(activity, permisoPrincipal) != PackageManager.PERMISSION_GRANTED) {
            // El usuario no ha aceptado el permiso, se solicita
            solicitarPermisos(codigoSolicitud)
            return false
        }
        return true // El permiso ya está concedido
    }

    private fun solicitarPermisos(codigoSolicitud: Int) {
        val listaPermisos = arrayOf(permisoPrincipal)

        // Sirve para dar una explicación al usuario de por qué se pide el permiso si ya lo rechazó una vez
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permisoPrincipal)) {
            Toast.makeText(
                activity,
                "El acceso a los archivos multimedia es necesario para subir imágenes.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Solicita el permiso de forma nativa
        ActivityCompat.requestPermissions(activity, listaPermisos, codigoSolicitud)
    }

    fun mostrarDialogoAjustes() {
        AlertDialog.Builder(activity)
            .setTitle("Permisos requeridos")
            .setMessage("Has denegado los permisos de almacenamiento de forma definitiva. Para usar la galería, debes activarlos manualmente en los ajustes del sistema.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                // Abre la pantalla de detalles de tu aplicación en los Ajustes del teléfono
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                // ¡CORREGIDO!: Faltaba iniciar la actividad del Intent
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancelar", null) // Cambiado a null si no quieres cerrar la app completa de golpe
            .setCancelable(false)
            .show()
    }
}