package com.project;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase auxiliar Requadre (Bounding Box).
 * Sirve para calcular el rectángulo imaginario que envuelve a una figura.
 * Se usa para saber si el ratón hace clic dentro de un objeto seleccionado
 * y para dibujar las líneas discontinuas alrededor de él.
 */
class Requadre {
    double minX, minY, maxX, maxY; // Coordenadas de las esquinas del recuadro
    
    public Requadre(double minX, double minY, double maxX, double maxY) {
        this.minX = minX; 
        this.minY = minY; 
        this.maxX = maxX; 
        this.maxY = maxY;
    }
    
    /**
     * Este método comprueba si una coordenada (x, y) del ratón
     * cae dentro de los límites de este recuadro.
     */
    public boolean conte(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
}

/**
 * Clase base abstracta 'Stroke'.
 * Todos los objetos que se dibuje (Líneas, Rectángulos o Trazos libres) heredan de aquí.
 * El enunciado exige que cada objeto conserve sus propios colores y grosores de forma independiente.
 */
public abstract class Stroke {
    String id;               // Identificador del objeto (ej: "Línia 1", "Rectangle 2")
    Color colorLinia;        // Color del contorno o de la propia línea
    Color colorEmplenament;  // Color del fondo (solo se aplica a Rectángulos)
    double ampleLinia;       // Grosor de la línea (tiene que ser mayor que 0)

    // Constructor base que rellena las propiedades comunes de cualquier figura
    public Stroke(String id, Color colorLinia, Color colorEmplenament, double ampleLinia) {
        this.id = id;
        this.colorLinia = colorLinia;
        this.colorEmplenament = colorEmplenament;
        this.ampleLinia = ampleLinia;
    }

    // Método abstracto para que cada figura sepa pintarse a sí misma en el Canvas
    public abstract void dibuixar(GraphicsContext gc);
    
    // Método abstracto para calcular el recuadro exterior discontinuo con un margen de seguridad
    public abstract Requadre getRequadre(double marge);
    
    // Método abstracto para mover la figura sumando el desplazamiento (dx, dy) del ratón
    public abstract void moure(double dx, double dy);
    
    // Se cambia el método toString para que en la lista de JavaFX se vea el nombre del objeto (id)
    @Override 
    public String toString() { 
        return id; 
    }
}

/**
 * 1. IMPLEMENTACIÓN DEL TRAÇ LLIURE
 * Un trazo libre no es más que una lista ordenada de muchos puntos (x, y) por los que pasa el ratón.
 */
class TracLliure extends Stroke {
    // Lista dinámica donde se guardaran todos los puntos que recorra el ratón al arrastrarlo
    List<Point2D> punts = new ArrayList<>();

    public TracLliure(String id, Color cl, double ample, Point2D inici) {
        // El trazo libre no tiene color de fondo, por eso se pone TRANSPARENT
        super(id, cl, Color.TRANSPARENT, ample);
        punts.add(inici); // Se guarda el primer punto donde el usuario hizo clic
    }

    @Override
    public void dibuixar(GraphicsContext gc) {
        if (punts.size() < 2) return; // Si no hay al menos 2 puntos, no se puede dibujar una línea
        
        gc.setStroke(colorLinia); // Se aplica su color guardado
        gc.setLineWidth(ampleLinia); // Se aplica su grosor guardado
        
        gc.beginPath(); // Se inicia el camino de dibujo
        gc.moveTo(punts.get(0).getX(), punts.get(0).getY()); // Se pone el lápiz en el primer punto
        
        // Se recorre todos los puntos guardados uniendo cada uno con el siguiente
        for (int i = 1; i < punts.size(); i++) {
            gc.lineTo(punts.get(i).getX(), punts.get(i).getY());
        }
        gc.stroke(); // Se pinta la línea definitiva en el lienzo
    }

