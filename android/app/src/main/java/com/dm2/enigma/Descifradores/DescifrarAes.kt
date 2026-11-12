package com.dm2.enigma.Descifradores

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import com.dm2.enigma.R
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Locale
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Descifrado AES-256-GCM por bloques del contenedor R2AE v1 (`encrypt/aes.py`).
 *
 * Cabecera de 37 bytes: "R2AE" · versión 1 · 200000 iteraciones (4 bytes big
 * endian) · sal de 16 · nonce de 12. Se valida antes de derivar la clave y se
 * usa como AAD. Después van el texto cifrado y una etiqueta GCM de 16 bytes al
 * final. La clave sale de PBKDF2-HMAC-SHA256 (32 bytes).
 *
 * El texto se escribe en un `.part` que solo se publica cuando la etiqueta se
 * autentica, así que una contraseña incorrecta no deja ningún archivo.
 *
 * El resultado va a un archivo temporal en la caché privada de la app (nunca a
 * Descargas ni a la galería): solo sirve para mostrar la vista previa y se
 * borra al salir de la pantalla.
 */
class DescifrarAes {

    /**
     * Error de descifrado. Los textos son recursos para que la interfaz los
     * muestre en el idioma elegido; [argumentos] rellena el mensaje.
     */
    class ErrorDescifradoException(
        @StringRes val titulo: Int,
        @StringRes val mensaje: Int,
        val argumentos: Array<out Any> = emptyArray<Any>()
    ) : Exception()

    /** Descifra el `.enc` de [origen] en un temporal de la caché. [alProgresar]: 0..100. */
    fun descifrarArchivo(
        context: Context,
        origen: Uri,
        contrasena: String,
        alProgresar: ((Int) -> Unit)? = null
    ): ResultadoDescifrado {
        comprobarContrasena(contrasena)

        val tamano = consultar(context, origen, OpenableColumns.SIZE)?.toLongOrNull() ?: -1L
        if (tamano > TAMANO_MAXIMO) {
            throw ErrorDescifradoException(
                R.string.archivo_grande,
                R.string.archivo_grande_detalle,
                arrayOf(formatearBytes(TAMANO_MAXIMO), formatearBytes(tamano))
            )
        }
        if (tamano in 1 until SOBRECARGA) {
            throw ErrorDescifradoException(
                R.string.archivo_no_valido,
                R.string.contenedor_incompleto
            )
        }

        val carpeta = carpetaTemporal(context)
        val textoEsperado = if (tamano > 0) tamano - SOBRECARGA else -1L
        comprobarEspacio(carpeta, textoEsperado)
        limpiarTemporales(context)
        val destino = File.createTempFile(PREFIJO_TEMPORAL, SUFIJO_CACHE, carpeta)

        val entrada = try {
            context.contentResolver.openInputStream(origen)
        } catch (e: Exception) {
            null
        } ?: throw ErrorDescifradoException(R.string.error_al_leer, R.string.no_abrir_cifrado)

        val escritos = try {
            entrada.use {
                descifrar(it, destino, contrasena, textoEsperado, alProgresar)
            }
        } catch (e: Exception) {
            destino.delete()
            throw e
        }
        return ResultadoDescifrado(destino, escritos)
    }

    /** Descifra [contenedor] en [destino] y devuelve los bytes escritos. */
    fun descifrar(
        contenedor: InputStream,
        destino: File,
        contrasena: String,
        tamanoTexto: Long = -1L,
        alProgresar: ((Int) -> Unit)? = null
    ): Long {
        comprobarContrasena(contrasena)

        val cabecera = ByteArray(TAMANO_CABECERA)
        if (!leerExacto(contenedor, cabecera)) {
            throw ErrorDescifradoException(R.string.archivo_no_valido, R.string.archivo_corto)
        }
        validarCabecera(cabecera)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            derivarClave(contrasena, cabecera.copyOfRange(9, 25)),
            GCMParameterSpec(TAMANO_ETIQUETA * 8, cabecera.copyOfRange(25, TAMANO_CABECERA))
        )
        cipher.updateAAD(cabecera)

        val temporal = File(destino.absolutePath + SUFIJO_TEMPORAL)
        var escritos = 0L
        var publicado = false

