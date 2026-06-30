package com.example.recetario.Actividades

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.recetario.Funciones.Validaciones
import com.example.recetario.R
import com.google.android.material.textfield.TextInputEditText

class EditarPerfil : AppCompatActivity() {

    private lateinit var imgFotoEditarPerfil: ImageView
    private lateinit var txtCambiarFoto: TextView
    private lateinit var etEditarNombre: TextInputEditText
    private lateinit var etEditarApellido: TextInputEditText
    private lateinit var btnGuardarCambios: Button
    private var fotoUriSeleccionada: Uri? = null

    private val seleccionarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                fotoUriSeleccionada = uri
                imgFotoEditarPerfil.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editar_perfil)

        inicializarVistas()
        configurarBotonAtras()

        txtCambiarFoto.setOnClickListener { abrirGaleria() }
        imgFotoEditarPerfil.setOnClickListener { abrirGaleria() }

        btnGuardarCambios.setOnClickListener {
            validarYGuardar()
        }
    }

    private fun inicializarVistas() {
        imgFotoEditarPerfil = findViewById(R.id.imgFotoEditarPerfil)
        txtCambiarFoto = findViewById(R.id.txtCambiarFoto)
        etEditarNombre = findViewById(R.id.etEditarNombre)
        etEditarApellido = findViewById(R.id.etEditarApellido)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)
    }

    private fun configurarBotonAtras() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        seleccionarFotoLauncher.launch(intent)
    }

    private fun validarYGuardar() {
        if (Validaciones.campoVacio(etEditarNombre.text.toString()) || Validaciones.campoVacio(etEditarApellido.text.toString())) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardarCambios.isEnabled = false
    }
}