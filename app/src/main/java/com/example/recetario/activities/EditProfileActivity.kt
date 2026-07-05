package com.example.recetario.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.view.ViewGroup
import android.widget.LinearLayout
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.request.CachePolicy
import com.example.recetario.R
import com.example.recetario.utils.SystemBarUtils
import com.example.recetario.utils.SquareCropImageView
import com.example.recetario.data.UserManager
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.PermissionManager
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.ValidationUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.min

class EditProfileActivity : AppCompatActivity() {

    private lateinit var imgFotoEditarPerfil: ImageView
    private lateinit var txtCambiarFoto: TextView
    private lateinit var etEditarNombre: TextInputEditText
    private lateinit var etEditarApellido: TextInputEditText
    private lateinit var btnGuardarCambios: Button
    private lateinit var btnIrCambiarContrasena: Button

    private val permissionManager = PermissionManager(this)
    private var fotoUriSeleccionada: Uri? = null
    private var fotoBitmapRecortada: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Validación de seguridad al rotar o iniciar
        if (AuthManager.obtenerUsuario() == null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_edit_profile)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootEditProfile))

        inicializarVistas()
        cargarDatosActuales()
        configurarEventos()
    }

    private fun inicializarVistas() {
        imgFotoEditarPerfil = findViewById(R.id.imgFotoEditarPerfil)
        txtCambiarFoto = findViewById(R.id.txtCambiarFoto)
        etEditarNombre = findViewById(R.id.etEditarNombre)
        etEditarApellido = findViewById(R.id.etEditarApellido)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)
        btnIrCambiarContrasena = findViewById(R.id.btnIrCambiarContrasena)
    }

    private fun cargarDatosActuales() {
        val usuario = SessionManager.usuario ?: return
        etEditarNombre.setText(usuario.nombre)
        etEditarApellido.setText(usuario.apellido)
        
        if (usuario.fotoPerfil.isNotBlank()) {
            imgFotoEditarPerfil.load(usuario.fotoPerfil) {
                crossfade(true)
                placeholder(android.R.drawable.sym_def_app_icon)
            }
        }
    }

    private fun configurarEventos() {
        txtCambiarFoto.setOnClickListener {
            if (permissionManager.permisosMultimedia(200)) {
                seleccionarFotoLauncher.launch("image/*")
            }
        }

        btnGuardarCambios.setOnClickListener {
            validarYActualizar()
        }

        btnIrCambiarContrasena.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }

    private val seleccionarFotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoUriSeleccionada = uri
            mostrarDialogoRecorte(uri)
        }
    }

    private fun mostrarDialogoRecorte(uri: Uri) {
        // 1. Cargar el Bitmap desde la Uri correctamente
        val bitmapOriginal = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    // Forzar asignador de software para que el Canvas pueda dibujar el bitmap en el Custom View
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (bitmapOriginal == null) {
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Usar los métodos reales de tu clase SquareCropImageView
        val cropImageView = SquareCropImageView(this)
        cropImageView.setImageBitmapForCrop(bitmapOriginal)

        AlertDialog.Builder(this)
            .setTitle("Recortar foto (1:1)")
            .setView(cropImageView)
            .setPositiveButton("Recortar") { _, _ ->
                // 3. Usar el método real obtenerBitmapRecortado() de SquareCropImageView
                val bitmapResult = cropImageView.obtenerBitmapRecortado()
                if (bitmapResult != null) {
                    fotoBitmapRecortada = bitmapResult
                    imgFotoEditarPerfil.setImageBitmap(bitmapResult)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validarYActualizar() {
        val nombre = etEditarNombre.text.toString().trim()
        val apellido = etEditarApellido.text.toString().trim()

        if (nombre.isEmpty()) {
            etEditarNombre.error = "Ingrese su nombre"
            return
        }

        if (!NetworkUtils.hayConexion(this)) {
            Toast.makeText(this, "Sin conexión", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardarPostState(false)

        lifecycleScope.launch {
            try {
                var urlFoto = SessionManager.usuario?.fotoPerfil ?: ""

                fotoBitmapRecortada?.let { bitmap ->
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    val bytes = stream.toByteArray()
                    val nombreArchivo = "perfil_${AuthManager.obtenerUsuario()?.uid}.jpg"
                    val nuevaUrl = UserManager.subirFotoPerfil(nombreArchivo, bytes)
                    if (nuevaUrl != null) urlFoto = nuevaUrl
                }

                val usuarioActualizado = SessionManager.usuario?.copy(
                    nombre = nombre,
                    apellido = apellido,
                    fotoPerfil = urlFoto
                )

                if (usuarioActualizado != null && UserManager.actualizarUsuario(usuarioActualizado)) {
                    SessionManager.usuario = usuarioActualizado
                    Toast.makeText(this@EditProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                    btnGuardarPostState(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                btnGuardarPostState(true)
            }
        }
    }

    private fun btnGuardarPostState(enabled: Boolean) {
        btnGuardarCambios.isEnabled = enabled
        btnGuardarCambios.text = if (enabled) "Guardar cambios" else "Actualizando..."
    }
}
