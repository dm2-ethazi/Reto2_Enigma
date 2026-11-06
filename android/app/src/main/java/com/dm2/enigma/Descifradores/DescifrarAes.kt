package com.dm2.enigma.Descifradores

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
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
 */
class DescifrarAes {

    /** Error de descifrado; [titulo] es el encabezado que muestra la interfaz. */
    class ErrorDescifradoException(val titulo: String, mensaje: String) : Exception(mensaje)

    /** Descifra el `.enc` de [origen] en Descargas/Enigma. [alProgresar]: 0..100. */
    fun descifrarArchivo(
        context: Context,
        origen: Uri,
        contrasena: String,
        alProgresar: ((Int) -> Unit)? = null
    ): ResultadoDescifrado {
        comprobarContrasena(contrasena)

        val tamano = consultar(context, origen, OpenableColumns.SIZE)?.toLongOrNull() ?: -1L
        if (tamano in 1 until SOBRECARGA) {
            throw ErrorDescifradoException(
                "Archivo no válido",
                "El contenedor está incompleto: le falta la cabecera o la etiqueta."
            )
        }

        val carpeta = carpetaDestino(context)
        val textoEsperado = if (tamano > 0) tamano - SOBRECARGA else -1L
        comprobarEspacio(carpeta, textoEsperado)
        val destino = archivoLibre(carpeta, nombreDe(context, origen))

        val entrada = try {
            context.contentResolver.openInputStream(origen)
        } catch (e: Exception) {
            null
        } ?: throw ErrorDescifradoException("Error al leer", "No se pudo abrir el archivo cifrado.")

        val escritos = entrada.use {
            descifrar(it, destino, contrasena, textoEsperado, alProgresar)
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
            throw ErrorDescifradoException("Archivo no válido", "El archivo es demasiado corto para ser R2AE.")
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
                            "Archivo no válido",
                            "El contenedor está truncado: falta la etiqueta GCM."
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
                "Contraseña incorrecta",
                "La contraseña no es válida o el archivo ha sido alterado. No se ha guardado ningún resultado."
            )
        } catch (e: IOException) {
            if (esFaltaDeEspacio(e)) {
                throw ErrorDescifradoException("No hay almacenamiento", "El dispositivo se quedó sin espacio libre.")
            }
            throw ErrorDescifradoException("Error al descifrar", e.message ?: "No se pudo escribir el archivo.")
        } finally {
            if (!publicado) temporal.delete()
        }
    }

    private fun validarCabecera(cabecera: ByteArray) {
        fun rechazar(motivo: String): Nothing =
            throw ErrorDescifradoException("Archivo no válido", motivo)

        if (String(cabecera, 0, 4, Charsets.US_ASCII) != "R2AE") {
            rechazar("El archivo no es un contenedor R2AE.")
        }
        val version = cabecera[4].toInt() and 0xFF
        if (version != VERSION) rechazar("Versión R2AE no compatible: $version.")
        if (ByteBuffer.wrap(cabecera).getInt(5) != ITERACIONES) {
            rechazar("El contenedor no usa $ITERACIONES iteraciones PBKDF2.")
        }
    }

    private fun comprobarContrasena(contrasena: String) {
        if (!esContrasenaValida(contrasena)) {
            throw ErrorDescifradoException(
                "Contraseña no válida",
                "La contraseña debe tener $TAMANO_CONTRASENA caracteres A-Z, a-z o 0-9."
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

        private const val SUFIJO_TEMPORAL = ".part"
        private const val CARPETA_DESTINO = "Enigma"
        private val PATRON_CONTRASENA = Regex("[A-Za-z0-9]{16}")

        /** Mismo formato que `validate_password` en Python. */
        fun esContrasenaValida(contrasena: String) = PATRON_CONTRASENA.matches(contrasena)

        fun formatearBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val unidades = arrayOf("KB", "MB", "GB")
            var valor = bytes.toDouble()
            var indice = -1
            while (valor >= 1024 && indice < unidades.lastIndex) {
                valor /= 1024
                indice++
            }
            return String.format(Locale.US, "%.1f %s", valor, unidades[indice])
        }

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

        private fun leerExacto(entrada: InputStream, destino: ByteArray): Boolean {
            var leidos = 0
            while (leidos < destino.size) {
                val n = entrada.read(destino, leidos, destino.size - leidos)
                if (n < 0) return false
                leidos += n
            }
            return true
        }

        private fun publicar(temporal: File, destino: File) {
            destino.delete()
            if (!temporal.renameTo(destino)) {
                temporal.copyTo(destino, overwrite = true)
                temporal.delete()
            }
        }

        private fun esFaltaDeEspacio(error: IOException): Boolean {
            val texto = error.message?.lowercase(Locale.ROOT) ?: return false
            return "enospc" in texto || "no space" in texto
        }

        private fun comprobarEspacio(carpeta: File, necesarios: Long) {
            if (necesarios <= 0) return
            val libres = try {
                carpeta.usableSpace
            } catch (e: SecurityException) {
                return
            }
            if (libres in 1 until necesarios) {
                throw ErrorDescifradoException(
                    "No hay almacenamiento",
                    "Se necesitan ${formatearBytes(necesarios)} y solo quedan ${formatearBytes(libres)} libres."
                )
            }
        }

        private fun carpetaDestino(context: Context): File {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val carpeta = File(base, CARPETA_DESTINO)
            if (!carpeta.isDirectory && !carpeta.mkdirs()) {
                throw ErrorDescifradoException(
                    "No hay almacenamiento",
                    "No se pudo crear la carpeta de descargas en el dispositivo."
                )
            }
            return carpeta
        }

        private fun nombreDe(context: Context, origen: Uri): String {
            val original = consultar(context, origen, OpenableColumns.DISPLAY_NAME)
                ?: origen.lastPathSegment.orEmpty()
            return original.substringAfterLast('/')
                .removeSuffix(".enc")
                .removeSuffix(".ENC")
                .replace(Regex("[^A-Za-z0-9._ -]"), "_")
                .trim()
                .ifEmpty { "archivo_descifrado" }
        }

        private fun consultar(context: Context, uri: Uri, columna: String): String? = try {
            context.contentResolver.query(uri, arrayOf(columna), null, null, null)?.use { cursor ->
                val indice = if (cursor.moveToFirst()) cursor.getColumnIndex(columna) else -1
                if (indice >= 0 && !cursor.isNull(indice)) cursor.getString(indice) else null
            }
        } catch (e: Exception) {
            null
        }

        private fun archivoLibre(carpeta: File, nombre: String): File {
            var archivo = File(carpeta, nombre)
            var indice = 1
            while (archivo.exists()) archivo = File(carpeta, "$nombre (${indice++})")
            return archivo
        }
    }
}

/** Resultado de un descifrado correcto: ya está autenticado y guardado. */
data class ResultadoDescifrado(val archivo: File, val tamanoBytes: Long)
