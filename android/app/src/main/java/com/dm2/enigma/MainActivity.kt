package com.dm2.enigma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dm2.enigma.Descifradores.DescifrarAes
import com.dm2.enigma.Descifradores.DescifrarCesar
import java.io.File

class MainActivity : AppCompatActivity() {

    private var pantallaActual = "menu"
    private var archivoTxtSeleccionado: Uri? = null
    private var archivoEncSeleccionado: Uri? = null
    private var textoDescifradoCesar = ""
    /** Temporal en caché con la imagen descifrada: solo vive mientras se ve la pantalla. */
    private var rutaImagenRecuperada: String? = null
    private var descifrandoAes = false
    private var generacion = 0
    private var selectorPendiente = false
    private var reiniciarAlEntrar = false
    private var aviso: AlertDialog? = null

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

        if (savedInstanceState == null) {
            mostrarMenu()
            return
        }

        pantallaActual = savedInstanceState.getString("pantalla_actual") ?: "menu"
        archivoTxtSeleccionado = savedInstanceState.uri("uri_txt")
        archivoEncSeleccionado = savedInstanceState.uri("uri_enc")

        when (pantallaActual) {
            "aes" -> {
                rutaImagenRecuperada = savedInstanceState.getString("ruta_imagen")
                mostrarAes(savedInstanceState.getString("texto_contrasena").orEmpty())
            }
            "cesar" -> {
                textoDescifradoCesar = savedInstanceState.getString("texto_descifrado_cesar").orEmpty()
                mostrarCesar(savedInstanceState.getString("texto_desplazamiento").orEmpty())
            }
            else -> mostrarMenu()
        }
    }

    private fun Bundle.uri(clave: String): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(clave, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(clave)
        }

    // ---------------------------------------------------------------
    //  Pantallas
    // ---------------------------------------------------------------

    fun mostrarMenu() {
        pantallaActual = "menu"
        limpiarEstado()
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnAesImagenes).setOnClickListener { mostrarAes() }
        findViewById<Button>(R.id.btnCesarTexto).setOnClickListener { mostrarCesar() }
    }

    /** Olvida archivo, contraseña y resultado: la pantalla se abre como la primera vez. */
    private fun limpiarEstado() {
        generacion++
        archivoTxtSeleccionado = null
        archivoEncSeleccionado = null
        textoDescifradoCesar = ""
        borrarImagenRecuperada()
        descifrandoAes = false
        aviso?.dismiss()
        aviso = null
    }

    override fun onStart() {
        super.onStart()
        if (!reiniciarAlEntrar) return
        reiniciarAlEntrar = false
        limpiarEstado()
        when (pantallaActual) {
            "aes" -> mostrarAes()
            "cesar" -> mostrarCesar()
        }
    }

    /** Solo girar la pantalla conserva el estado; al salir de la app o de la vista se reinicia. */
    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations || selectorPendiente) return
        reiniciarAlEntrar = true
        borrarImagenRecuperada()
    }

    override fun onDestroy() {
        if (isFinishing) borrarImagenRecuperada()
        super.onDestroy()
    }

    /** Borra la imagen descifrada: nunca queda nada guardado al salir de la ventana. */
    private fun borrarImagenRecuperada() {
        rutaImagenRecuperada?.let { File(it).delete() }
        rutaImagenRecuperada = null
        DescifrarAes.limpiarTemporales(this)
    }

    private fun mostrarAes(textoRecuperado: String = "") {
        pantallaActual = "aes"
        setContentView(R.layout.activity_aes)

        findViewById<Button>(R.id.btnVolver).setOnClickListener { mostrarMenu() }
        findViewById<Button>(R.id.btnImportarEnc).setOnClickListener { abrirSelectorEnc() }

        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        etContrasena.setText(textoRecuperado)

        val btnMostrar = findViewById<ImageButton>(R.id.btnMostrarContrasena)
        btnMostrar.setOnClickListener {
            val oculta = etContrasena.inputType ==
                (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            etContrasena.inputType = if (oculta) {
                btnMostrar.setImageResource(R.drawable.ic_ojo_cerrado)
                btnMostrar.contentDescription = "Ocultar contraseña"
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                btnMostrar.setImageResource(R.drawable.ic_ojo_abierto)
                btnMostrar.contentDescription = "Mostrar contraseña"
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etContrasena.setSelection(etContrasena.text.length)
        }

        findViewById<Button>(R.id.btnDescifrarAes).setOnClickListener {
            val uri = archivoEncSeleccionado
            val contrasena = etContrasena.text.toString()

            when {
                descifrandoAes -> Unit
                uri == null -> mostrarAviso(
                    "Archivo no seleccionado",
                    "Importa primero un archivo .enc generado por el cifrador AES."
                )
                !DescifrarAes.esContrasenaValida(contrasena) -> mostrarAviso(
                    "Contraseña no válida",
                    "La contraseña debe tener 16 caracteres A-Z, a-z o 0-9."
                )
                else -> iniciarDescifradoAes(uri, contrasena)
            }
        }

        archivoEncSeleccionado?.let { uri ->
            findViewById<TextView>(R.id.tvEstadoAes).apply {
                text = "Archivo: ${obtenerNombreArchivo(uri)}"
                visibility = View.VISIBLE
            }
        }

        rutaImagenRecuperada?.let { ruta ->
            val archivo = File(ruta)
            if (archivo.exists()) mostrarVistaPrevia(archivo) else rutaImagenRecuperada = null
        }
    }

    private fun mostrarCesar(textoRecuperado: String = "") {
        pantallaActual = "cesar"
        setContentView(R.layout.activity_cesar)

        findViewById<Button>(R.id.btnVolver).setOnClickListener { mostrarMenu() }
        findViewById<Button>(R.id.btnImportarTxt).setOnClickListener { abrirSelectorTxt() }

        val etDesplazamiento = findViewById<EditText>(R.id.etDesplazamiento)
        etDesplazamiento.setText(textoRecuperado)
        etDesplazamiento.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val texto = dest.subSequence(0, dstart).toString() +
                source.subSequence(start, end) +
                dest.subSequence(dend, dest.length)
            val numero = texto.toIntOrNull()
            if (texto.isEmpty() || (numero != null && numero in 1..25)) null else ""
        })

        val tvMensaje = findViewById<TextView>(R.id.tvMensajeRecuperado)
        tvMensaje.text = textoDescifradoCesar

        findViewById<Button>(R.id.btnDescifrarCesar).setOnClickListener {
            val uri = archivoTxtSeleccionado
            val desplazamiento = etDesplazamiento.text.toString().trim().toIntOrNull()

            when {
                uri == null -> mostrarAviso("Error", "Selecciona primero un archivo .txt")
                desplazamiento == null || desplazamiento !in 1..25 ->
                    mostrarAviso("Error", "Introduce un desplazamiento entre 1 y 25")
                else -> try {
                    textoDescifradoCesar = DescifrarCesar().descifrarArchivo(this, uri, desplazamiento)
                    tvMensaje.text = textoDescifradoCesar
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al leer el archivo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ---------------------------------------------------------------
    //  Descifrado AES
    // ---------------------------------------------------------------

    private fun iniciarDescifradoAes(uri: Uri, contrasena: String) {
        val g = generacion
        descifrandoAes = true
        findViewById<Button>(R.id.btnDescifrarAes).isEnabled = false
        findViewById<ProgressBar>(R.id.pbProgresoAes).apply {
            isIndeterminate = true
            visibility = View.VISIBLE
        }
        findViewById<TextView>(R.id.tvEstadoAes).apply {
            text = "Descifrando…"
            visibility = View.VISIBLE
        }

        Thread {
            try {
                val resultado = DescifrarAes()
                    .descifrarArchivo(applicationContext, uri, contrasena) { mostrarProgreso(g, it) }

                runOnUiThread {
                    if (!pantallaAes(g)) return@runOnUiThread
                    finalizarDescifradoAes(
                        "Descifrado · ${DescifrarAes.formatearBytes(resultado.tamanoBytes)}"
                    )
                    rutaImagenRecuperada = resultado.archivo.absolutePath
                    mostrarVistaPrevia(resultado.archivo, g)
                    Toast.makeText(
                        this,
                        "Vista previa lista · se borra al salir de la pantalla",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: DescifrarAes.ErrorDescifradoException) {
                runOnUiThread {
                    if (!pantallaAes(g)) return@runOnUiThread
                    finalizarDescifradoAes(e.titulo)
                    mostrarAviso(e.titulo, e.message.orEmpty())
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!pantallaAes(g)) return@runOnUiThread
                    finalizarDescifradoAes("Error al descifrar")
                    mostrarAviso("Error al descifrar", e.message ?: "No se pudo leer el archivo.")
                }
            }
        }.start()
    }

    private fun pantallaAes(g: Int = generacion) =
        g == generacion && pantallaActual == "aes" && !isFinishing && !isDestroyed

    private fun mostrarProgreso(g: Int, porcentaje: Int) {
        runOnUiThread {
            if (!pantallaAes(g)) return@runOnUiThread
            findViewById<ProgressBar>(R.id.pbProgresoAes).apply {
                isIndeterminate = false
                progress = porcentaje
            }
            findViewById<TextView>(R.id.tvEstadoAes).text = "Descifrando… $porcentaje %"
        }
    }

    private fun finalizarDescifradoAes(estado: String) {
        descifrandoAes = false
        findViewById<Button>(R.id.btnDescifrarAes).isEnabled = true
        findViewById<ProgressBar>(R.id.pbProgresoAes).visibility = View.GONE
        findViewById<TextView>(R.id.tvEstadoAes).apply {
            text = estado
            visibility = View.VISIBLE
        }
    }

    private fun mostrarVistaPrevia(archivo: File, g: Int = generacion) {
        Thread {
            val foto = cargarImagen(archivo)
            runOnUiThread {
                if (!pantallaAes(g)) return@runOnUiThread
                findViewById<ImageView>(R.id.ivImagenRecuperada).apply {
                    if (foto != null) setImageBitmap(foto)
                    else setImageResource(android.R.drawable.ic_menu_gallery)
                }
                findViewById<TextView>(R.id.tvTituloImagen).text =
                    if (foto != null) {
                        "Imagen recuperada · solo en pantalla"
                    } else {
                        "Archivo recuperado (sin vista previa)"
                    }
            }
        }.start()
    }

    /** Decodifica la imagen reduciéndola a 1600 px para no agotar la memoria. */
    private fun cargarImagen(archivo: File): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var factor = 1
        while (maxOf(limites.outWidth, limites.outHeight) / factor > 1600) factor *= 2

        return try {
            BitmapFactory.decodeFile(
                archivo.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = factor }
            )
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun mostrarAviso(titulo: String, mensaje: String) {
        if (isFinishing || isDestroyed) return
        aviso = AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .create()
            .also { it.show() }
    }

    // ---------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("pantalla_actual", pantallaActual)
        outState.putParcelable("uri_txt", archivoTxtSeleccionado)
        outState.putParcelable("uri_enc", archivoEncSeleccionado)

        when (pantallaActual) {
            "aes" -> {
                outState.putString(
                    "texto_contrasena",
                    findViewById<EditText>(R.id.etContrasena)?.text.toString()
                )
                outState.putString("ruta_imagen", rutaImagenRecuperada)
            }
            "cesar" -> {
                outState.putString(
                    "texto_desplazamiento",
                    findViewById<EditText>(R.id.etDesplazamiento)?.text.toString()
                )
                outState.putString("texto_descifrado_cesar", textoDescifradoCesar)
            }
        }
    }

    private val selectorTxt =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            selectorPendiente = false
            uri ?: return@registerForActivityResult
            archivoTxtSeleccionado = uri
            Toast.makeText(this, "Archivo TXT seleccionado", Toast.LENGTH_SHORT).show()
        }

    private val selectorEnc =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            selectorPendiente = false
            uri ?: return@registerForActivityResult

            val nombre = obtenerNombreArchivo(uri)
            if (!nombre.lowercase().endsWith(".png.enc")) {
                Toast.makeText(this, "Selecciona un archivo con extensión .png.enc", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }

            archivoEncSeleccionado = uri
            borrarImagenRecuperada()
            findViewById<TextView>(R.id.tvEstadoAes).apply {
                text = "Archivo: $nombre"
                visibility = View.VISIBLE
            }
            findViewById<ImageView>(R.id.ivImagenRecuperada)
                .setImageResource(android.R.drawable.ic_menu_gallery)
            findViewById<TextView>(R.id.tvTituloImagen).text = "Imagen recuperada"

            Toast.makeText(this, "Archivo ENC seleccionado", Toast.LENGTH_SHORT).show()
        }

    fun abrirSelectorTxt() {
        selectorPendiente = true
        selectorTxt.launch(arrayOf("text/plain", "text/*"))
    }

    fun abrirSelectorEnc() {
        selectorPendiente = true
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