        try {
            temporal.outputStream().buffered().use { salida ->
                // Se mantienen siempre los últimos 16 bytes sin descifrar porque
                // son la etiqueta GCM y deben ir a doFinal().
                val buffer = ByteArray(TAMANO_BLOQUE + TAMANO_ETIQUETA)
                var enBuffer = 0
                var fin = false

                while (true) {
                    while (!fin && enBuffer < buffer.size) {
                        val leidos = contenedor.read(buffer, enBuffer, buffer.size - enBuffer)
                        if (leidos < 0) fin = true else enBuffer += leidos
                    }

                    val cifrado = if (fin) enBuffer - TAMANO_ETIQUETA else buffer.size - TAMANO_ETIQUETA
                    if (cifrado < 0) {
                        throw ErrorDescifradoException(
                            R.string.archivo_no_valido,
                            R.string.contenedor_truncado
                        )
                    }

                    if (cifrado > 0) {
                        salida.write(cipher.update(buffer, 0, cifrado))
                        escritos += cifrado
                    }

                    if (fin) {
                        salida.write(cipher.doFinal(buffer, cifrado, TAMANO_ETIQUETA))
                        break
                    }

                    System.arraycopy(buffer, cifrado, buffer, 0, TAMANO_ETIQUETA)
                    enBuffer = TAMANO_ETIQUETA
                    if (tamanoTexto > 0) {
                        alProgresar?.invoke(((escritos * 100) / tamanoTexto).toInt().coerceAtMost(100))
                    }
                }
            }
            publicar(temporal, destino)
            publicado = true
            return escritos
        } catch (e: AEADBadTagException) {
            throw ErrorDescifradoException(
                R.string.contrasena_incorrecta,
                R.string.contrasena_incorrecta_detalle
            )
        } catch (e: IOException) {
            if (esFaltaDeEspacio(e)) {
                throw ErrorDescifradoException(R.string.sin_almacenamiento, R.string.sin_espacio)
            }
            throw ErrorDescifradoException(R.string.error_al_descifrar, R.string.no_escribir_archivo)
        } finally {
            if (!publicado) temporal.delete()
        }
    }

    /** Comprueba el magic «R2AE», la versión y las iteraciones antes de derivar la clave. */
    private fun validarCabecera(cabecera: ByteArray) {
        fun rechazar(@StringRes motivo: Int, vararg argumentos: Any): Nothing =
            throw ErrorDescifradoException(
                R.string.archivo_no_valido,
                motivo,
                arrayOf(*argumentos)
            )

        if (String(cabecera, 0, 4, Charsets.US_ASCII) != "R2AE") {
            rechazar(R.string.no_r2ae)
        }
        val version = cabecera[4].toInt() and 0xFF
        if (version != VERSION) rechazar(R.string.version_no_compatible, version)
        if (ByteBuffer.wrap(cabecera).getInt(5) != ITERACIONES) {
            rechazar(R.string.iteraciones_incorrectas, ITERACIONES)
        }
    }

    /** Rechaza contraseñas que no sean 16 caracteres alfanuméricos. */
    private fun comprobarContrasena(contrasena: String) {
        if (!esContrasenaValida(contrasena)) {
            throw ErrorDescifradoException(
                R.string.contrasena_no_valida,
                R.string.contrasena_formato
            )
        }
    }

    companion object {
        private const val TAMANO_CONTRASENA = 16
        private const val ITERACIONES = 200_000
        private const val TAMANO_CABECERA = 37
        private const val TAMANO_ETIQUETA = 16
        private const val VERSION = 1
        private const val TAMANO_BLOQUE = 1024 * 1024

        /** Cabecera + etiqueta: lo que no es texto cifrado. */
        private const val SOBRECARGA = TAMANO_CABECERA + TAMANO_ETIQUETA

        /** Tamaño máximo del contenedor que la app acepta descifrar. */
        const val TAMANO_MAXIMO = 100L * 1024 * 1024

        private const val SUFIJO_TEMPORAL = ".part"
        private const val SUFIJO_CACHE = ".tmp"
        private const val CARPETA_TEMPORAL = "descifrado"
        private const val PREFIJO_TEMPORAL = "enigma-"
        private val PATRON_CONTRASENA = Regex("[A-Za-z0-9]{16}")

        /** Mismo formato que `validate_password` en Python. */
        fun esContrasenaValida(contrasena: String) = PATRON_CONTRASENA.matches(contrasena)

        /** Tamaño legible en B, KiB, MiB o GiB. */
        fun formatearBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val unidades = arrayOf("KiB", "MiB", "GiB")
            var valor = bytes.toDouble()
            var indice = -1
            while (valor >= 1024 && indice < unidades.lastIndex) {
                valor /= 1024
                indice++
            }
            return String.format(Locale.US, "%.1f %s", valor, unidades[indice])
        }

        /** Clave AES-256 con PBKDF2-HMAC-SHA256 (200 000 iteraciones) y la sal de la cabecera. */
        private fun derivarClave(contrasena: String, sal: ByteArray): SecretKeySpec {
            val especificacion = PBEKeySpec(contrasena.toCharArray(), sal, ITERACIONES, 256)
            try {
                val clave = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacion).encoded
                return SecretKeySpec(clave, "AES")
            } finally {
                especificacion.clearPassword()
            }
        }

        /** Llena [destino] desde [entrada]; false si el flujo se acaba antes. */
        private fun leerExacto(entrada: InputStream, destino: ByteArray): Boolean {
            var leidos = 0
            while (leidos < destino.size) {
                val n = entrada.read(destino, leidos, destino.size - leidos)
                if (n < 0) return false
                leidos += n
            }
            return true
        }

        /** Renombra el `.part` al destino final; si el renombrado falla, copia y borra. */
        private fun publicar(temporal: File, destino: File) {
            destino.delete()
            if (!temporal.renameTo(destino)) {
                temporal.copyTo(destino, overwrite = true)
                temporal.delete()
            }
        }

        /** Detecta el disco lleno (ENOSPC) en el mensaje de la excepción. */
        private fun esFaltaDeEspacio(error: IOException): Boolean {
            val texto = error.message?.lowercase(Locale.ROOT) ?: return false
            return "enospc" in texto || "no space" in texto
        }

        /** Comprueba que queda espacio libre suficiente antes de escribir el temporal. */
        private fun comprobarEspacio(carpeta: File, necesarios: Long) {
            if (necesarios <= 0) return
            val libres = try {
                carpeta.usableSpace
            } catch (e: SecurityException) {
                return
            }
            if (libres in 1 until necesarios) {
                throw ErrorDescifradoException(
                    R.string.sin_almacenamiento,
                    R.string.espacio_insuficiente,
                    arrayOf(formatearBytes(necesarios), formatearBytes(libres))
                )
            }
        }

        /** Caché privada de la app: el resultado nunca aparece en Descargas ni en la galería. */
        private fun carpetaTemporal(context: Context): File {
            val carpeta = File(context.cacheDir, CARPETA_TEMPORAL)
            if (!carpeta.isDirectory && !carpeta.mkdirs()) {
                throw ErrorDescifradoException(
                    R.string.sin_almacenamiento,
                    R.string.carpeta_temporal
                )
            }
            return carpeta
        }

        /** Borra los temporales que hayan quedado de ejecuciones anteriores. */
        fun limpiarTemporales(context: Context) {
            try {
                File(context.cacheDir, CARPETA_TEMPORAL).listFiles()?.forEach { it.delete() }
            } catch (e: SecurityException) {
                // Sin permisos sobre la caché no hay nada que limpiar.
            }
        }

        /** Lee una columna del proveedor de documentos (tamaño, nombre…); null si no puede. */
        private fun consultar(context: Context, uri: Uri, columna: String): String? = try {
            context.contentResolver.query(uri, arrayOf(columna), null, null, null)?.use { cursor ->
                val indice = if (cursor.moveToFirst()) cursor.getColumnIndex(columna) else -1
                if (indice >= 0 && !cursor.isNull(indice)) cursor.getString(indice) else null
            }
        } catch (e: Exception) {
            null
        }

    }
}

/** Resultado de un descifrado correcto: autenticado y en un temporal que hay que borrar. */
data class ResultadoDescifrado(val archivo: File, val tamanoBytes: Long)
