package com.dm2.enigma.Descifradores

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import com.dm2.enigma.R

class DescifrarCesar {

    /**
     * Error de lectura del .txt. Los textos son recursos para que la interfaz
     * los muestre en el idioma elegido; [argumentos] rellena el mensaje.
     */
    class ErrorCesarException(
        @StringRes val titulo: Int,
        @StringRes val mensaje: Int,
        val argumentos: Array<out Any> = emptyArray<Any>()
    ) : Exception()

    companion object {
        // Rango 1: ASCII imprimible estándar
        private const val R1_INI = 32
        private const val R1_FIN = 126

        // Rango 2: ASCII extendido (Latin-1 imprimible)
        private const val R2_INI = 128
        private const val R2_FIN = 254

        /** Tamaño máximo del .txt que la app acepta descifrar. */
        const val TAMANO_MAXIMO = 200L * 1024

        private fun consultarTamano(context: Context, uri: Uri): Long = try {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    val indice = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && indice >= 0 && !cursor.isNull(indice)) {
                        cursor.getLong(indice)
                    } else {
                        -1L
                    }
                } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    fun descifrarTexto(texto: String, num: Int): String {
        val sb = StringBuilder(texto.length)
        for (c in texto) {
            val codigo = c.code

            if ((codigo in R1_INI..R1_FIN)) {
                var nuevo = codigo - num

                if (nuevo < R1_INI) {
                    val resta = R1_INI - nuevo
                    nuevo = (R1_FIN + 1) - resta
                }

                sb.append(nuevo.toChar())
            } else {
                // Saltos de línea, tabuladores, etc., se quedan igual
                sb.append(c)
            }
        }
        return sb.toString()
    }


    /**
     * Lee un archivo .txt desde su Uri y devuelve su contenido como String.
     */
    fun leerArchivoTxt(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException(context.getString(R.string.no_abrir_cifrado))
    }

    /**
     * Atajo: lee el .txt de la Uri y lo devuelve descifrado. Si el archivo supera
     * [TAMANO_MAXIMO] no se llega a leer: abrirlo entero tumbaba la app.
     */
    fun descifrarArchivo(context: Context, uri: Uri, num: Int): String {
        val tamano = consultarTamano(context, uri)
        if (tamano > TAMANO_MAXIMO) {
            throw ErrorCesarException(
                R.string.archivo_grande,
                R.string.archivo_grande_detalle,
                arrayOf(
                    DescifrarAes.formatearBytes(TAMANO_MAXIMO),
                    DescifrarAes.formatearBytes(tamano)
                )
            )
        }
        return descifrarTexto(leerArchivoTxt(context, uri), num)
    }
}
