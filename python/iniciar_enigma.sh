#!/usr/bin/env bash
set -e

# Permite iniciar Enigma desde cualquier directorio, incluso con espacios.
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

if [[ -x .venv/bin/python ]]; then
    exec .venv/bin/python main.py "$@"
fi

for enigma_python in python3.14 python3 python; do
    if command -v "$enigma_python" >/dev/null 2>&1; then
        exec "$enigma_python" main.py "$@"
    fi
done

printf '%s\n' 'No se encontró Python. Instala Python 3.14 y las dependencias de requirements.txt.' >&2
exit 1
