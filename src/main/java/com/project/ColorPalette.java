package com.project;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Clase encastada encargada de gestionar y dibujar de forma personalizada
 * la hilera de colores predefinidos del enunciado en su propio Canvas.
 */
public class ColorPalette {
    private final Canvas canvas; // El Canvas secundario que se vincula desde Scene Builder
    
    // El array con los 8 colores principales indicados en la imagen del examen
    private final Color[] colors = {
        Color.BLACK, Color.GRAY, Color.RED, Color.ORANGE, 
        Color.YELLOW, Color.GREEN, Color.BLUE, Color.PURPLE
    };
    
    // Se ajustan las dimensiones de las casillas para que quepan bien en tu Canvas de Scene Builder
    private final double midaCasella = 11; // Ancho y alto de cada cuadradito de color
    private final double espai = 3;        // Separación en píxeles entre cuadradito y cuadradito

    // Constructor: Recibe el Canvas donde se va a pintar la paleta
    public ColorPalette(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Borra el Canvas de colores y lo vuelve a pintar por completo.
     * Mueve verticalmente hacia arriba la casilla que coincida con el color actual del formulario.
     */
    public void redibuixar(Color colorActual) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Se limpia todo el Canvas de colores para que no queden restos de dibujos anteriores
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Bucle para dibujar las 8 casillas de colores en fila
        for (int i = 0; i < colors.length; i++) {
            // Se calcula la posición X de cada cuadradito basandose en su índice
            double x = i * (midaCasella + espai) + 4;
            
            // Posición base por defecto en el eje Y
            double y = 25;

            if (colors[i].equals(colorActual)) {
                y -= 8; // Sube la casilla 8 píxeles
            }

            // 1. Se rellena el cuadradito con su color correspondiente
            gc.setFill(colors[i]);
            gc.fillRect(x, y, midaCasella, midaCasella);
            
            // 2. Se le pinta un contorno negro muy fino para que se distingan colores claros como el amarillo
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeRect(x, y, midaCasella, midaCasella);
        }
    }

    /**
     * Este método recibe la coordenada X donde el usuario ha hecho clic dentro del Canvas
     * y calcula matemáticamente si ha pulsado sobre alguna de las casillas.
     * Si coincide, devuelve el objeto Color de esa casilla; si no, devuelve null.
     */
    public Color detectarColorClick(double clickX) {
        for (int i = 0; i < colors.length; i++) {
            // Se calcula dónde empieza la casilla horizontalmente en su eje X
            double cX = i * (midaCasella + espai) + 4;
            
            // Se comprueba si el clic del ratón entra dentro del ancho de este cuadradito
            if (clickX >= cX && clickX <= cX + midaCasella) {
                return colors[i]; // Se devuelve el color detectado
            }
        }
        return null; // Si hace clic fuera de cualquier casilla válida, no devuelve nada
    }
}
