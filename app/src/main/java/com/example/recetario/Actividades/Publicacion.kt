package com.example.recetario.Actividades

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.recetario.Funciones.Navegacion
import com.example.recetario.Funciones.Permisos
import com.example.recetario.Funciones.Validaciones
import com.example.recetario.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText

class Publicacion : AppCompatActivity() {

    private var selectedMediaUri: Uri? = null
    private var tipoContenido = ""

    // Vistas multimedia
    private lateinit var tvMediaHint: TextView
    private lateinit var btnSelectMedia: Button
    private lateinit var imagePreview: ImageView
    private lateinit var bottomNavigation: BottomNavigationView

    // Nuevas vistas del formulario de recetas
    private lateinit var etPostNombre: TextInputEditText
    private lateinit var etPostDescripcion: TextInputEditText
    private lateinit var etPostIngredientes: TextInputEditText
    private lateinit var etPostProceso: TextInputEditText
    private lateinit var btnCancelarPost: Button
    private lateinit var btnGuardarPost: Button

    private val manejadorPermisos = Permisos(this)

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
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

        // Eventos de selección de imagen
        btnSelectMedia.setOnClickListener {
            if (manejadorPermisos.permisosMultimedia(100)) {
                abrirGaleria()
            }
        }

        // Evento Cancelar
        btnCancelarPost.setOnClickListener {
            finish()
        }

        // Evento Guardar / Publicar
        btnGuardarPost.setOnClickListener {
            validarYPublicar()
        }
    }

    private fun inicializarVistas() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnSelectMedia = findViewById(R.id.btnPostImagen)
        tvMediaHint = findViewById(R.id.tvMediaHint)
        imagePreview = findViewById(R.id.imagePreview)

        // Inicialización de los campos de texto según tus IDs del XML
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
                R.id.nav_añadir -> true // Ya estamos aquí
                R.id.nav_perfil -> Navegacion.irPerfil(this)
                else -> false
            }
        }
        // Asegura que el ícono correcto esté marcado en la barra inferior
        bottomNavigation.selectedItemId = R.id.nav_añadir
    }

    private fun abrirGaleria() {
        pickMedia.launch("image/*")
    }

    private fun validarYPublicar() {
        val nombre = etPostNombre.text.toString().trim()
        val descripcion = etPostDescripcion.text.toString().trim()
        val ingredientes = etPostIngredientes.text.toString().trim()
        val proceso = etPostProceso.text.toString().trim()

        // 1. Validar que se haya seleccionado una imagen
        if (selectedMediaUri == null) {
            Toast.makeText(this, "Por favor, selecciona una imagen para tu receta", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Validar campos usando tu clase auxiliar Validaciones
        if (Validaciones.campoVacio(nombre)) {
            etPostNombre.error = "El nombre es obligatorio"
            etPostNombre.requestFocus()
            return
        }

        if (Validaciones.campoVacio(descripcion)) {
            etPostDescripcion.error = "La descripción es obligatoria"
            etPostDescripcion.requestFocus()
            return
        }

        if (Validaciones.campoVacio(ingredientes)) {
            etPostIngredientes.error = "Debes ingresar los ingredientes"
            etPostIngredientes.requestFocus()
            return
        }

        if (Validaciones.campoVacio(proceso)) {
            etPostProceso.error = "Debes ingresar los pasos de preparación"
            etPostProceso.requestFocus()
            return
        }

        // Deshabilitar botón para evitar envíos dobles
        btnGuardarPost.isEnabled = false

        // TODO: Aquí disparas tu corrutina para Firebase Storage + Supabase
        Toast.makeText(this, "Publicando receta...", Toast.LENGTH_SHORT).show()

        // Simulación de éxito por el momento:
        // finish()
    }

    private fun ajustarImagen(uri: Uri, tamañoMaximo: Int): Bitmap? {
        return try {
            val bitmapOriginal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            val ancho = bitmapOriginal.width
            val alto = bitmapOriginal.height

            if (ancho <= tamañoMaximo && alto <= tamañoMaximo) {
                return bitmapOriginal
            }

            val escala = minOf(tamañoMaximo.toFloat() / ancho, tamañoMaximo.toFloat() / alto)
            val nuevoAncho = (ancho * escala).toInt()
            val nuevoAlto = (alto * escala).toInt()

            Bitmap.createScaledBitmap(bitmapOriginal, nuevoAncho, nuevoAlto, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val permisosAceptados = grantResults.isNotEmpty() && grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }

            if (permisosAceptados) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
                abrirGaleria()
            } else {
                val permisoPrincipal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permisoPrincipal)) {
                    manejadorPermisos.mostrarDialogoAjustes()
                } else {
                    Toast.makeText(this, "No se podrá subir imágenes si no concede los permisos", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}