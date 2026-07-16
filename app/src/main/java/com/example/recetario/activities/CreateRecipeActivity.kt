package com.example.recetario.activities

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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recetario.R
import com.example.recetario.data.IngredientManager
import com.example.recetario.data.LocalDraftManager
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
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

    private val ingredientRows = mutableListOf<IngredientRow>()
    private val stepRows = mutableListOf<StepRow>()
    private val permissionManager = PermissionManager(this)

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            mostrarVistaPrevia(uri)
        }
    }

    inner class IngredientRow(val view: View) {
        val etNombre: TextInputEditText = view.findViewById(R.id.etNombre)
        val etCantidad: TextInputEditText = view.findViewById(R.id.etCantidad)
        val etUnidad: AutoCompleteTextView = view.findViewById(R.id.etUnidad)
        val btnEliminar: MaterialButton = view.findViewById(R.id.btnEliminar)
    }

    inner class StepRow(val view: View) {
        val tvNumero: TextView = view.findViewById(R.id.tvNumeroPaso)
        val etDescripcion: TextInputEditText = view.findViewById(R.id.etDescripcion)
        val etTiempo: TextInputEditText = view.findViewById(R.id.etTiempoSeleccionado)
        val btnEliminar: MaterialButton = view.findViewById(R.id.btnEliminar)
        var totalSegundos: Int = 0
    }

    private data class RecipeFormData(
        val nombre: String,
        val descripcion: String,
        val ingredientes: List<Ingredient>,
        val pasos: List<Step>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!validarSesionActiva()) return

        setContentView(R.layout.activity_create_recipe)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootCreateRecipe))

        inicializarVistas()
        configurarNavegacion()
        configurarEventos()
        cargarModoFormulario()

        onBackPressedDispatcher.addCallback(this) {
            mostrarDialogoDeBorrador()
        }
    }

    private fun validarSesionActiva(): Boolean {
        if (AuthManager.obtenerUsuario() != null) return true

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        return false
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
        btnPostImagen.setOnClickListener { seleccionarImagen() }
        btnAgregarIngrediente.setOnClickListener { agregarCampoIngrediente() }
        btnAgregarPaso.setOnClickListener { agregarCampoPaso() }
        btnCancelarPost.setOnClickListener { mostrarDialogoDeBorrador() }
        btnGuardarPost.setOnClickListener { validarYGuardarReceta() }
    }

    private fun seleccionarImagen() {
        if (permissionManager.permisosMultimedia(REQUEST_MEDIA_PERMISSION)) {
            abrirGaleria()
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
            prepararFormularioNuevo()
            return
        }

        prepararFormularioEdicion(receta)
        cargarDetalleDeReceta(receta)
    }

    private fun prepararFormularioNuevo() {
        lblNuevaReceta.text = "Nueva receta"
        btnGuardarPost.text = "Publicar receta"
        agregarCampoIngrediente()
        agregarCampoPaso()
    }

    private fun prepararFormularioEdicion(receta: Recipe) {
        lblNuevaReceta.text = if (receta.estado == ESTADO_BORRADOR_LOCAL) "Editar borrador" else "Editar receta"
        btnGuardarPost.text = if (receta.estado == ESTADO_BORRADOR_LOCAL) "Publicar borrador" else "Guardar cambios"

        etPostNombre.setText(receta.nombre)
        etPostDescripcion.setText(receta.descripcion)
        mostrarImagenExistente(receta)
    }

    private fun mostrarImagenExistente(receta: Recipe) {
        if (receta.imagenUrl.isBlank()) return

        tvMediaHint.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        imagePreview.load(File(receta.imagenUrl).takeIf { it.exists() } ?: receta.imagenUrl)
        btnPostImagen.text = "Cambiar imagen"
    }

    private fun cargarDetalleDeReceta(receta: Recipe) {
        lifecycleScope.launch {
            val ingredientes = obtenerIngredientesDeEdicion(receta)
            val pasos = obtenerPasosDeEdicion(receta)

            pintarIngredientes(ingredientes)
            pintarPasos(pasos)
        }
    }

    private suspend fun obtenerIngredientesDeEdicion(receta: Recipe): List<Ingredient> {
        if (receta.estado != ESTADO_BORRADOR_LOCAL) {
            return IngredientManager.obtenerIngredientes(receta.id)
        }

        return LocalDraftManager.obtenerTodos(this)
            .find { it.recipe.id == receta.id }
            ?.ingredientes
            ?: emptyList()
    }

    private suspend fun obtenerPasosDeEdicion(receta: Recipe): List<Step> {
        if (receta.estado != ESTADO_BORRADOR_LOCAL) {
            return StepManager.obtenerPasos(receta.id)
        }

        return LocalDraftManager.obtenerTodos(this)
            .find { it.recipe.id == receta.id }
            ?.pasos
            ?: emptyList()
    }

    private fun pintarIngredientes(ingredientes: List<Ingredient>) {
        contenedorIngredientes.removeAllViews()
        ingredientRows.clear()

        if (ingredientes.isEmpty()) {
            agregarCampoIngrediente()
        } else {
            ingredientes.forEach { agregarCampoIngrediente(it) }
        }
    }

    private fun pintarPasos(pasos: List<Step>) {
        contenedorPasos.removeAllViews()
        stepRows.clear()

        if (pasos.isEmpty()) {
            agregarCampoPaso()
        } else {
            pasos.forEach { agregarCampoPaso(it) }
        }
    }

    private fun mostrarDialogoDeBorrador() {
        if (estaEditandoRecetaPublicada()) {
            mostrarDialogoDescartarCambios()
            return
        }

        mostrarDialogoGuardarBorrador()
    }

    private fun estaEditandoRecetaPublicada(): Boolean {
        return recetaEnEdicion != null && recetaEnEdicion?.estado != ESTADO_BORRADOR_LOCAL
    }

    private fun mostrarDialogoDescartarCambios() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Descartar cambios?")
            .setMessage("Si sales ahora, perderás los cambios realizados en esta receta publicada.")
            .setPositiveButton("Descartar") { _, _ -> NavigationHelper.volverARecetas(this@CreateRecipeActivity) }
            .setNegativeButton("Seguir editando") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun mostrarDialogoGuardarBorrador() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Qué deseas hacer con la receta?")
            .setMessage("Tienes cambios sin guardar. Puedes guardar un borrador para publicarlo luego o salir sin guardar.")
            .setPositiveButton("Guardar borrador (Local)") { _, _ -> guardarComoBorrador() }
            .setNegativeButton("Salir sin guardar") { _, _ -> NavigationHelper.volverARecetas(this@CreateRecipeActivity) }
            .setNeutralButton("Seguir editando") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun guardarComoBorrador() {
        val formData = obtenerDatosFormulario()
        if (!validarBorrador(formData)) return

        btnCancelarPost.isEnabled = false
        mostrarMensaje("Guardando en tu teléfono...")

        lifecycleScope.launch {
            guardarBorradorLocal(formData)
        }
    }

    private fun validarBorrador(formData: RecipeFormData): Boolean {
        if (formData.nombre.isNotEmpty()) return true

        mostrarMensaje("Escribe al menos el nombre para guardar el borrador")
        etPostNombre.error = "Falta el nombre"
        return false
    }

    private suspend fun guardarBorradorLocal(formData: RecipeFormData) {
        try {
            LocalDraftManager.guardarBorrador(
                this,
                crearRecetaBorrador(formData),
                formData.ingredientes,
                formData.pasos,
                selectedMediaUri
            )

            mostrarMensaje("Borrador guardado localmente")
            NavigationHelper.volverARecetas(this)
        } catch (e: Exception) {
            e.printStackTrace()
            btnCancelarPost.isEnabled = true
            mostrarMensaje("Error al guardar localmente")
        }
    }

    private fun crearRecetaBorrador(formData: RecipeFormData): Recipe {
        return Recipe(
            id = recetaEnEdicion?.id ?: UUID.randomUUID().toString(),
            usuarioId = AuthManager.obtenerUsuario()?.uid.orEmpty(),
            nombreUsuario = obtenerNombreUsuarioActual(),
            nombre = formData.nombre,
            descripcion = formData.descripcion,
            imagenUrl = recetaEnEdicion?.imagenUrl ?: "",
            estado = ESTADO_BORRADOR_LOCAL
        )
    }

    private fun agregarCampoIngrediente(ingrediente: Ingredient? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_ingredient_row, contenedorIngredientes, false)
        val row = IngredientRow(view)

        configurarSelectorUnidad(row)
        cargarIngredienteEnFila(row, ingrediente)
        configurarEliminacionIngrediente(row, view)

        contenedorIngredientes.addView(view)
        ingredientRows.add(row)
    }

    private fun configurarSelectorUnidad(row: IngredientRow) {
        val unidades = resources.getStringArray(R.array.unidades_medida)
        val adapterUnidades = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unidades)
        row.etUnidad.setAdapter(adapterUnidades)
    }

    private fun cargarIngredienteEnFila(row: IngredientRow, ingrediente: Ingredient?) {
        if (ingrediente == null) return

        row.etNombre.setText(ingrediente.nombre)
        row.etCantidad.setText(ingrediente.cantidad)
        row.etUnidad.setText(ingrediente.unidad, false)
    }

    private fun configurarEliminacionIngrediente(row: IngredientRow, view: View) {
        row.btnEliminar.setOnClickListener {
            if (ingredientRows.size > 1) {
                ingredientRows.remove(row)
                contenedorIngredientes.removeView(view)
            } else {
                limpiarFilaIngrediente(row)
            }
        }
    }

    private fun limpiarFilaIngrediente(row: IngredientRow) {
        row.etNombre.text?.clear()
        row.etCantidad.text?.clear()
        row.etUnidad.text?.clear()
    }

    private fun agregarCampoPaso(paso: Step? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_step_row, contenedorPasos, false)
        val row = StepRow(view)

        row.tvNumero.text = "${stepRows.size + 1}"
        cargarPasoEnFila(row, paso)
        configurarSelectorTiempo(row)
        configurarEliminacionPaso(row, view)

        contenedorPasos.addView(view)
        stepRows.add(row)
    }

    private fun cargarPasoEnFila(row: StepRow, paso: Step?) {
        if (paso == null) return

        row.etDescripcion.setText(paso.descripcion)
        row.totalSegundos = paso.tiempoSegundos
        actualizarTextoTiempo(row)
    }

    private fun configurarSelectorTiempo(row: StepRow) {
        row.etTiempo.setOnClickListener {
            mostrarPickerDuracion(row.totalSegundos) { nuevosSegundos ->
                row.totalSegundos = nuevosSegundos
                actualizarTextoTiempo(row)
            }
        }
    }

    private fun configurarEliminacionPaso(row: StepRow, view: View) {
        row.btnEliminar.setOnClickListener {
            if (stepRows.size > 1) {
                stepRows.remove(row)
                contenedorPasos.removeView(view)
                renumerarPasos()
            } else {
                limpiarFilaPaso(row)
            }
        }
    }

    private fun limpiarFilaPaso(row: StepRow) {
        row.etDescripcion.text?.clear()
        row.totalSegundos = 0
        actualizarTextoTiempo(row)
    }

    private fun actualizarTextoTiempo(row: StepRow) {
        val h = row.totalSegundos / 3600
        val m = (row.totalSegundos % 3600) / 60
        val s = row.totalSegundos % 60
        row.etTiempo.setText(String.format("%02d:%02d:%02d", h, m, s))
    }

    private fun mostrarPickerDuracion(segundosActuales: Int, onResult: (Int) -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_duration_picker, null)
        val pickerH = crearNumberPicker(view, R.id.pickerHours, 0, 23, segundosActuales / 3600)
        val pickerM = crearNumberPicker(view, R.id.pickerMinutes, 0, 59, (segundosActuales % 3600) / 60)
        val pickerS = crearNumberPicker(view, R.id.pickerSeconds, 0, 59, segundosActuales % 60)

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Aceptar") { _, _ ->
                val total = (pickerH.value * 3600) + (pickerM.value * 60) + pickerS.value
                onResult(total)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearNumberPicker(
        view: View,
        id: Int,
        min: Int,
        max: Int,
        value: Int
    ): NumberPicker {
        return view.findViewById<NumberPicker>(id).apply {
            minValue = min
            maxValue = max
            this.value = value
        }
    }

    private fun renumerarPasos() {
        stepRows.forEachIndexed { index, row ->
            row.tvNumero.text = "${index + 1}"
        }
    }

    private fun abrirGaleria() {
        pickMedia.launch("image/*")
    }

    private fun mostrarVistaPrevia(uri: Uri) {
        tvMediaHint.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        btnPostImagen.text = "Cambiar imagen"

        val bitmap = ajustarImagen(uri, IMAGE_MAX_SIZE)
        if (bitmap != null) {
            imagePreview.setImageBitmap(bitmap)
        } else {
            imagePreview.load(uri)
        }
    }

    private fun validarYGuardarReceta() {
        val formData = obtenerDatosFormulario()
        if (!validarPublicacion(formData)) return

        btnGuardarPost.isEnabled = false
        mostrarMensaje("Publicando receta...")

        lifecycleScope.launch {
            publicarReceta(formData)
        }
    }

    private fun obtenerDatosFormulario(): RecipeFormData {
        return RecipeFormData(
            nombre = etPostNombre.text.toString().trim(),
            descripcion = etPostDescripcion.text.toString().trim(),
            ingredientes = leerIngredientesFormulario(),
            pasos = leerPasosFormulario()
        )
    }

    private fun leerIngredientesFormulario(): List<Ingredient> {
        return ingredientRows
            .map { row ->
                Ingredient(
                    nombre = row.etNombre.text.toString().trim(),
                    cantidad = row.etCantidad.text.toString().trim(),
                    unidad = row.etUnidad.text.toString().trim()
                )
            }
            .filter { it.nombre.isNotBlank() }
    }

    private fun leerPasosFormulario(): List<Step> {
        return stepRows
            .mapIndexed { index, row ->
                Step(
                    numero = index + 1,
                    descripcion = row.etDescripcion.text.toString().trim(),
                    tiempoSegundos = row.totalSegundos
                )
            }
            .filter { it.descripcion.isNotBlank() }
    }

    private fun validarPublicacion(formData: RecipeFormData): Boolean {
        if (recetaEnEdicion == null && selectedMediaUri == null) {
            mostrarMensaje("Selecciona una imagen")
            return false
        }

        if (formData.nombre.isEmpty()) {
            etPostNombre.error = "Falta el nombre"
            return false
        }

        if (formData.ingredientes.isEmpty()) {
            mostrarMensaje("Añade al menos un ingrediente")
            return false
        }

        if (formData.pasos.isEmpty()) {
            mostrarMensaje("Añade al menos un paso")
            return false
        }

        if (!NetworkUtils.hayConexion(this)) {
            mostrarMensaje("Sin conexión")
            return false
        }

        return true
    }

    private suspend fun publicarReceta(formData: RecipeFormData) {
        try {
            val idBorradorLocal = recetaEnEdicion?.id
            val urlImagen = obtenerUrlImagen()

            if (urlImagen == null) {
                mostrarMensaje("Error al subir imagen")
                btnGuardarPost.isEnabled = true
                return
            }

            val receta = crearRecetaPublicada(formData, urlImagen)
            val exito = guardarCabeceraReceta(receta)

            if (!exito) {
                mostrarMensaje("Error al publicar")
                btnGuardarPost.isEnabled = true
                return
            }

            guardarDetalleReceta(receta.id, formData)
            eliminarBorradorSiAplica(idBorradorLocal)

            mostrarMensaje("¡Receta publicada!")
            NavigationHelper.volverARecetas(this)
        } catch (e: Exception) {
            e.printStackTrace()
            btnGuardarPost.isEnabled = true
            mostrarMensaje("Error al publicar")
        }
    }

    private fun crearRecetaPublicada(formData: RecipeFormData, urlImagen: String): Recipe {
        return Recipe(
            id = recetaEnEdicion?.id ?: UUID.randomUUID().toString(),
            usuarioId = AuthManager.obtenerUsuario()?.uid.orEmpty(),
            nombreUsuario = obtenerNombreUsuarioActual(),
            nombre = formData.nombre,
            descripcion = formData.descripcion,
            imagenUrl = urlImagen,
            estado = ESTADO_PUBLICADO
        )
    }

    private suspend fun guardarCabeceraReceta(receta: Recipe): Boolean {
        return if (recetaEnEdicion == null || recetaEnEdicion?.estado == ESTADO_BORRADOR_LOCAL) {
            RecipeManager.crearReceta(receta) != null
        } else {
            RecipeManager.actualizarReceta(receta)
        }
    }

    private suspend fun guardarDetalleReceta(recetaId: String, formData: RecipeFormData) {
        if (recetaEnEdicion != null && recetaEnEdicion?.estado != ESTADO_BORRADOR_LOCAL) {
            IngredientManager.eliminarIngredientes(recetaId)
            StepManager.eliminarPasos(recetaId)
        }

        formData.ingredientes.forEach { ingrediente ->
            ingrediente.recetaId = recetaId
            ingrediente.id = UUID.randomUUID().toString()
            IngredientManager.crearIngrediente(ingrediente)
        }

        formData.pasos.forEach { paso ->
            paso.recetaId = recetaId
            paso.id = UUID.randomUUID().toString()
            StepManager.crearPaso(paso)
        }
    }

    private suspend fun eliminarBorradorSiAplica(idBorradorLocal: String?) {
        if (recetaEnEdicion?.estado == ESTADO_BORRADOR_LOCAL && !idBorradorLocal.isNullOrBlank()) {
            LocalDraftManager.eliminarBorrador(this, idBorradorLocal)
        }
    }

    private suspend fun obtenerUrlImagen(): String? {
        if (selectedMediaUri != null) {
            val bitmap = ajustarImagen(selectedMediaUri!!, IMAGE_MAX_SIZE) ?: return null
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            val nuevaUrl =  RecipeManager.subirImagen("receta_${System.currentTimeMillis()}.jpg", stream.toByteArray())
            if (nuevaUrl != null) { eliminarImagenAnteriorSiAplica(nuevaUrl) }

            return nuevaUrl
        }

        if (recetaEnEdicion?.estado == ESTADO_BORRADOR_LOCAL && recetaEnEdicion?.imagenUrl?.isNotBlank() == true) {
            return subirImagenDeBorradorLocal(recetaEnEdicion!!.imagenUrl)
        }

        return recetaEnEdicion?.imagenUrl
    }

    private suspend fun eliminarImagenAnteriorSiAplica(nuevaUrl: String) {
        val imagenAnterior = recetaEnEdicion?.imagenUrl.orEmpty()

        if (imagenAnterior.isBlank()) return
        if (imagenAnterior == nuevaUrl) return
        if (!imagenAnterior.contains("/storage/v1/object/public/image/")) return

        RecipeManager.eliminarImagen(imagenAnterior)
    }

    private suspend fun subirImagenDeBorradorLocal(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null

        return RecipeManager.subirImagen(
            "receta_${System.currentTimeMillis()}.jpg",
            file.readBytes()
        )
    }

    private fun ajustarImagen(uri: Uri, maxSize: Int): Bitmap? {
        return try {
            val original = obtenerBitmapOriginal(uri)
            val ratio = min(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height).coerceAtMost(1f)
            Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt(),
                (original.height * ratio).toInt(),
                true
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun obtenerBitmapOriginal(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
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

    private fun obtenerNombreUsuarioActual(): String {
        val usuario = SessionManager.usuario ?: return AuthManager.obtenerUsuario()?.email.orEmpty()
        return "${usuario.nombre} ${usuario.apellido}".trim()
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_MEDIA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            abrirGaleria()
        }
    }

    companion object {
        private const val REQUEST_MEDIA_PERMISSION = 100
        private const val IMAGE_MAX_SIZE = 900
        private const val JPEG_QUALITY = 80
        private const val ESTADO_BORRADOR_LOCAL = "borrador_local"
        private const val ESTADO_PUBLICADO = "publicado"
    }
}