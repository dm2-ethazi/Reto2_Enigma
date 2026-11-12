package com.dm2.enigma

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.dm2.enigma.Descifradores.DescifrarAes
import com.dm2.enigma.Descifradores.DescifrarCesar
import java.io.File

/**
 * Pantalla principal de CriptoApp: menú, descifrado AES de imágenes y descifrado
 * César de textos, con selector de idioma y avisos en el idioma elegido.
 *
 * Una sola actividad compone las tres pantallas con [ponerLayout]; el estado se
 * guarda en [onSaveInstanceState] y todo resultado descifrado vive en memoria o
 * en la caché privada, que se borra al salir de la pantalla.
 */
class MainActivity : AppCompatActivity() {

    /** Pantalla visible: `"menu"`, `"aes"` o `"cesar"`. */
    private var pantallaActual = "menu"
    /** `.txt` elegido para el descifrado César. */
    private var archivoTxtSeleccionado: Uri? = null
    /** `.png.enc` elegido para el descifrado AES. */
    private var archivoEncSeleccionado: Uri? = null
    /** Último texto descifrado con César, para no perderlo al girar la pantalla. */
    private var textoDescifradoCesar = ""
    /** Temporal en caché con la imagen descifrada: solo vive mientras se ve la pantalla. */
    private var rutaImagenRecuperada: String? = null
    /** Evita lanzar dos descifrados AES a la vez. */
    private var descifrandoAes = false
    /** Sube al reiniciar la pantalla: descarta resultados de descifrados antiguos. */
    private var generacion = 0
    /** Hay un selector de archivos abierto: al volver no se reinicia la pantalla. */
    private var selectorPendiente = false
    /** La pantalla debe limpiarse la próxima vez que la app vuelva al frente. */
    private var reiniciarAlEntrar = false
    /** Diálogo de aviso visible, para poder cerrarlo al cambiar de pantalla. */
    private var aviso: AlertDialog? = null

