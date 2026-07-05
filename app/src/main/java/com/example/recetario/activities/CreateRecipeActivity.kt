package com.example.recetario.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recetario.R
import com.example.recetario.data.IngredientManager
import com.example.recetario.data.RecipeManager
import com.example.recetario.data.StepManager
import com.example.recetario.model.Ingredient
import com.example.recetario.model.Recipe
import com.example.recetario.model.Step
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NavigationHelper
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.PermissionManager
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.SystemBarUtils
import com.example.recetario.utils.ValidationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.min

class CreateRecipeActivity : AppCompatActivity() {

    private var selectedMediaUri: Uri? = null
    private var recetaEnEdicion: Recipe? = null

    private lateinit var navigationBar: NavigationBarView
    private lateinit var lblNuevaReceta: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var tvMediaHint: TextView
    private lateinit var btnPostImagen: MaterialButton
    private lateinit var etPostNombre: TextInputEditText
    private lateinit var etPostDescripcion: TextInputEditText
    private lateinit var contenedorIngredientes: LinearLayout
    private lateinit var contenedorPasos: LinearLayout
    private lateinit var btnAgregarIngrediente: Button
    private lateinit var btnAgregarPaso: Button
    private lateinit var btnGuardarPost: Button
    private lateinit var btnCancelarPost: Button

