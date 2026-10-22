package com.project;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class Controller {

    // Enumeración para saber en todo momento qué herramienta se tiene activa
    public enum Eina { TRAC_LLIURE, LINIA, RECTANGLE, MOURE }

    // 1. COMPONENTES VINCULADOS DIRECTAMENTE DESDE TU SCENE BUILDER
    @FXML private Canvas canvas;               // El lienzo izquierdo de 600x500
    @FXML private Canvas colorsCanvas;         // El lienzo inferior de 120x60
    @FXML private RadioButton thinRadio;       // Tu primer RadioButton
    @FXML private RadioButton thickRadio;      // Tu segundo RadioButton
    @FXML private ToggleGroup widthGroup;      // El grupo que unifica los RadioButtons

    // 2. COMPONENTES DINÁMICOS (Se crearán e inyectarán solos para no romper tu FXML)
    private ColorPicker cpLinia;               // Selector de color del contorno
    private ColorPicker cpEmplenament;         // Selector de color del fondo (para Rectángulos)
    private Spinner<Integer> spinnerAmple;     // Selector numérico del grosor del trazo
    private ListView<Stroke> listViewFormes;   // Lista visible de la derecha con los objetos creados
    private Button btnEliminar;                // Botón para borrar el objeto seleccionado

    // 3. VARIABLES DE ESTADO Y CONTROL INTERNO
    private Eina einaActual = Eina.TRAC_LLIURE; // Se empieza pintando trazo libre por defecto
    private final ObservableList<Stroke> llistaObjectes = FXCollections.observableArrayList(); // Lista en memoria de figuras
    private int comptadorFormes = 1;            // Contador para dar nombre automático (ej: "Línia 1")

    // Puntos de control para el ratón
    private Point2D puntInicialRatoli;          // Dónde se hace el primer clic
    private Point2D darrerPuntMoure;           // Última posición guardada en modo arrastrar
    private TracLliure tracActualProgres;       // Trazo libre temporal mientras se dibuja
    private Point2D fiProgres;                  // Punto final temporal de líneas o rectángulos
    
    private ColorPalette paletaColors;          // Instancia de nuestra clase del paso anterior

    /**
     * El método initialize() se ejecuta automáticamente cuando JavaFX carga el FXML.
     * Aquí se modifica y se recoloca tus componentes para cumplir con las exigencias del examen.
     */
    @FXML
    public void initialize() {
        // Vincula el Canvas de abajo con el gestor que pinta los colores predefinidos.
        paletaColors = new ColorPalette(colorsCanvas);

        // --- CAJA DE HERRAMIENTAS (Columna Central) ---
        // Cambia el texto de los dos textos de RadioButtons
        thinRadio.setText("Freehand Trace");
        thickRadio.setText("Line");

        // Crea un tercer RadioButton para el rectángulo y lo une al mismo grupo de exclusión.
        RadioButton rbRect = new RadioButton("Rectangle");
        rbRect.setToggleGroup(widthGroup);

        // Obtiene el panel central del FXML e introduce el nuevo botón de Rectangle en su posición.
        VBox vboxCentre = (VBox) thinRadio.getParent();
        vboxCentre.getChildren().add(3, rbRect); 

        // Crea los selectores visuales de color (contorno y fondo) y el selector numérico de grosor.
        cpLinia = new ColorPicker(Color.BLACK);            
        cpEmplenament = new ColorPicker(Color.LIGHTBLUE);  
        spinnerAmple = new Spinner<>(1, 50, 3);            
        spinnerAmple.setPrefWidth(120);

        // Inserta una línea divisoria, las etiquetas de texto y los selectores en el panel central.
        vboxCentre.getChildren().add(4, new Separator());
        vboxCentre.getChildren().add(5, new Label("Stroke Color:"));
        vboxCentre.getChildren().add(6, cpLinia);
        vboxCentre.getChildren().add(7, new Label("Fill Color:"));
        vboxCentre.getChildren().add(8, cpEmplenament);
        vboxCentre.getChildren().add(9, new Label("Line Width:"));
        vboxCentre.getChildren().add(10, spinnerAmple);

        // --- LISTA DE OBJETOS (Columna Derecha) ---
        // Accede al panel de la derecha navegando a través de la raíz principal del programa.
        HBox arrelHBox = (HBox) canvas.getParent();
        VBox vboxDreta = (VBox) arrelHBox.getChildren().get(2);

        // Crea el componente de la lista visual y lo enlaza con la colección de memoria.
        listViewFormes = new ListView<>(llistaObjectes);
        
        // Crea el botón para eliminar elementos y lo deja desactivado al inicio.
        btnEliminar = new Button("Delete Selected");
        btnEliminar.setDisable(true);
        btnEliminar.setMaxWidth(Double.MAX_VALUE);

        // Inserta la lista y el botón dentro del panel de la derecha.
        vboxDreta.getChildren().addAll(listViewFormes, btnEliminar);

        // --- ACCIONES Y EVENTOS ---
        // Asigna la función de cambiar de herramienta cuando se pulsa cada RadioButton.
        thinRadio.setOnAction(e -> canviarEina(Eina.TRAC_LLIURE));
        thickRadio.setOnAction(e -> canviarEina(Eina.LINIA));
        rbRect.setOnAction(e -> canviarEina(Eina.RECTANGLE));
        
        // Asigna la función de borrar el dibujo actual cuando se pulsa el botón de eliminar.
        btnEliminar.setOnAction(e -> eliminarObjecteSeleccionat());

        // Conecta los movimientos del ratón en el lienzo con las funciones de dibujo y arrastre.
        canvas.setOnMousePressed(this::gestionarMousePressed);
        canvas.setOnMouseDragged(this::gestionarMouseDragged);
        canvas.setOnMouseReleased(this::gestionarMouseReleased);
        
        // Conecta el clic del ratón sobre la paleta inferior para capturar el color seleccionado.
        colorsCanvas.setOnMousePressed(this::gestionarClickColors);

        // Hace que la paleta inferior se entere y se actualice sola si el usuario cambia el selector general.
        cpLinia.valueProperty().addListener((obs, vell, nou) -> paletaColors.redibuixar(nou));

        // Detecta si el usuario marca un objeto en la lista derecha para activar el modo de mover y borrar.
        listViewFormes.getSelectionModel().selectedItemProperty().addListener((obs, antic, nou) -> {
            if (nou != null) {
                einaActual = Eina.MOURE;
                btnEliminar.setDisable(false);
            } else {
                btnEliminar.setDisable(true);
            }
            redibuixarTot(); // Fuerza al lienzo a repintarse para mostrar las líneas discontinuas.
        });

        // Pinta la paleta de colores y el papel blanco por primera vez al arrancar la aplicación.
        paletaColors.redibuixar(cpLinia.getValue());
        redibuixarTot();
    }



    /**
     * Cambia la herramienta de dibujo activa y limpia cualquier selección previa
     */
    private void canviarEina(Eina novaEina) {
        einaActual = novaEina;
        listViewFormes.getSelectionModel().clearSelection(); // Se desmarca la lista
        redibuixarTot();
    }

    /**
     * Elimina la figura seleccionada tanto de la pantalla como de la lista de memoria
     */
    private void eliminarObjecteSeleccionat() {
        Stroke seleccionat = listViewFormes.getSelectionModel().getSelectedItem();
        if (seleccionat != null) {
            llistaObjectes.remove(seleccionat);
            listViewFormes.getSelectionModel().clearSelection();
            restablirEinaSegonsRadio();
            
            redibuixarTot();
        }
    }

    /**
     * Método auxiliar para sincronizar la herramienta con los RadioButtons visuales
     */
    private void restablirEinaSegonsRadio() {
        if (thinRadio.isSelected()) {
            einaActual = Eina.TRAC_LLIURE;
        } else if (thickRadio.isSelected()) {
            einaActual = Eina.LINIA;
        } else {
            // Buscamos el tercer RadioButton (Rectangle) que inyectamos dinámicamente en el grupo
            einaActual = Eina.RECTANGLE;
        }
    }

    /**
     * Escucha si tocas la paleta inferior y transfiere ese color al ColorPicker principal
     */
    private void gestionarClickColors(MouseEvent event) {
        Color detectat = paletaColors.detectarColorClick(event.getX());
        if (detectat != null) {
            cpLinia.setValue(detectat); // Actualiza el selector nativo
            paletaColors.redibuixar(detectat); // Vuelve a calcular qué casilla salta
        }
    }
    /**
     * Se ejecuta justo cuando el usuario hace el primer CLIC sobre el Canvas de dibujo.
     */
    private void gestionarMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        
        // Se guardan las coordenadas iniciales donde ha empezado el clic
        puntInicialRatoli = new Point2D(x, y);
        darrerPuntMoure = new Point2D(x, y); // Se usa en modo movimiento para calcular distancias

        // MODO MOVIMIENTO: Si se tiene una herramienta seleccionada en la lista
        if (einaActual == Eina.MOURE) {
            Stroke seleccionat = listViewFormes.getSelectionModel().getSelectedItem();
            if (seleccionat != null) {
                Requadre req = seleccionat.getRequadre(5); // Margen de 5 píxeles
                
                // Si el usuario hace clic FUERA del recuadro del objeto, lo deseleccionamos automáticamente
                if (req.conte(x, y)) {
                    return; 
                }
            }
        }

        // MODO DIBUJO: Si s'empieza a dibujar, se limpia cualquier selección previa de la lista
        boolean objecteTrobat = false;
        for (int i = llistaObjectes.size() - 1; i >= 0; i--) {
            Stroke f = llistaObjectes.get(i);
            if (f.getRequadre(5).conte(x, y)) {
                // Al seleccionarlo en la lista visual, el listener saltará,
                // activará el modo MOURE y habilitará el botón de borrar de inmediato.
                listViewFormes.getSelectionModel().select(f);
                einaActual = Eina.MOURE;
                objecteTrobat = true;
                break;
            }
        }

        // S'inicializa el objeto en progreso según la herramienta que esté activa
        if (!objecteTrobat) {
            // Limpiamos la selección de la lista de la derecha
            listViewFormes.getSelectionModel().clearSelection();
            
            // Restablecemos la herramienta de dibujo según el botón que esté marcado
            restablirEinaSegonsRadio();

            // Inicializamos la nueva figura correspondiente
            if (einaActual == Eina.TRAC_LLIURE) {
                tracActualProgres = new TracLliure("Freehand " + comptadorFormes, cpLinia.getValue(), spinnerAmple.getValue(), puntInicialRatoli);
            } else if (einaActual == Eina.LINIA || einaActual == Eina.RECTANGLE) {
                fiProgres = puntInicialRatoli;
            }
        }
    }

    /**
     * Se ejecuta de forma continua MIENTRAS el usuario mantiene pulsado el ratón y lo ARRASTRA.
     */
    private void gestionarMouseDragged(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        // MODO MOVIMIENTO: Si se arrastra una figura existente
        if (einaActual == Eina.MOURE) {
            Stroke seleccionat = listViewFormes.getSelectionModel().getSelectedItem();
            if (seleccionat != null) {
                // Se calcula cuántos píxeles se ha movido el ratón desde la última comprobación
                double dx = x - darrerPuntMoure.getX();
                double dy = y - darrerPuntMoure.getY();
                
                // Se desplaza las coordenadas internas del objeto
                seleccionat.moure(dx, dy);
                
                // Se actualiza el último punto de control para el siguiente ciclo del arrastre
                darrerPuntMoure = new Point2D(x, y);
                
                // Se refresca todo el lienzo para ver el desplazamiento fluido en pantalla
                redibuixarTot();
            }
            return;
        }

        // MODO DIBUJO: Se actualiza la forma fantasma en progreso para la previsualización interactiva
        if (einaActual == Eina.TRAC_LLIURE && tracActualProgres != null) {
            tracActualProgres.punts.add(new Point2D(x, y)); // Se añade el nuevo punto al trazo libre
            redibuixarTot();
        } else if (einaActual == Eina.LINIA || einaActual == Eina.RECTANGLE) {
            fiProgres = new Point2D(x, y); // Se actualiza el extremo final temporal
            redibuixarTot();
        }
    }

    /**
     * Se ejecuta en el momento exacto en que el usuario SUELTA el botón del ratón.
     */
    private void gestionarMouseReleased(MouseEvent event) {
        // Si estamos moviendo un objeto, no hay que guardar nada nuevo al soltar
        if (einaActual == Eina.MOURE) return;

        double x = event.getX();
        double y = event.getY();
        Stroke novaForma = null;

        // Se finaliza el objeto con su posición e información definitiva
        if (einaActual == Eina.TRAC_LLIURE && tracActualProgres != null) {
            tracActualProgres.punts.add(new Point2D(x, y));
            novaForma = tracActualProgres;
            tracActualProgres = null; // Se vacia la variable temporal
        } else if (einaActual == Eina.LINIA) {
            fiProgres = new Point2D(x, y);
            novaForma = new Linia("Line " + comptadorFormes, cpLinia.getValue(), spinnerAmple.getValue(), puntInicialRatoli, fiProgres);
            fiProgres = null;
        } else if (einaActual == Eina.RECTANGLE) {
            fiProgres = new Point2D(x, y);
            novaForma = new RectangleForma("Rectangle " + comptadorFormes, cpLinia.getValue(), cpEmplenament.getValue(), spinnerAmple.getValue(), puntInicialRatoli, fiProgres);
            fiProgres = null;
        }

        // Si se ha generado un objeto válido, se guarda permanentemente en la colección en memoria
        if (novaForma != null) {
            llistaObjectes.add(novaForma); // Esto hace que aparezca de inmediato en la lista visual de la derecha
            comptadorFormes++;            // Se incrementa el contador para el siguiente nombre
            redibuixarTot();              // Se vuelve a pintar
        }
    }

    /**
     * MOTOR DE REDIBUJO TOTAL DEL LIENZO
     * Borra el canvas principal por completo y vuelve a pintar todo en orden de capas estrictas.
     */
    private void redibuixarTot() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Se limpia los píxeles antiguos de toda la pantalla
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Se pinta un fondo blanco opaco limpio para simular el folio de papel del Paint
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // CAPA 1: Dibujar de forma fija todos los objetos que ya están completados y guardados en la lista
        for (Stroke f : llistaObjectes) {
            f.dibuixar(gc);
        }

        // CAPA 2: Dibujar las formas "fantasma" que se están trazando en tiempo real en este instante
        if (einaActual == Eina.TRAC_LLIURE && tracActualProgres != null) {
            tracActualProgres.dibuixar(gc);
        } else if (einaActual == Eina.LINIA && puntInicialRatoli != null && fiProgres != null) {
            gc.setStroke(cpLinia.getValue());
            gc.setLineWidth(spinnerAmple.getValue());
            gc.strokeLine(puntInicialRatoli.getX(), puntInicialRatoli.getY(), fiProgres.getX(), fiProgres.getY());
        } else if (einaActual == Eina.RECTANGLE && puntInicialRatoli != null && fiProgres != null) {
            // Se aplica las propiedades del formulario al rectángulo fantasma interactivo
            double x = Math.min(puntInicialRatoli.getX(), fiProgres.getX());
            double y = Math.min(puntInicialRatoli.getY(), fiProgres.getY());
            double w = Math.abs(fiProgres.getX() - puntInicialRatoli.getX());
            double h = Math.abs(fiProgres.getY() - puntInicialRatoli.getY());
            
            gc.setFill(cpEmplenament.getValue());
            gc.fillRect(x, y, w, h);
            gc.setStroke(cpLinia.getValue());
            gc.setLineWidth(spinnerAmple.getValue());
            gc.strokeRect(x, y, w, h);
        }

        // CAPA 3: Si hay un objeto seleccionado por el usuario en la lista derecha, pintamos su recuadro punteado
        Stroke seleccionat = listViewFormes.getSelectionModel().getSelectedItem();
        if (seleccionat != null) {
            Requadre req = seleccionat.getRequadre(5); // Se obtiene sus dimensiones envolventes con margen de holgura
            
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.2);
            
            gc.setLineDashes(6, 4); // 6 píxeles pintados, 4 píxeles transparentes de separación
            gc.strokeRect(req.minX, req.minY, req.maxX - req.minX, req.maxY - req.minY);
            
            // Importante: Se desactiva la línea discontinua volviendo a null para no estropear los siguientes dibujos estándar
            gc.setLineDashes(null); 
        }
    }
}
