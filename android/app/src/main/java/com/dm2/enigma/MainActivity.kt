package com.dm2.enigma

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var archivoTxtSeleccionado: Uri? = null
    private var archivoEncSeleccionado: Uri? = null

    private var pantallaActual = "menu"

    private val REGEX_CONTRASENA = Regex("[A-Za-z0-9]{16}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (findViewById<Button>(R.id.btnAesImagenes) == null) {
                    mostrarMenu()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState != null) {
            // Recuperar datos
            pantallaActual = savedInstanceState.getString("pantalla_actual", "menu")

            archivoTxtSeleccionado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("uri_txt", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("uri_txt")
            }

            archivoEncSeleccionado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("uri_enc", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("uri_enc")
            }

            // Restaurar la pantalla correcta
            when (pantallaActual) {
                "aes" -> mostrarAes(savedInstanceState.getString("texto_contrasena") ?: "")
                "cesar" -> mostrarCesar(savedInstanceState.getString("texto_desplazamiento") ?: "")
                else -> mostrarMenu()
            }
        } else {
            mostrarMenu()
        }
    }

    // ---------------------------------------------------------------
    //  Pantallas
    // ---------------------------------------------------------------
    fun mostrarMenu() {
        pantallaActual = "menu"
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnAesImagenes).setOnClickListener { mostrarAes() }
        findViewById<Button>(R.id.btnCesarTexto).setOnClickListener { mostrarCesar() }
    }

    private fun mostrarAes(textoRecuperado: String = "") {
        pantallaActual = "aes"
        setContentView(R.layout.activity_aes)

        findViewById<Button>(R.id.btnVolver).setOnClickListener { mostrarMenu() }
        findViewById<Button>(R.id.btnImportarEnc).setOnClickListener { abrirSelectorEnc() }

        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val btnMostrar = findViewById<ImageButton>(R.id.btnMostrarContrasena)

        // Restaurar el texto guardado
        etContrasena.setText(textoRecuperado)

        // --- Validación al pulsar Descifrar ---
        findViewById<Button>(R.id.btnDescifrarAes).setOnClickListener {
            val pass = etContrasena.text.toString()   // ← leer AQUÍ, no fuera
            if (!esContrasenaValida(pass)) {
                Toast.makeText(
                    this,
                    "Formato: 16 caracteres (letras y números)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            // ... aquí continúa el descifrado real
            Toast.makeText(this, "Contraseña válida", Toast.LENGTH_SHORT).show()
        }

        // --- Toggle mostrar / ocultar contraseña ---
        var visible = false
        btnMostrar.setOnClickListener {
            visible = !visible

            val cursor = etContrasena.selectionEnd

            if (visible) {
                etContrasena.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnMostrar.setImageResource(R.drawable.ic_ojo_cerrado)
                etContrasena.typeface = android.graphics.Typeface.DEFAULT
                btnMostrar.contentDescription = "Ocultar contraseña"
            } else {
                etContrasena.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnMostrar.setImageResource(R.drawable.ic_ojo_abierto)
                etContrasena.typeface = android.graphics.Typeface.DEFAULT
                btnMostrar.contentDescription = "Mostrar contraseña"
            }

            etContrasena.setSelection(cursor.coerceAtMost(etContrasena.text.length))
        }
    }

    private fun mostrarCesar(textoRecuperado: String = "") {
        pantallaActual = "cesar"
        setContentView(R.layout.activity_cesar)

        findViewById<Button>(R.id.btnVolver)?.setOnClickListener { mostrarMenu() }
        findViewById<Button>(R.id.btnImportarTxt).setOnClickListener { abrirSelectorTxt() }

        val etDesplazamiento = findViewById<EditText>(R.id.etDesplazamiento)

        // Restaurar el texto guardado
        etDesplazamiento.setText(textoRecuperado)

        etDesplazamiento.filters = arrayOf(
            InputFilter { source, start, end, dest, dstart, dend ->
                val texto = dest.toString().substring(0, dstart) +
                        source.toString().substring(start, end) +
                        dest.toString().substring(dend)

                if (texto.isEmpty()) return@InputFilter null

                val numero = texto.toIntOrNull() ?: return@InputFilter ""

                if (numero in 1..25) null else ""
            }
        )
    }

    // ---------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Guardar en qué pantalla está el usuario
        outState.putString("pantalla_actual", pantallaActual)

        // Guardar el texto escrito en las cajas
        if (pantallaActual == "aes") {
            outState.putString("texto_contrasena", findViewById<EditText>(R.id.etContrasena)?.text.toString())
        } else if (pantallaActual == "cesar") {
            outState.putString("texto_desplazamiento", findViewById<EditText>(R.id.etDesplazamiento)?.text.toString())
        }

        // Guardar los archivos seleccionados (las URIs)
        outState.putParcelable("uri_txt", archivoTxtSeleccionado)
        outState.putParcelable("uri_enc", archivoEncSeleccionado)
    }

    private val selectorTxt =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            archivoTxtSeleccionado = uri
            Toast.makeText(this, "Archivo TXT seleccionado", Toast.LENGTH_SHORT).show()
        }

    private val selectorEnc =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            val nombre = obtenerNombreArchivo(uri)
            if (!nombre.lowercase().endsWith(".enc")) {
                Toast.makeText(this, "Selecciona un archivo con extensión .enc", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            archivoEncSeleccionado = uri
            Toast.makeText(this, "Archivo ENC seleccionado", Toast.LENGTH_SHORT).show()
        }

    private fun esContrasenaValida(pass: String): Boolean =
        REGEX_CONTRASENA.matches(pass)

    fun abrirSelectorTxt() {
        selectorTxt.launch(arrayOf("text/plain", "text/*"))
    }

    fun abrirSelectorEnc() {
        selectorEnc.launch(arrayOf("*/*"))
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "archivo"
        contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val indice = cursor.getColumnIndex("_display_name")
                if (indice >= 0) nombre = cursor.getString(indice)
            }
        }
        return nombre
    }
}