    private val camposIngredientes = mutableListOf<TextInputEditText>()
    private val camposPasos = mutableListOf<TextInputEditText>()
    private val permissionManager = PermissionManager(this)

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            mostrarVistaPrevia(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthManager.obtenerUsuario() == null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_create_recipe)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootCreateRecipe))

        inicializarVistas()
        configurarNavegacion()
        configurarEventos() // Ahora se configura antes para evitar que el 'return' lo bloquee
        cargarModoFormulario()

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.volverARecetas(this@CreateRecipeActivity)
        }
    }

    private fun inicializarVistas() {
        navigationBar = findViewById(R.id.bottomNavigation)
        lblNuevaReceta = findViewById(R.id.lblNuevaReceta)
        imagePreview = findViewById(R.id.imagePreview)
        tvMediaHint = findViewById(R.id.tvMediaHint)
        btnPostImagen = findViewById(R.id.btnPostImagen)
        etPostNombre = findViewById(R.id.etPostNombre)
        etPostDescripcion = findViewById(R.id.etPostDescripcion)
        contenedorIngredientes = findViewById(R.id.contenedorIngredientes)
        contenedorPasos = findViewById(R.id.contenedorPasos)
        btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente)
        btnAgregarPaso = findViewById(R.id.btnAgregarPaso)
        btnGuardarPost = findViewById(R.id.btnGuardarPost)
        btnCancelarPost = findViewById(R.id.btnCancelarPost)
    }

    private fun configurarEventos() {
        btnPostImagen.setOnClickListener {
            if (permissionManager.permisosMultimedia(100)) {
                abrirGaleria()
            }
        }

        btnAgregarIngrediente.setOnClickListener {
            agregarCampoIngrediente()
        }

        btnAgregarPaso.setOnClickListener {
            agregarCampoPaso()
        }

        btnCancelarPost.setOnClickListener {
            NavigationHelper.volverARecetas(this)
        }

        btnGuardarPost.setOnClickListener {
            validarYGuardarReceta()
        }
    }

    private fun configurarNavegacion() {
        navigationBar.selectedItemId = R.id.nav_add
        navigationBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> NavigationHelper.irRecetas(this)
                R.id.nav_add -> true
                R.id.nav_perfil -> NavigationHelper.irPerfil(this)
                else -> false
            }
        }
    }

    private fun cargarModoFormulario() {
        recetaEnEdicion = obtenerRecetaDesdeIntent()
        val receta = recetaEnEdicion

        if (receta == null) {
            lblNuevaReceta.text = "Nueva receta"
            btnGuardarPost.text = "Publicar receta"
            agregarCampoIngrediente()
            agregarCampoPaso()
            return
        }

        lblNuevaReceta.text = "Editar receta"
        btnGuardarPost.text = "Guardar cambios"
        etPostNombre.setText(receta.nombre)
        etPostDescripcion.setText(receta.descripcion)

        if (receta.imagenUrl.isNotBlank()) {
            tvMediaHint.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            imagePreview.load(receta.imagenUrl)
            btnPostImagen.text = "Cambiar imagen"
        }

        lifecycleScope.launch {
            val ingredientes = IngredientManager.obtenerIngredientes(receta.id)
            val pasos = StepManager.obtenerPasos(receta.id)

            contenedorIngredientes.removeAllViews()
            camposIngredientes.clear()
            if (ingredientes.isEmpty()) {
                agregarCampoIngrediente()
            } else {
                ingredientes.forEach { agregarCampoIngrediente(it.nombre) }
            }

            contenedorPasos.removeAllViews()
            camposPasos.clear()
            if (pasos.isEmpty()) {
                agregarCampoPaso()
            } else {
                pasos.forEach { agregarCampoPaso(it.descripcion) }
            }
        }
    }

    private fun agregarCampoIngrediente(valorInicial: String = "") {
        agregarCampoDinamico(
            contenedorIngredientes, 
            camposIngredientes, 
            "Ingrediente", 
            valorInicial
        )
    }

    private fun agregarCampoPaso(valorInicial: String = "") {
        agregarCampoDinamico(
            contenedorPasos, 
            camposPasos, 
            "Paso", 
            valorInicial
        )
    }

    private fun agregarCampoDinamico(
        contenedor: LinearLayout, 
        campos: MutableList<TextInputEditText>, 
        prefix: String, 
        valorInicial: String
    ) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_dynamic_field, contenedor, false)
        
        val inputLayout = view.findViewById<TextInputLayout>(R.id.inputLayout)
        val editText = view.findViewById<TextInputEditText>(R.id.editText)
        val btnEliminar = view.findViewById<MaterialButton>(R.id.btnEliminar)

        inputLayout.hint = "$prefix ${campos.size + 1}"
        editText.setText(valorInicial)
        
        btnEliminar.setOnClickListener {
            if (campos.size > 1) {
                campos.remove(editText)
                contenedor.removeView(view)
                actualizarHints(campos, prefix)
            } else {
                editText.text?.clear()
            }
        }

        contenedor.addView(view)
        campos.add(editText)
    }

    private fun actualizarHints(campos: List<TextInputEditText>, prefix: String) {
        campos.forEachIndexed { index, editText ->
            val layout = editText.parent.parent as? TextInputLayout
            layout?.hint = "$prefix ${index + 1}"
        }
    }

    private fun abrirGaleria() {
        pickMedia.launch("image/*")
    }

    private fun mostrarVistaPrevia(uri: Uri) {
        tvMediaHint.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        btnPostImagen.text = "Cambiar imagen"
        val bitmap = ajustarImagen(uri, 900)
        if (bitmap != null) imagePreview.setImageBitmap(bitmap)
        else imagePreview.load(uri)
    }

    private fun validarYGuardarReceta() {
        val nombre = etPostNombre.text.toString().trim()
        val descripcion = etPostDescripcion.text.toString().trim()
        val ingredientes = camposIngredientes.map { it.text.toString().trim() }.filter { it.isNotBlank() }
        val pasos = camposPasos.map { it.text.toString().trim() }.filter { it.isNotBlank() }

        if (recetaEnEdicion == null && selectedMediaUri == null) {
            Toast.makeText(this, "Selecciona una imagen", Toast.LENGTH_SHORT).show()
            return
        }
        if (nombre.isEmpty()) { etPostNombre.error = "Falta el nombre"; return }
        if (ingredientes.isEmpty()) { Toast.makeText(this, "Añade al menos un ingrediente", Toast.LENGTH_SHORT).show(); return }
        if (pasos.isEmpty()) { Toast.makeText(this, "Añade al menos un paso", Toast.LENGTH_SHORT).show(); return }

        if (!NetworkUtils.hayConexion(this)) {
            Toast.makeText(this, "Sin conexión", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardarPost.isEnabled = false
        Toast.makeText(this, "Guardando receta...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val urlImagen = obtenerUrlImagen()
                if (urlImagen == null) {
                    Toast.makeText(this@CreateRecipeActivity, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                    btnGuardarPost.isEnabled = true
                    return@launch
                }

                val receta = Recipe(
                    id = recetaEnEdicion?.id ?: UUID.randomUUID().toString(),
                    usuarioId = recetaEnEdicion?.usuarioId ?: AuthManager.obtenerUsuario()?.uid.orEmpty(),
                    nombreUsuario = recetaEnEdicion?.nombreUsuario ?: "${SessionManager.usuario?.nombre} ${SessionManager.usuario?.apellido}",
                    nombre = nombre,
                    descripcion = descripcion,
                    imagenUrl = urlImagen
                )

                val exito = if (recetaEnEdicion == null) RecipeManager.crearReceta(receta) != null else RecipeManager.actualizarReceta(receta)

                if (exito) {
                    if (recetaEnEdicion != null) {
                        IngredientManager.eliminarIngredientes(receta.id)
                        StepManager.eliminarPasos(receta.id)
                    }
                    ingredientes.forEach { IngredientManager.crearIngrediente(Ingredient(UUID.randomUUID().toString(), receta.id, it, "")) }
                    pasos.forEachIndexed { i, s -> StepManager.crearPaso(Step(UUID.randomUUID().toString(), receta.id, i + 1, s)) }
                    
                    Toast.makeText(this@CreateRecipeActivity, "¡Receta guardada!", Toast.LENGTH_SHORT).show()
                    NavigationHelper.volverARecetas(this@CreateRecipeActivity)
                } else {
                    Toast.makeText(this@CreateRecipeActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                    btnGuardarPost.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                btnGuardarPost.isEnabled = true
            }
        }
    }

    private suspend fun obtenerUrlImagen(): String? {
        val uri = selectedMediaUri ?: return recetaEnEdicion?.imagenUrl
        val bitmap = ajustarImagen(uri, 900) ?: return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val nombre = "receta_${System.currentTimeMillis()}.jpg"
        return RecipeManager.subirImagen(nombre, stream.toByteArray())
    }

    private fun ajustarImagen(uri: Uri, maxSize: Int): Bitmap? {
        return try {
            val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            val ratio = min(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height).coerceAtMost(1f)
            Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
        } catch (e: Exception) { null }
    }

    private fun obtenerRecetaDesdeIntent(): Recipe? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_RECETA_EDITAR", Recipe::class.java)
                ?: intent.getParcelableExtra("EXTRA_RECETA", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("EXTRA_RECETA_EDITAR")
                ?: intent.getParcelableExtra("EXTRA_RECETA")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            abrirGaleria()
        }
    }
}