    /** Abre el menú la primera vez y reconstruye la pantalla anterior tras un giro. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La app es siempre oscura: iconos claros en las barras del sistema.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

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

    /**
     * Coloca el layout y aparta su contenido de la barra de estado, la cámara frontal,
     * los botones de navegación y el teclado: el pergamino nunca queda debajo.
     */
    private fun ponerLayout(@LayoutRes layout: Int) {
        setContentView(layout)
        val raiz = findViewById<View>(R.id.main) ?: return
        // Márgenes de mesa definidos en el layout: las barras del sistema se SUMAN a ellos.
        val margenIzq = raiz.paddingLeft
        val margenSup = raiz.paddingTop
        val margenDer = raiz.paddingRight
        val margenInf = raiz.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            vista.updatePadding(
                left = margenIzq + barras.left,
                top = margenSup + barras.top,
                right = margenDer + barras.right,
                bottom = margenInf + maxOf(barras.bottom, teclado)
            )
            insets
        }
        ViewCompat.requestApplyInsets(raiz)
    }

    /** Recupera el [Uri] guardado en [clave], también en versiones anteriores a API 33. */
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

    /** Abre el menú de inicio: elegir AES o César, cambiar idioma y ver «Acerca de». */
    fun mostrarMenu() {
        pantallaActual = "menu"
        limpiarEstado()
        ponerLayout(R.layout.activity_main)
        findViewById<Button>(R.id.btnAesImagenes).setOnClickListener { mostrarAes() }
        findViewById<Button>(R.id.btnCesarTexto).setOnClickListener { mostrarCesar() }
        configurarSelectorIdioma()
        findViewById<Button>(R.id.btnAcercaDe).setOnClickListener {
            mostrarAviso(getString(R.string.acerca_de), getString(R.string.acerca_de_texto))
        }
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

    /** Muestra la pantalla de AES con [textoRecuperado] ya escrito en la contraseña. */
    private fun mostrarAes(textoRecuperado: String = "") {
        pantallaActual = "aes"
        ponerLayout(R.layout.activity_aes)

        findViewById<Button>(R.id.btnVolver).setOnClickListener { mostrarMenu() }
        findViewById<Button>(R.id.btnImportarEnc).setOnClickListener { abrirSelectorEnc() }
        findViewById<Button>(R.id.btnAyuda).setOnClickListener {
            mostrarAviso(getString(R.string.ayuda_aes_titulo), getString(R.string.ayuda_aes_texto))
        }

        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        etContrasena.setText(textoRecuperado)

        val btnMostrar = findViewById<ImageButton>(R.id.btnMostrarContrasena)
        btnMostrar.setOnClickListener {
            // Se cambia el método de transformación, no el inputType: cambiar
            // inputType a "password" impone la tipografía monoespaciada del sistema.
            val oculta = etContrasena.transformationMethod is PasswordTransformationMethod
            if (oculta) {
                etContrasena.transformationMethod = null
                btnMostrar.setImageResource(R.drawable.ic_ojo_cerrado)
                btnMostrar.contentDescription = getString(R.string.ocultar_contrasena)
            } else {
                etContrasena.transformationMethod = PasswordTransformationMethod.getInstance()
                btnMostrar.setImageResource(R.drawable.ic_ojo_abierto)
                btnMostrar.contentDescription = getString(R.string.mostrar_contrasena)
            }
            etContrasena.setSelection(etContrasena.text.length)
        }

        findViewById<Button>(R.id.btnDescifrarAes).setOnClickListener {
            val uri = archivoEncSeleccionado
            val contrasena = etContrasena.text.toString()

            when {
                descifrandoAes -> Unit
                uri == null -> mostrarAviso(
                    getString(R.string.archivo_no_seleccionado),
                    getString(R.string.importa_enc_primero)
                )
                !DescifrarAes.esContrasenaValida(contrasena) -> mostrarAviso(
                    getString(R.string.contrasena_no_valida),
                    getString(R.string.contrasena_formato)
                )
                else -> iniciarDescifradoAes(uri, contrasena)
            }
        }

        archivoEncSeleccionado?.let { uri ->
            findViewById<TextView>(R.id.tvEstadoAes).apply {
                text = getString(R.string.archivo_seleccionado, obtenerNombreArchivo(uri))
                visibility = View.VISIBLE
            }
        }

        rutaImagenRecuperada?.let { ruta ->
            val archivo = File(ruta)
            if (archivo.exists()) mostrarVistaPrevia(archivo) else rutaImagenRecuperada = null
        }
    }

    /** Muestra la pantalla de César con el desplazamiento [textoRecuperado]. */
    private fun mostrarCesar(textoRecuperado: String = "") {
        pantallaActual = "cesar"
        ponerLayout(R.layout.activity_cesar)

        findViewById<Button>(R.id.btnVolver).setOnClickListener { mostrarMenu() }
        findViewById<Button>(R.id.btnImportarTxt).setOnClickListener { abrirSelectorTxt() }
        findViewById<Button>(R.id.btnAyuda).setOnClickListener {
            mostrarAviso(getString(R.string.ayuda_cesar_titulo), getString(R.string.ayuda_cesar_texto))
        }

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
                uri == null -> mostrarAviso(
                    getString(R.string.error),
                    getString(R.string.selecciona_txt)
                )
                desplazamiento == null || desplazamiento !in 1..25 ->
                    mostrarAviso(
                        getString(R.string.error),
                        getString(R.string.desplazamiento_rango)
                    )
                else -> try {
                    textoDescifradoCesar = DescifrarCesar().descifrarArchivo(this, uri, desplazamiento)
                    tvMensaje.text = textoDescifradoCesar
                } catch (e: DescifrarCesar.ErrorCesarException) {
                    mostrarAviso(getString(e.titulo), getString(e.mensaje, *e.argumentos))
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        getString(R.string.error_leer_archivo, e.message.orEmpty()),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------------------------------------------------------
    //  Idioma
    // ---------------------------------------------------------------

    /** Rellena el desplegable del inicio y aplica el idioma que elija el usuario. */
    private fun configurarSelectorIdioma() {
        val spinner = findViewById<Spinner>(R.id.spIdioma) ?: return
        val etiquetas = resources.getStringArray(R.array.idiomas)
        val adaptador = ArrayAdapter(this, R.layout.item_idioma, etiquetas)
        adaptador.setDropDownViewResource(R.layout.item_idioma_desplegable)
        spinner.adapter = adaptador
        spinner.setSelection(indiceIdiomaActual(), false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                cambiarIdioma(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        findViewById<View>(R.id.tvIdiomaFlecha)?.setOnClickListener { spinner.performClick() }
    }

    /** Idioma activo: el elegido en la app o, si no hay ninguno, el del sistema. */
    private fun idiomaActual(): String =
        AppCompatDelegate.getApplicationLocales().get(0)?.language
            ?: resources.configuration.locales[0].language

    /** Posición del idioma activo dentro de [CODIGOS_IDIOMA]. */
    private fun indiceIdiomaActual(): Int = when {
        idiomaActual().startsWith("en") -> 1
        idiomaActual().startsWith("eu") -> 2
        else -> 0
    }

    private fun cambiarIdioma(indice: Int) {
        val codigo = CODIGOS_IDIOMA.getOrNull(indice) ?: return
        // Tras recrearse la actividad por el cambio, la selección inicial no se reaplica.
        if (idiomaActual() == codigo) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codigo))
    }

    // ---------------------------------------------------------------
    //  Descifrado AES
    // ---------------------------------------------------------------

    /** Descifra el `.enc` en segundo plano, con barra de progreso y estado en pantalla. */
    private fun iniciarDescifradoAes(uri: Uri, contrasena: String) {
        val g = generacion
        descifrandoAes = true
        findViewById<Button>(R.id.btnDescifrarAes).isEnabled = false
        findViewById<ProgressBar>(R.id.pbProgresoAes).apply {
            isIndeterminate = true
            visibility = View.VISIBLE
        }
        findViewById<TextView>(R.id.tvEstadoAes).apply {
            text = getString(R.string.descifrando)
            visibility = View.VISIBLE
        }

        Thread {
            try {
                val resultado = DescifrarAes()
                    .descifrarArchivo(applicationContext, uri, contrasena) { mostrarProgreso(g, it) }

                runOnUiThread {
                    if (!pantallaAes(g)) return@runOnUiThread
                    finalizarDescifradoAes(
                        getString(
                            R.string.descifrado_tamano,
                            DescifrarAes.formatearBytes(resultado.tamanoBytes)
                        )
                    )
                    rutaImagenRecuperada = resultado.archivo.absolutePath
                    mostrarVistaPrevia(resultado.archivo, g)
                    Toast.makeText(
                        this,
                        getString(R.string.vista_previa_lista),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: DescifrarAes.ErrorDescifradoException) {
                runOnUiThread {
                    if (!pantallaAes(g)) return@runOnUiThread
                    finalizarDescifradoAes(getString(e.titulo))
                    mostrarAviso(getString(e.titulo), getString(e.mensaje, *e.argumentos))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!pantallaAes(g)) return@runOnUiThread
                    finalizarDescifradoAes(getString(R.string.error_al_descifrar))
                    mostrarAviso(
                        getString(R.string.error_al_descifrar),
                        e.message ?: getString(R.string.no_escribir_archivo)
                    )
                }
            }
        }.start()
    }

    /** True si la pantalla de AES sigue viva y el descifrado [g] no es de una sesión anterior. */
    private fun pantallaAes(g: Int = generacion) =
        g == generacion && pantallaActual == "aes" && !isFinishing && !isDestroyed

    /** Pinta el porcentaje del descifrado en la barra de progreso de AES. */
    private fun mostrarProgreso(g: Int, porcentaje: Int) {
        runOnUiThread {
            if (!pantallaAes(g)) return@runOnUiThread
            findViewById<ProgressBar>(R.id.pbProgresoAes).apply {
                isIndeterminate = false
                progress = porcentaje
            }
            findViewById<TextView>(R.id.tvEstadoAes).text =
                getString(R.string.descifrando_progreso, porcentaje)
        }
    }

    /** Devuelve la pantalla de AES a su estado normal mostrando el mensaje [estado]. */
    private fun finalizarDescifradoAes(estado: String) {
        descifrandoAes = false
        findViewById<Button>(R.id.btnDescifrarAes).isEnabled = true
        findViewById<ProgressBar>(R.id.pbProgresoAes).visibility = View.GONE
        findViewById<TextView>(R.id.tvEstadoAes).apply {
            text = estado
            visibility = View.VISIBLE
        }
    }

    /** Carga la imagen descifrada en segundo plano y la muestra en el pergamino. */
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
                        getString(R.string.imagen_recuperada_aviso)
                    } else {
                        getString(R.string.imagen_sin_vista)
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

    /** Diálogo de aviso compartido por errores, ayuda y «Acerca de». */
    private fun mostrarAviso(titulo: String, mensaje: String) {
        if (isFinishing || isDestroyed) return
        aviso = AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton(R.string.aceptar, null)
            .create()
            .also { it.show() }
    }

    // ---------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------

    /** Guarda pantalla, archivos elegidos y textos para sobrevivir a un giro. */
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

    /** Selector del `.txt` que se descifra con César. */
    private val selectorTxt =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            selectorPendiente = false
            uri ?: return@registerForActivityResult
            archivoTxtSeleccionado = uri
            Toast.makeText(this, getString(R.string.txt_seleccionado), Toast.LENGTH_SHORT).show()
        }

    /** Selector del contenedor `.png.enc` de AES; valida la extensión al volver. */
    private val selectorEnc =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            selectorPendiente = false
            uri ?: return@registerForActivityResult

            val nombre = obtenerNombreArchivo(uri)
            if (!nombre.lowercase().endsWith(".png.enc")) {
                Toast.makeText(this, getString(R.string.extension_enc), Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }

            archivoEncSeleccionado = uri
            borrarImagenRecuperada()
            findViewById<TextView>(R.id.tvEstadoAes).apply {
                text = getString(R.string.archivo_seleccionado, nombre)
                visibility = View.VISIBLE
            }
            findViewById<ImageView>(R.id.ivImagenRecuperada)
                .setImageResource(android.R.drawable.ic_menu_gallery)
            findViewById<TextView>(R.id.tvTituloImagen).text = getString(R.string.imagen_recuperada)

            Toast.makeText(this, getString(R.string.enc_seleccionado), Toast.LENGTH_SHORT).show()
        }

    /** Abre el selector de archivos de texto. */
    fun abrirSelectorTxt() {
        selectorPendiente = true
        selectorTxt.launch(arrayOf("text/plain", "text/*"))
    }

    /** Abre el selector del archivo cifrado de AES. */
    fun abrirSelectorEnc() {
        selectorPendiente = true
        selectorEnc.launch(arrayOf("*/*"))
    }

    /** Nombre visible del archivo; si el proveedor no lo da, devuelve «Archivo». */
    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = getString(R.string.archivo)
        contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val indice = cursor.getColumnIndex("_display_name")
                if (indice >= 0) nombre = cursor.getString(indice)
            }
        }
        return nombre
    }

    companion object {
        /** Mismo orden que `R.array.idiomas`. */
        private val CODIGOS_IDIOMA = arrayOf("es", "en", "eu")
    }
}
