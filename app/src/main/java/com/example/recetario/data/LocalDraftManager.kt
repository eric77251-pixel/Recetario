package com.example.recetario.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.recetario.model.Recipe
import com.google.firebase.auth.FirebaseAuth // Importación vital para saber quién está usando la app
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

@Serializable
data class DraftData(
    val recipe: Recipe,
    val ingredientes: List<String>,
    val pasos: List<String>
)

object LocalDraftManager {
    private const val PREFS_NAME = "MisBorradoresLocales"

    private val jsonParser = Json { ignoreUnknownKeys = true }

    // NUEVA FUNCIÓN: Crea una clave única o "cajón" para cada cuenta que inicie sesión
    private fun getKeyUsuario(): String {
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_desconocido"
        return "borradores_$usuarioId"
    }

    fun guardarBorrador(
        context: Context,
        recipe: Recipe,
        ingredientes: List<String>,
        pasos: List<String>,
        uriImagenOriginal: Uri?
    ) {
        if (uriImagenOriginal != null) {
            recipe.imagenUrl = guardarImagenLocal(context, uriImagenOriginal, recipe.id)
        }

        val nuevoBorrador = DraftData(recipe, ingredientes, pasos)
        val borradores = obtenerTodos(context).toMutableList()

        val index = borradores.indexOfFirst { it.recipe.id == recipe.id }
        if (index != -1) {
            borradores[index] = nuevoBorrador
        } else {
            borradores.add(nuevoBorrador)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Usamos la clave única del usuario para guardar
        prefs.edit().putString(getKeyUsuario(), jsonParser.encodeToString(borradores)).apply()
    }

    fun obtenerTodos(context: Context): List<DraftData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Usamos la clave única del usuario para leer
        val jsonString = prefs.getString(getKeyUsuario(), null) ?: return emptyList()
        return try {
            jsonParser.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun eliminarBorrador(context: Context, id: String) {
        val borradores = obtenerTodos(context).toMutableList()
        val borrador = borradores.find { it.recipe.id == id }

        borrador?.recipe?.imagenUrl?.let { ruta ->
            if (ruta.isNotBlank()) File(ruta).delete()
        }

        borradores.removeAll { it.recipe.id == id }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Usamos la clave única del usuario para sobrescribir (borrar)
        prefs.edit().putString(getKeyUsuario(), jsonParser.encodeToString(borradores)).apply()
    }

    private fun guardarImagenLocal(context: Context, uri: Uri, id: String): String {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            val ratio = min(900f / bitmap.width, 900f / bitmap.height).coerceAtMost(1f)
            val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)

            val file = File(context.filesDir, "borrador_$id.jpg")
            val stream = FileOutputStream(file)
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.flush()
            stream.close()

            file.absolutePath
        } catch (e: Exception) {
            ""
        }
    }
}