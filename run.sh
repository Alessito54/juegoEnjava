#!/bin/bash

# Script para compilar y ejecutar el juego Java 3D Rendering
# Uso automático de GUI si hay pantalla conectada, o Xvfb si no

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "Compilando..."
javac -encoding ISO-8859-1 -d bin/ -sourcepath src/ src/org/drgnst/game/*.java src/org/drgnst/game/**/*.java

if [ $? -eq 0 ]; then
    echo "Compilación exitosa."
    
    # Copiar recursos a bin/
    echo "Copiando recursos..."
    cp -r res/* bin/ 2>/dev/null || mkdir -p bin/levels bin/textures && cp -r res/* bin/
    
    # Detectar si hay pantalla disponible
    if [ -n "$DISPLAY" ] && [ "$DISPLAY" != "" ]; then
        echo "Pantalla detectada ($DISPLAY). Ejecutando con GUI..."
        java -cp bin org.drgnst.game.Main
    else
        echo "No hay pantalla conectada. Usando servidor gráfico virtual (Xvfb)..."
        echo "El juego está corriendo pero sin GUI visible."
        xvfb-run -a java \
            -Djava.awt.headless=false \
            -Dsun.java2d.opengl=false \
            -cp bin org.drgnst.game.Main
    fi
else
    echo "Error en la compilación"
    exit 1
fi

