package com.example.recetario.Actividades

import java.util.UUID
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.addCallback
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.recetario.Funciones.Navegacion
import com.example.recetario.Funciones.Permisos
import com.example.recetario.Funciones.Sesion
import com.example.recetario.Funciones.Validaciones
import com.example.recetario.Manager.IngredientManager
import com.example.recetario.Manager.StepManager
import com.example.recetario.Manager.RecipeManager
import com.example.recetario.Modelos.Ingredientes
import com.example.recetario.Modelos.Pasos
import com.example.recetario.Modelos.Recipe
import com.example.recetario.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class CreateRecipeActivity : AppCompatActivity() {

    private var selectedMediaUri: Uri? = null
    private var tipoContenido = ""

    private lateinit var tvMediaHint: TextView
    private lateinit var btnSelectMedia: Button
    private lateinit var imagePreview: ImageView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var etPostNombre: TextInputEditText
    private lateinit var etPostDescripcion: TextInputEditText
    private lateinit var etPostIngredientes: TextInputEditText
    private lateinit var etPostProceso: TextInputEditText

    private lateinit var btnCancelarPost: Button
    private lateinit var btnGuardarPost: Button

    private val manejadorPermisos = Permisos(this)

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            tipoContenido = "imagen"
            tvMediaHint.visibility = View.GONE
            btnSelectMedia.text = "Cambiar imagen"
            imagePreview.visibility = View.VISIBLE

            val bitmap = ajustarImagen(uri, 800)
            if (bitmap != null) {
                imagePreview.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nuevopost)

        inicializarVistas()
        configurarNavegacion()


        btnSelectMedia.setOnClickListener {
            if (manejadorPermisos.permisosMultimedia(100)) {
                abrirGaleria()
            }
        }

        btnCancelarPost.setOnClickListener {
            finish()
        }

        btnGuardarPost.setOnClickListener {
            validarYPublicar()
        }
        onBackPressedDispatcher.addCallback(this) {
            Navegacion.volverARecetas(this@CreateRecipeActivity)
        }
    }

    private fun inicializarVistas() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnSelectMedia = findViewById(R.id.btnPostImagen)
        tvMediaHint = findViewById(R.id.tvMediaHint)
        imagePreview = findViewById(R.id.imagePreview)

        etPostNombre = findViewById(R.id.etPostNombre)
        etPostDescripcion = findViewById(R.id.etPostDescripcion)
        etPostIngredientes = findViewById(R.id.etPostIngredientes)
        etPostProceso = findViewById(R.id.etPostProceso)

        btnCancelarPost = findViewById(R.id.btnCancelarPost)
        btnGuardarPost = findViewById(R.id.btnGuardarPost)
    }

    private fun configurarNavegacion() {
        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> Navegacion.irRecetas(this)
                R.id.nav_añadir -> true
                R.id.nav_perfil -> Navegacion.irPerfil(this)
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_añadir
    }

    private fun abrirGaleria() {
        pickMedia.launch("image/*")
    }

    private fun validarYPublicar() {

        val nombre = etPostNombre.text.toString().trim()
        val descripcion = etPostDescripcion.text.toString().trim()

        val listaIngredientes = etPostIngredientes.text
            .toString()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val listaPasos = etPostProceso.text
            .toString()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (selectedMediaUri == null) {
            Toast.makeText(this, "Seleccione una imagen.", Toast.LENGTH_SHORT).show()
            return
        }

        if (Validaciones.campoVacio(nombre)) {
            etPostNombre.error = "Ingrese un nombre."
            return
        }

        if (Validaciones.campoVacio(descripcion)) {
            etPostDescripcion.error = "Ingrese una descripción."
            return
        }

        if (listaIngredientes.isEmpty()) {
            etPostIngredientes.error = "Ingrese al menos un ingrediente."
            return
        }

        if (listaPasos.isEmpty()) {
            etPostProceso.error = "Ingrese al menos un paso."
            return
        }

        btnGuardarPost.isEnabled = false

        Toast.makeText(
            this,
            "Publicando receta...",
            Toast.LENGTH_SHORT
        ).show()

        lifecycleScope.launch {

            //==========================
            // Imagen
            //==========================

            val bitmap = ajustarImagen(selectedMediaUri!!, 800)

            if (bitmap == null) {
                mostrarError("No fue posible procesar la imagen seleccionada.")
                return@launch
            }

            val stream = ByteArrayOutputStream()

            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                85,
                stream
            )

            val bytesImagen = stream.toByteArray()

            val nombreArchivo = "receta_${System.currentTimeMillis()}.jpg"

            val urlImagen = RecipeManager.subirImagen(
                nombreArchivo,
                bytesImagen
            )

            if (urlImagen == null) {
                mostrarError("No fue posible subir la imagen.")
                return@launch
            }

            //==========================
            // Receta
            //==========================

            val receta = Recipe(
                id = UUID.randomUUID().toString(),
                nombreUsuario = "${Sesion.usuario?.nombre} ${Sesion.usuario?.apellido}",
                usuarioId = Sesion.usuario?.id ?: "",
                nombre = nombre,
                descripcion = descripcion,
                imagenUrl = urlImagen
            )

            val recetaCreada = RecipeManager.crearReceta(receta)

            if (recetaCreada == null) {
                mostrarError("No fue posible guardar la receta.")
                return@launch
            }

            //==========================
            // Ingredientes
            //==========================

            for (ingrediente in listaIngredientes) {

                val ok = IngredientManager.crearIngrediente(

                    Ingredientes(
                        id = UUID.randomUUID().toString(),
                        recetaId = receta.id,
                        nombre = ingrediente,
                        cantidad = ""
                    )
                )

                if (!ok) {
                    mostrarError("No fue posible guardar los ingredientes.")
                    return@launch
                }
            }

            //==========================
            // Pasos
            //==========================

            for ((indice, paso) in listaPasos.withIndex()) {

                val ok = StepManager.crearPaso(

                    Pasos(
                        id = UUID.randomUUID().toString(),
                        recetaId = receta.id,
                        numero = indice + 1,
                        descripcion = paso
                    )
                )

                if (!ok) {
                    mostrarError("No fue posible guardar los pasos.")
                    return@launch
                }
            }

            Toast.makeText(
                this@CreateRecipeActivity,
                "Receta publicada correctamente.",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    private fun mostrarError(mensaje: String) {

        btnGuardarPost.isEnabled = true

        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun ajustarImagen(uri: Uri, max: Int): Bitmap? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            val ok = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (ok) abrirGaleria()
        }
    }
}