    @Override
    public Requadre getRequadre(double marge) {
        // Se busca los valores mínimos y máximos de X e Y entre TODOS los puntos del trazo libre
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        
        for (Point2D p : punts) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getY() > maxY) maxY = p.getY();
        }
        // Se devuelve el recuadro aplicando el margen y la mitad del grosor de la línea
        return new Requadre(minX - marge - ampleLinia/2, minY - marge - ampleLinia/2, 
                             maxX + marge + ampleLinia/2, maxY + marge + ampleLinia/2);
    }

    @Override
    public void moure(double dx, double dy) {
        // Para mover un trazo libre, se suma el desplazamiento a cada uno de sus puntos en la lista
        for (int i = 0; i < punts.size(); i++) {
            Point2D p = punts.get(i);
            punts.set(i, new Point2D(p.getX() + dx, p.getY() + dy));
        }
    }
}

/**
 * 2. IMPLEMENTACIÓN DE LA LÍNIA RECTA
 * Una línea recta solo necesita saber dónde empieza (inici) y dónde acaba (fi).
 */
class Linia extends Stroke {
    Point2D inici, fi; // Punto inicial y punto final

    public Linia(String id, Color cl, double ample, Point2D inici, Point2D fi) {
        super(id, cl, Color.TRANSPARENT, ample); // Tampoco tiene color de fondo
        this.inici = inici; 
        this.fi = fi;
    }

    @Override
    public void dibuixar(GraphicsContext gc) {
        gc.setStroke(colorLinia);
        gc.setLineWidth(ampleLinia);
        // Dibuja una línea directa del punto inicial al final
        gc.strokeLine(inici.getX(), inici.getY(), fi.getX(), fi.getY());
    }

    @Override
    public Requadre getRequadre(double marge) {
        // El mínimo y máximo se calcula simplemente comparando los dos extremos
        double minX = Math.min(inici.getX(), fi.getX());
        double maxX = Math.max(inici.getX(), fi.getX());
        double minY = Math.min(inici.getY(), fi.getY());
        double maxY = Math.max(inici.getY(), fi.getY());
        
        return new Requadre(minX - marge - ampleLinia/2, minY - marge - ampleLinia/2, 
                             maxX + marge + ampleLinia/2, maxY + marge + ampleLinia/2);
    }

    @Override
    public void moure(double dx, double dy) {
        // Se desplaza ambos extremos de la línea
        inici = new Point2D(inici.getX() + dx, inici.getY() + dy);
        fi = new Point2D(fi.getX() + dx, fi.getY() + dy);
    }
}

/**
 * 3. IMPLEMENTACIÓN DEL RECTANGLE
 * Guarda la posición inicial del clic y la final. Para dibujarlo calcula
 * la esquina superior izquierda (mínimos) y las distancias absolutas (ancho y alto).
 */
class RectangleForma extends Stroke {
    Point2D inici, fi;

    public RectangleForma(String id, Color cl, Color ce, double ample, Point2D inici, Point2D fi) {
        super(id, cl, ce, ample); // El rectángulo sí usa el color de fondo (ce)
        this.inici = inici; 
        this.fi = fi;
    }

    @Override
    public void dibuixar(GraphicsContext gc) {
        // Fórmulas requeridas por el enunciado para poder arrastrar en cualquier dirección
        double x = Math.min(inici.getX(), fi.getX());
        double y = Math.min(inici.getY(), fi.getY());
        double w = Math.abs(fi.getX() - inici.getX());
        double h = Math.abs(fi.getY() - inici.getY());

        // Primero se rellena el fondo del rectángulo
        gc.setFill(colorEmplenament);
        gc.fillRect(x, y, w, h);
        
        // Después se pinta el contorno
        gc.setStroke(colorLinia);
        gc.setLineWidth(ampleLinia);
        gc.strokeRect(x, y, w, h);
    }

    @Override
    public Requadre getRequadre(double marge) {
        // Se calcula sus esquinas para saber el recuadro de selección
        double x = Math.min(inici.getX(), fi.getX());
        double y = Math.min(inici.getY(), fi.getY());
        double w = Math.abs(fi.getX() - inici.getX());
        double h = Math.abs(fi.getY() - inici.getY());
        
        return new Requadre(x - marge - ampleLinia/2, y - marge - ampleLinia/2, 
                             x + w + marge + ampleLinia/2, y + h + marge + ampleLinia/2);
    }

    @Override
    public void moure(double dx, double dy) {
        // Se mueve los dos puntos de control del rectángulo
        inici = new Point2D(inici.getX() + dx, inici.getY() + dy);
        fi = new Point2D(fi.getX() + dx, fi.getY() + dy);
    }
}
