#!/bin/bash

# Script para compilar y ejecutar el juego Java 3D Rendering CON GUI (pantalla conectada)

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "Compilando..."
javac -encoding ISO-8859-1 -d bin/ -sourcepath src/ src/org/drgnst/game/*.java src/org/drgnst/game/**/*.java

if [ $? -eq 0 ]; then
    echo "Compilación exitosa."
    
    # Copiar recursos a bin/
    echo "Copiando recursos..."
    cp -r res/* bin/ 2>/dev/null || mkdir -p bin/levels bin/textures && cp -r res/* bin/
    
    echo "Iniciando juego con GUI..."
    
    # Ejecutar SIN Xvfb - para ver la ventana del juego
    java -cp bin org.drgnst.game.Main
else
    echo "Error en la compilación"
    exit 1
fi
