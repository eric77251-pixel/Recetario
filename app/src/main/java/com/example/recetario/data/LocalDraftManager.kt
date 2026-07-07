package com.example.recetario.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.recetario.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

// Estructura para agrupar todo el borrador en un solo bloque local
@Serializable
data class DraftData(
    val recipe: Recipe,
    val ingredientes: List<String>,
    val pasos: List<String>
)

object LocalDraftManager {
    private const val PREFS_NAME = "MisBorradoresLocales"
    private const val KEY_LISTA = "lista_borradores"

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun guardarBorrador(
        context: Context,
        recipe: Recipe,
        ingredientes: List<String>,
        pasos: List<String>,
        uriImagenOriginal: Uri?
    ) {
        // 1. Si el usuario seleccionó una foto nueva, la copiamos físicamente al celular
        if (uriImagenOriginal != null) {
            recipe.imagenUrl = guardarImagenLocal(context, uriImagenOriginal, recipe.id)
        }

        // 2. Empaquetamos todo
        val nuevoBorrador = DraftData(recipe, ingredientes, pasos)
        val borradores = obtenerTodos(context).toMutableList()

        // 3. Reemplazamos si ya existía, o lo agregamos nuevo
        val index = borradores.indexOfFirst { it.recipe.id == recipe.id }
        if (index != -1) {
            borradores[index] = nuevoBorrador
        } else {
            borradores.add(nuevoBorrador)
        }

        // 4. Guardamos como texto JSON
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LISTA, jsonParser.encodeToString(borradores)).apply()
    }

    fun obtenerTodos(context: Context): List<DraftData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_LISTA, null) ?: return emptyList()
        return try {
            jsonParser.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun eliminarBorrador(context: Context, id: String) {
        val borradores = obtenerTodos(context).toMutableList()
        val borrador = borradores.find { it.recipe.id == id }

        // ¡Importante! Borrar la foto física para no llenar la memoria del celular
        borrador?.recipe?.imagenUrl?.let { ruta ->
            if (ruta.isNotBlank()) File(ruta).delete()
        }

        borradores.removeAll { it.recipe.id == id }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LISTA, jsonParser.encodeToString(borradores)).apply()
    }

    private fun guardarImagenLocal(context: Context, uri: Uri, id: String): String {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            // Reducimos la imagen para que la app no se vuelva pesada
            val ratio = min(900f / bitmap.width, 900f / bitmap.height).coerceAtMost(1f)
            val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)

            // Guardamos en la memoria protegida de la aplicación
            val file = File(context.filesDir, "borrador_$id.jpg")
            val stream = FileOutputStream(file)
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.flush()
            stream.close()

            file.absolutePath // Retorna la ruta física (ej. /data/user/0/.../borrador.jpg)
        } catch (e: Exception) {
            ""
        }
    }
}