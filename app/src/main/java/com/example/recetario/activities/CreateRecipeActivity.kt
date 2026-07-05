package com.example.recetario.activities

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.LinearLayout
import android.view.Gravity
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recetario.R
import com.example.recetario.utils.SystemBarUtils
import com.example.recetario.data.IngredientManager
import com.example.recetario.data.RecipeManager
import com.example.recetario.data.StepManager
import com.example.recetario.model.Ingredient
import com.example.recetario.model.Recipe
import com.example.recetario.model.Step
import com.example.recetario.utils.NavigationHelper
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.PermissionManager
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.ValidationUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.min

class CreateRecipeActivity : AppCompatActivity() {

    private var selectedMediaUri: Uri? = null
    private var recetaEnEdicion: Recipe? = null

    private lateinit var lblNuevaReceta: TextView
    private lateinit var tvMediaHint: TextView
    private lateinit var btnSelectMedia: Button
    private lateinit var imagePreview: ImageView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var etPostNombre: TextInputEditText
    private lateinit var etPostDescripcion: TextInputEditText
    private lateinit var contenedorIngredientes: LinearLayout
    private lateinit var contenedorPasos: LinearLayout
    private lateinit var btnAgregarIngrediente: Button
    private lateinit var btnAgregarPaso: Button
    private lateinit var btnCancelarPost: Button
    private lateinit var btnGuardarPost: Button

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
        setContentView(R.layout.activity_create_recipe)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootCreateRecipe))

        inicializarVistas()
        configurarNavigationHelper()
        cargarModoFormulario()
        configurarEventos()
    }

    private fun inicializarVistas() {
        lblNuevaReceta = findViewById(R.id.lblNuevaReceta)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnSelectMedia = findViewById(R.id.btnPostImagen)
        tvMediaHint = findViewById(R.id.tvMediaHint)
        imagePreview = findViewById(R.id.imagePreview)
        etPostNombre = findViewById(R.id.etPostNombre)
        etPostDescripcion = findViewById(R.id.etPostDescripcion)
        contenedorIngredientes = findViewById(R.id.contenedorIngredientes)
        contenedorPasos = findViewById(R.id.contenedorPasos)
        btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente)
        btnAgregarPaso = findViewById(R.id.btnAgregarPaso)
        btnCancelarPost = findViewById(R.id.btnCancelarPost)
        btnGuardarPost = findViewById(R.id.btnGuardarPost)
    }

    private fun configurarEventos() {
        btnSelectMedia.setOnClickListener {
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

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.volverARecetas(this@CreateRecipeActivity)
        }
    }

    private fun configurarNavigationHelper() {
        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> NavigationHelper.irRecetas(this)
                R.id.nav_add -> true
                R.id.nav_perfil -> NavigationHelper.irPerfil(this)
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_add
    }

    /**
     * Permite reutilizar esta pantalla para crear o editar recetas.
     * Si llega una receta por Intent, el formulario entra en modo edición.
     */
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
            btnSelectMedia.text = "Cambiar imagen"
        }

        lifecycleScope.launch {
            val ingredientes = IngredientManager.obtenerIngredientes(receta.id)
            val pasos = StepManager.obtenerPasos(receta.id)

            cargarCamposDinamicos(
                valores = ingredientes.map { it.nombre },
                esIngrediente = true
            )

            cargarCamposDinamicos(
                valores = pasos.map { it.descripcion },
                esIngrediente = false
            )
        }
    }

    /**
     * Carga los campos dinámicos cuando se edita una receta existente.
     */
    private fun cargarCamposDinamicos(valores: List<String>, esIngrediente: Boolean) {
        if (esIngrediente) {
            contenedorIngredientes.removeAllViews()
            camposIngredientes.clear()
            if (valores.isEmpty()) agregarCampoIngrediente()
            valores.forEach { agregarCampoIngrediente(it) }
        } else {
            contenedorPasos.removeAllViews()
            camposPasos.clear()
            if (valores.isEmpty()) agregarCampoPaso()
            valores.forEach { agregarCampoPaso(it) }
        }
    }

    private fun agregarCampoIngrediente(valorInicial: String = "") {
        agregarCampoDinamico(
            contenedor = contenedorIngredientes,
            campos = camposIngredientes,
            hint = "Ingrediente ${camposIngredientes.size + 1}",
            valorInicial = valorInicial
        )
    }

    private fun agregarCampoPaso(valorInicial: String = "") {
        agregarCampoDinamico(
            contenedor = contenedorPasos,
            campos = camposPasos,
            hint = "Paso ${camposPasos.size + 1}",
            valorInicial = valorInicial
        )
    }

    /**
     * Crea una nueva fila editable. Cada fila tiene su propio TextInput y un botón
     * para eliminarla, similar a un formulario dinámico de inventario.
     */
    private fun agregarCampoDinamico(
        contenedor: LinearLayout,
        campos: MutableList<TextInputEditText>,
        hint: String,
        valorInicial: String = ""
    ) {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8.dp() }
        }

        val inputLayout = TextInputLayout(
            this,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val editText = TextInputEditText(inputLayout.context).apply {
            setText(valorInicial)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            maxLines = 2
        }

        val btnEliminar = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "−"
            textSize = 20f
            contentDescription = "Eliminar campo"
            minWidth = 0
            minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(52.dp(), 56.dp()).apply {
                marginStart = 8.dp()
            }
            setOnClickListener {
                if (campos.size > 1) {
                    campos.remove(editText)
                    contenedor.removeView(fila)
                } else {
                    editText.text?.clear()
                    editText.requestFocus()
                }
            }
        }

        inputLayout.addView(editText)
        fila.addView(inputLayout)
        fila.addView(btnEliminar)
        contenedor.addView(fila)
        campos.add(editText)
    }

    private fun obtenerValoresCampos(campos: List<TextInputEditText>): List<String> {
        return campos.map { it.text.toString().trim() }.filter { it.isNotBlank() }
    }

    private fun marcarPrimerCampoVacio(campos: List<TextInputEditText>, mensaje: String) {
        val campo = campos.firstOrNull { it.text.toString().trim().isBlank() } ?: campos.firstOrNull()
        campo?.error = mensaje
        campo?.requestFocus()
        mostrarToast(mensaje)
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
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

    private fun abrirGaleria() {
        pickMedia.launch("image/*")
    }

    private fun mostrarVistaPrevia(uri: Uri) {
        tvMediaHint.visibility = View.GONE
        btnSelectMedia.text = "Cambiar imagen"
        imagePreview.visibility = View.VISIBLE

        val bitmap = ajustarImagen(uri, 900)
        if (bitmap != null) {
            imagePreview.setImageBitmap(bitmap)
        } else {
            imagePreview.load(uri)
        }
    }

    /**
     * Valida la información del formulario antes de subir imagen, receta,
     * ingredientes y pasos al almacenamiento remoto.
     */
    private fun validarYGuardarReceta() {
        val nombre = etPostNombre.text.toString().trim()
        val descripcion = etPostDescripcion.text.toString().trim()
        val listaIngredientes = obtenerValoresCampos(camposIngredientes)
        val listaPasos = obtenerValoresCampos(camposPasos)
        val esEdicion = recetaEnEdicion != null

        if (!esEdicion && selectedMediaUri == null) {
            mostrarToast("Seleccione una imagen para la receta.")
            return
        }

        if (ValidationUtils.campoVacio(nombre)) {
            etPostNombre.error = "Ingrese el nombre de la receta"
            etPostNombre.requestFocus()
            return
        }

        if (ValidationUtils.campoVacio(descripcion)) {
            etPostDescripcion.error = "Ingrese una descripción"
            etPostDescripcion.requestFocus()
            return
        }

        if (listaIngredientes.isEmpty()) {
            marcarPrimerCampoVacio(camposIngredientes, "Ingrese al menos un ingrediente")
            return
        }

        if (listaPasos.isEmpty()) {
            marcarPrimerCampoVacio(camposPasos, "Ingrese al menos un paso")
            return
        }

        if (!NetworkUtils.hayConexion(this)) {
            mostrarToast("Sin conexión. Intente nuevamente.")
            return
        }

        btnGuardarPost.isEnabled = false
        mostrarToast(if (esEdicion) "Actualizando receta..." else "Publicando receta...")

        lifecycleScope.launch {
            try {
                guardarReceta(nombre, descripcion, listaIngredientes, listaPasos)
            } catch (e: Exception) {
                e.printStackTrace()
                mostrarError("Ocurrió un error inesperado al guardar la receta.")
            }
        }
    }

    private suspend fun guardarReceta(
        nombre: String,
        descripcion: String,
        listaIngredientes: List<String>,
        listaPasos: List<String>
    ) {
        val recetaBase = recetaEnEdicion
        val urlImagen = obtenerUrlImagen(recetaBase?.imagenUrl.orEmpty())

        if (urlImagen == null) {
            mostrarError("No fue posible subir la imagen.")
            return
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val usuarioSesion = SessionManager.usuario
        val receta = Recipe(
            id = recetaBase?.id ?: UUID.randomUUID().toString(),
            usuarioId = recetaBase?.usuarioId ?: firebaseUser?.uid.orEmpty(),
            nombreUsuario = recetaBase?.nombreUsuario
                ?: "${usuarioSesion?.nombre.orEmpty()} ${usuarioSesion?.apellido.orEmpty()}".trim(),
            nombre = nombre,
            descripcion = descripcion,
            imagenUrl = urlImagen,
            fechaCreacion = recetaBase?.fechaCreacion.orEmpty()
        )

        val guardado = if (recetaBase == null) {
            RecipeManager.crearReceta(receta) != null
        } else {
            RecipeManager.actualizarReceta(receta)
        }

        if (!guardado) {
            mostrarError("No fue posible guardar la receta.")
            return
        }

        if (recetaBase != null) {
            IngredientManager.eliminarIngredientes(receta.id)
            StepManager.eliminarPasos(receta.id)
        }

        if (!guardarIngredientes(receta.id, listaIngredientes)) return
        if (!guardarPasos(receta.id, listaPasos)) return

        mostrarToast(
            if (recetaBase == null) "Receta publicada correctamente."
            else "Receta actualizada correctamente."
        )

        NavigationHelper.volverARecetas(this)
    }

    private suspend fun obtenerUrlImagen(urlActual: String): String? {
        val uri = selectedMediaUri ?: return urlActual.ifBlank { null }
        val bitmap = ajustarImagen(uri, 900) ?: return null

        val bytesImagen = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        }

        val nombreArchivo = if (recetaEnEdicion == null) {
            "receta_${System.currentTimeMillis()}.jpg"
        } else {
            "receta_${recetaEnEdicion!!.id}.jpg"
        }

        return RecipeManager.subirImagen(nombreArchivo, bytesImagen)
    }

    private suspend fun guardarIngredientes(recetaId: String, ingredientes: List<String>): Boolean {
        for (ingrediente in ingredientes) {
            val ok = IngredientManager.crearIngrediente(
                Ingredient(
                    id = UUID.randomUUID().toString(),
                    recetaId = recetaId,
                    nombre = ingrediente,
                    cantidad = ""
                )
            )

            if (!ok) {
                mostrarError("No fue posible guardar los ingredientes.")
                return false
            }
        }
        return true
    }

    private suspend fun guardarPasos(recetaId: String, pasos: List<String>): Boolean {
        for ((indice, paso) in pasos.withIndex()) {
            val ok = StepManager.crearPaso(
                Step(
                    id = UUID.randomUUID().toString(),
                    recetaId = recetaId,
                    numero = indice + 1,
                    descripcion = paso
                )
            )

            if (!ok) {
                mostrarError("No fue posible guardar los pasos.")
                return false
            }
        }
        return true
    }

    private fun obtenerLineas(texto: String): List<String> {
        return texto.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun mostrarError(mensaje: String) {
        btnGuardarPost.isEnabled = true

        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun ajustarImagen(uri: Uri, maxSize: Int): Bitmap? {
        return try {
            val bitmapOriginal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            val ratio = min(
                maxSize.toFloat() / bitmapOriginal.width,
                maxSize.toFloat() / bitmapOriginal.height
            ).coerceAtMost(1f)

            val nuevoAncho = (bitmapOriginal.width * ratio).toInt()
            val nuevoAlto = (bitmapOriginal.height * ratio).toInt()

            Bitmap.createScaledBitmap(bitmapOriginal, nuevoAncho, nuevoAlto, true)
        } catch (e: Exception) {
            e.printStackTrace()
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

            if (ok) {
                abrirGaleria()
            } else {
                mostrarToast("El permiso de imágenes es necesario para seleccionar la foto de la receta.")
            }
        }
    }
}
