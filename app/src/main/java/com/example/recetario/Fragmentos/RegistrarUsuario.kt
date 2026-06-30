package com.example.recetario.Fragmentos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.recetario.Funciones.Authentication
import com.example.recetario.Funciones.Validaciones
import com.example.recetario.R

class RegistrarUsuario : Fragment() {

    private lateinit var txtNombre: EditText
    private lateinit var txtApellido: EditText
    private lateinit var txtCorreo: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtConfirmarPassword: EditText
    private lateinit var btnCrearUsuario: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.creacion_usario,
            container,
            false
        )

        txtNombre = view.findViewById(R.id.txtNombre)
        txtApellido = view.findViewById(R.id.txtApellido)
        txtCorreo = view.findViewById(R.id.txtCorreo)
        txtPassword = view.findViewById(R.id.txtPassword)
        txtConfirmarPassword = view.findViewById(R.id.txtConfirmarPassword)
        btnCrearUsuario = view.findViewById(R.id.btnCrearUsuario)

        btnCrearUsuario.setOnClickListener {

            if (!validarFormulario()) {
                return@setOnClickListener
            }

            btnCrearUsuario.isEnabled = false

            Authentication.crearUsuario(
                txtCorreo.text.toString().trim(),
                txtPassword.text.toString()
            ) { exito, mensaje ->

                btnCrearUsuario.isEnabled = true

                if (exito) {

                    // Firebase inicia sesión automáticamente al crear un usuario
                    Authentication.cerrarSesion()

                    Toast.makeText(
                        requireContext(),
                        "Usuario creado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()

                } else {

                    AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage(
                            mensaje ?: "No fue posible crear el usuario."
                        )
                        .setPositiveButton("Aceptar", null)
                        .show()
                }
            }
        }

        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Al presionar Atrás vuelve al fragmento anterior
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    private fun validarFormulario(): Boolean {

        val nombre = txtNombre.text.toString().trim()
        val apellido = txtApellido.text.toString().trim()
        val correo = txtCorreo.text.toString().trim()
        val password = txtPassword.text.toString()
        val confirmar = txtConfirmarPassword.text.toString()

        if (Validaciones.campoVacio(nombre)) {
            txtNombre.error = "Ingrese un nombre"
            txtNombre.requestFocus()
            return false
        }

        if (Validaciones.campoVacio(apellido)) {
            txtApellido.error = "Ingrese un apellido"
            txtApellido.requestFocus()
            return false
        }

        if (Validaciones.campoVacio(correo)) {
            txtCorreo.error = "Ingrese un correo"
            txtCorreo.requestFocus()
            return false
        }

        if (!Validaciones.correoValido(correo)) {
            txtCorreo.error = "Correo inválido"
            txtCorreo.requestFocus()
            return false
        }

        if (Validaciones.campoVacio(password)) {
            txtPassword.error = "Ingrese una contraseña"
            txtPassword.requestFocus()
            return false
        }

        if (!Validaciones.contraseñaValida(password)) {
            txtPassword.error =
                "Debe contener mínimo 8 caracteres, una letra, un número y un símbolo."
            txtPassword.requestFocus()
            return false
        }

        if (!Validaciones.contraseñasCoinciden(password, confirmar)) {
            txtConfirmarPassword.error = "Las contraseñas no coinciden"
            txtConfirmarPassword.requestFocus()
            return false
        }

        return true
    }
}