package com.dm2.enigma.Descifradores

import android.content.Context
import android.net.Uri

class DescifrarCesar {

    companion object {
        // Rango 1: ASCII imprimible estándar
        private const val R1_INI = 32
        private const val R1_FIN = 126

        // Rango 2: ASCII extendido (Latin-1 imprimible)
        private const val R2_INI = 128
        private const val R2_FIN = 254
    }

    fun descifrarTexto(texto: String, num: Int): String {
        val sb = StringBuilder(texto.length)
        for (c in texto) {
            val codigo = c.code

            if ((codigo in 32..126)) {
                var nuevo = codigo - num

                if (nuevo < 32) {
                    nuevo = 126
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
        } ?: throw IllegalStateException("No se pudo abrir el archivo")
    }

    /**
     * Atajo: lee el .txt de la Uri y lo devuelve descifrado.
     */
    fun descifrarArchivo(context: Context, uri: Uri, num: Int): String {
        return descifrarTexto(leerArchivoTxt(context, uri), num)
    }
}