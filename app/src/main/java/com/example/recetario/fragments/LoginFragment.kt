package com.example.recetario.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.recetario.activities.MainActivity
import com.example.recetario.utils.ValidationUtils
import com.example.recetario.R
import com.example.recetario.utils.AuthManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.recetario.data.UserManager
import com.example.recetario.utils.SessionManager


class LoginFragment : Fragment() {

    private lateinit var txtUsuario: EditText
    private lateinit var txtPasswordLogin: EditText
    private lateinit var btnAcceder: Button
    private lateinit var btnCrearUsuario: Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_login,
            container,
            false
        )

        txtUsuario = view.findViewById(R.id.txtUsuario)
        txtPasswordLogin = view.findViewById(R.id.txtPasswordLogin)
        btnAcceder = view.findViewById(R.id.btnAcceder)
        btnCrearUsuario = view.findViewById(R.id.btnCrearUsuario)


        btnAcceder.setOnClickListener {
            iniciarSesion()
        }

        btnCrearUsuario.setOnClickListener {
            (requireActivity() as MainActivity).cambiarFragmento(
                RegisterFragment(),
                agregarAlBackStack = true,
                mostrarMenu = false
            )
        }

        return view
    }

    private fun iniciarSesion() {

        val correo = txtUsuario.text.toString().trim()
        val contraseña = txtPasswordLogin.text.toString()

        // Validar correo vacío
        if (ValidationUtils.campoVacio(correo)) {
            txtUsuario.error = "Ingrese un correo"
            txtUsuario.requestFocus()
            return
        }

        // Validar formato del correo
        if (!ValidationUtils.correoValido(correo)) {
            txtUsuario.error = "Correo inválido"
            txtUsuario.requestFocus()
            return
        }

        // Validar contraseña
        if (ValidationUtils.campoVacio(contraseña)) {
            txtPasswordLogin.error = "Ingrese una contraseña"
            txtPasswordLogin.requestFocus()
            return
        }

        // Evita múltiples clics
        btnAcceder.isEnabled = false

        AuthManager.iniciarSesion(
            correo,
            contraseña
        ) { exito, mensaje ->

            if (!exito) {

                btnAcceder.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    mensaje ?: "Correo o contraseña incorrectos",
                    Toast.LENGTH_LONG
                ).show()

                return@iniciarSesion
            }

            // Usuario autenticado en Firebase
            val firebaseUser = AuthManager.obtenerUsuario()

            if (firebaseUser == null) {

                btnAcceder.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    "No se pudo obtener el usuario autenticado.",
                    Toast.LENGTH_SHORT
                ).show()

                return@iniciarSesion
            }

            // Buscar los datos del usuario en Supabase
            viewLifecycleOwner.lifecycleScope.launch {

                val usuario = UserManager.obtenerUsuario(firebaseUser.uid.toString())

                btnAcceder.isEnabled = true

                if (usuario == null) {

                    Toast.makeText(
                        requireContext(),
                        "No se encontró el usuario en la base de datos.",
                        Toast.LENGTH_LONG
                    ).show()

                    AuthManager.cerrarSesion()
                    return@launch
                }

                // Guardar el usuario en memoria
                SessionManager.usuario = usuario

                Toast.makeText(
                    requireContext(),
                    "Bienvenido ${usuario.nombre}",
                    Toast.LENGTH_SHORT
                ).show()

                // Ir al fragmento principal
                (requireActivity() as MainActivity).cambiarFragmento(
                    HomeFragment(),
                    agregarAlBackStack = false,
                    mostrarMenu = true
                )
            }
        }
    }

}