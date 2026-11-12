using System.Text;

namespace Enigma.Game
{
    /// <summary>Cifrado César A–Z y normalización compartida de mensajes y respuestas.</summary>
    /// <remarks>@author koldo</remarks>
    public static class CaesarCipher
    {
        /// <summary>Normaliza mayúsculas, acentos y espacios para comparar respuestas.</summary>
        /// <param name="text">Texto introducido o frase del nivel.</param>
        /// <returns>Texto normalizado; vacío si no contiene una respuesta.</returns>
        public static string Normalize(string text)
        {
            var result = new StringBuilder();
            bool pendingSpace = false;

            foreach (char character in (text ?? string.Empty).Trim().Normalize(NormalizationForm.FormC))
            {
                if (char.IsWhiteSpace(character))
                {
                    pendingSpace = result.Length > 0;
                    continue;
                }

                if (pendingSpace)
                    result.Append(' ');

                pendingSpace = false;
                char upper = char.ToUpperInvariant(character);
                if (upper == '\u00d1')
                {
                    result.Append(upper);
                    continue;
                }

                // Solo las vocales pierden el acento; la Ñ es una letra distinta de N.
                char basic = upper.ToString().Normalize(NormalizationForm.FormD)[0];
                result.Append("AEIOU".IndexOf(basic) >= 0 ? basic : upper);
            }

            return result.ToString();
        }

        /// <summary>Desplaza las letras y conserva espacios y puntuación.</summary>
        /// <param name="text">Mensaje original.</param>
        /// <param name="shift">Desplazamiento positivo para cifrar y negativo para descifrar.</param>
        /// <returns>Mensaje desplazado con retorno circular entre A y Z.</returns>
        public static string Encode(string text, int shift)
        {
            int offset = (shift % 26 + 26) % 26;
            char[] letters = (text ?? string.Empty).ToCharArray();

            for (int index = 0; index < letters.Length; index++)
            {
                if (letters[index] >= 'A' && letters[index] <= 'Z')
                    letters[index] = (char)('A' + (letters[index] - 'A' + offset) % 26);
            }

            return new string(letters);
        }
    }
}
