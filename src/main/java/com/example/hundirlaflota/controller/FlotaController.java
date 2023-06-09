package com.example.hundirlaflota.controller;

import com.example.hundirlaflota.FlotaApp;
import com.example.hundirlaflota.net.DatagramSocketClient;
import com.example.hundirlaflota.net.DatagramSocketServer;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.Pair;


import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FlotaController implements Initializable {
    @FXML
    private Label lblResponse;
    @FXML
    private Label infoGame;
    @FXML
    private Label namePlayer;
    private String nom;
    @FXML
    private GridPane gridPlayer;
    @FXML
    private GridPane gridEnemy;
    @FXML
    private ImageView imagenBanderaEnemigo;
    @FXML
    private ImageView imagenBanderaJugador;
    @FXML
    Circle circleServer;
    @FXML
    Circle circleClient;
    @FXML
    private Label counterPush;
    @FXML
    private Text counterPush2;
    List<Button> botones;
    private int pulsaciones = 0, pulsacionesEnemy = 0, contadorBarcos = 0, ultimoTurno, aciertos;
    private Button botonBlanco;
    @FXML
    private Button americanos;
    private String enemigo = "", responseGanador = "", numBoton = "", posicionBarcos = "", resp = "";


    //------------------------------------------------------------------------------------------------------------------------
    // Funcion para activar y  desactivar los botones de todos los gridPane
    @FXML
    private void panelBotones(ActionEvent event, boolean estado, GridPane grid) {
        // Obtener los botones del GridPane
        List<Button> botones = grid.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .collect(Collectors.toList());
        if (estado) {
            // Activar los botones
            botones.forEach(button -> button.setDisable(false));
            //Desactivar botones
        } else {
            botones.forEach(button -> button.setDisable(true));
        }
    }

    //------------------------------------------------------------------------------------------------------------------------
    // Funcion que sirve para desactivar el click de los botones que hemos pulsado con algun momento, ya sea que hemos colocado barco,
    // o bien que han sido pulsados por el enemigo.
    @FXML
    private void desactivarBotonesColor(ActionEvent event) {
        List<Button> botones = gridEnemy.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> button.getStyle().matches("-fx-background-color: (black|red|deepskyblue)"))
                .collect(Collectors.toList());

        botones.forEach(button -> button.setDisable(true));
    }

    //------------------------------------------------------------------------------------------------------------------------
    //Reflejar Jugada del enemigo en nuestro panel y nos pone rojo el boton en el que hayan hecho contacto
    @FXML
    private void reflejarJugada(String jugada, ActionEvent event) {
        // Obtener los botones del GridPane
        if (!jugada.equals("consultaTurno") && !jugada.contains("boton30") && !jugada.contains("gameover")) {
            String[] jugadaSplit;

            jugadaSplit = jugada.split(" ");
            System.out.println(jugada);
            String jugadaEnemy = jugadaSplit[1];
            jugadaEnemy = jugadaEnemy.replace("boton", "botonplayer");
            // Obtener los botones del GridPane
            botones = gridPlayer.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .collect(Collectors.toList());

            // Desactivar los botones
            Button miBoton = (Button) gridPlayer.lookup("#" + jugadaEnemy);
            if (miBoton.getStyle().equals("-fx-background-color: black")) {
                miBoton.setStyle("-fx-background-color: red");
                animateButtonCircle(miBoton, Color.YELLOW,1);



            } else {
                miBoton.setStyle("-fx-background-color: deepskyblue");
                animateButtonCircle(miBoton, Color.BLUE, 5);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------------
    //Creamos el Cliente y sobrescribimos los metodos para que reaccione a la respuesta del servidor
    //Inspirado en el adivinaFX del jordi en github
    DatagramSocketClient client = new DatagramSocketClient() {
        @Override
        public int getResponse(byte[] data, int length) {
            resp = new String(data, 0, length);
            String response = new String(data, 0, length);
            //Aqui recibimos la primera respuesta al enviar el boton de americanos y ponemos el turno a par
            if (response.contains("turno par")) {
                turnoPar = true;
                Platform.runLater(() -> lblResponse.setText(response));

            }
            // Aqui si el mensaje es gameover ponemos el texto de perdedor de la partida en el infoGame.
            else if (response.contains("gameover") && !gameWin) {
                String[] splitGanador = response.split(" ");
                responseGanador = splitGanador[0];
                stopClientTorn();
                Platform.runLater(() ->
                        infoGame.setText("You Lose. Winner: " + responseGanador));
                infoGame.setTextFill(Color.LAVENDER);
                Platform.runLater(() ->
                        showFinal(new ActionEvent()));

            }
            // Aqui si el mensaje es blanco ponemos el boton en rojo haciendo HIT en el blanco
            else if (response.equals("blanco")) {
                aciertos++;
                Platform.runLater(() -> counterPush.setText(String.valueOf(aciertos)));
                botonBlanco.setStyle("-fx-background-color: red");
                animateButtonCircle(botonBlanco, Color.YELLOW,1);
                //Si el numero de aciertos es igual al numero de casillas. Entonces  hemos ganado
                if (aciertos == 4) {
                    try {
                        String message = nom + " ganador"; // crea un mensaje con el valor actualizado del contador
                        client.send(message.getBytes()); // envía el mensaje al servidor
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() ->
                            showFinal(new ActionEvent()));
                    Platform.runLater(() ->
                            infoGame.setText("YOU WIN. Nice Job!"));
                    infoGame.setTextFill(Color.GOLD);
                    Platform.runLater(() ->
                            lblResponse.setText("Eres el Ganador"));
                    client.gameWin = true;
                }
            }
            //Aqui recibimos el numero de turno y lo gestionamos para saber si es nuesrtro turno o NO
            // Tambien comprobamos si la partida se ha acabado para no actualizar mas el turno
            else if (response.matches("[^a-zA-Z]+")) {

                int numTurno = Integer.parseInt(response);
                if (numTurno % 2 == 0 && turnoPar && numTurno != ultimoTurno && gameWin == false) {
                    turno = true;
                    stopClientTorn();
                    Platform.runLater(() -> {
                        infoGame.setTextFill(Color.GREEN);
                        infoGame.setText("Tu turno");
                        americanos.setDisable(true);
                        panelBotones(new ActionEvent(), true, gridEnemy);
                        desactivarBotonesColor(new ActionEvent());
                    });

                } else if (numTurno % 2 != 0 && !turnoPar && numTurno != ultimoTurno && !gameWin) {
                    turno = true;
                    stopClientTorn();
                    Platform.runLater(() -> {
                        infoGame.setTextFill(Color.GREEN);
                        americanos.setVisible(false);
                        infoGame.setText("Tu turno");
                        if (imagenBanderaEnemigo.getImage() == null) {
                            imagenBanderaJugador.setImage(new Image(FlotaApp.class.getResource("images/urss.png").toString()));
                            imagenBanderaEnemigo.setImage(new Image(FlotaApp.class.getResource("images/usa.png").toString()));
                        }
                        panelBotones(new ActionEvent(), true, gridEnemy);
                        desactivarBotonesColor(new ActionEvent());
                    });
                }
                ultimoTurno = numTurno;
            } else {
                String[] splitEnemy = response.split(" ");
                String responseEnemy = splitEnemy[0];

                    if (enemigo.equals("")){
                        enemigo = responseEnemy;
                        if(enemigo.equals(nom)){
                            String message = nom + " ganador"; // crea un mensaje con el valor actualizado del contador
                            try {
                                client.send(message.getBytes()); // envía el mensaje al servidor
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    Platform.runLater(() -> lblResponse.setText(responseEnemy));
                    reflejarJugada(response, new ActionEvent());
                }

            return length;
        }

        @Override
        public byte[] getRequest() {
            return String.valueOf(pulsacionesEnemy).getBytes();
        }

        @Override
        public boolean mustContinue(byte[] data) {
            return !resp.equals("Correcte");
        }
    };

    //------------------------------------------------------------------------------------------------------------------------
    //Handler que envia el boton pulsado al servidor y pinta el boton de azul hasta recibir confirmación si se ha hecho blanco
    @FXML
    protected void handleEnemyButtonAction(ActionEvent event) {
        Button button = (Button) event.getSource(); // obtiene el botón que ha generado el evento
        button.setStyle("-fx-background-color: deepskyblue");
        animateButtonCircle(button, Color.BLUE, 5);
        numBoton = button.getId(); // obtiene el número del botón a partir del ID
        botonBlanco = button;
        // Desactivar los botones
        panelBotones(event, false, gridEnemy);
        // Cambiamos el texto del turno
        infoGame.setTextFill(Color.RED);
        infoGame.setText("Turno del ENEMIGO");
        pulsacionesEnemy++;
        counterPush2.setText(String.valueOf(pulsacionesEnemy));
        try {
            String message = nom + " " + numBoton; // crea un mensaje con el valor actualizado del contador
            client.send(message.getBytes()); // envía el mensaje al servidor
            client.runClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------------------
    //handler para enviar los botones pulsados en nuestro tablero cuando sean los requeridos
    @FXML
    protected void handlePlayerButtonAction(ActionEvent event) throws IOException {
        Button button = (Button) event.getSource(); // obtiene el botón que ha generado el evento
        if (button.getId().contains("player")) {
            posicionBarcos += button.getId() + ",";
            contadorBarcos++;
            button.setStyle("-fx-background-color: black");
            animateButtonCircle(button,Color.WHITE, 3);
            if (contadorBarcos == 4) {
                panelBotones(new ActionEvent(), false, gridPlayer);
                posicionBarcos = posicionBarcos.substring(0, posicionBarcos.length() - 1);
                americanos.setDisable(false);
                client.send(posicionBarcos.getBytes());
                infoGame.setText("ELEGIR AMERICANOS");
                infoGame.setTextFill(Color.BLACK);
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------------
    //Funcion que al presionar americanos nos selecciona ese equipo y espera la respuesta del enemigo
    @FXML
    protected void handleAmericanos(ActionEvent event) throws IOException {
        panelBotones(new ActionEvent(), false, gridEnemy);
        String envio = nom + " boton30";
        botonBlanco = (Button) event.getSource();
        americanos.setVisible(false);
        infoGame.setText("Turno del ENEMIGO");
        infoGame.setTextFill(Color.RED);
        imagenBanderaJugador.setImage(new Image(FlotaApp.class.getResource("images/usa.png").toString()));
        imagenBanderaEnemigo.setImage(new Image(FlotaApp.class.getResource("images/urss.png").toString()));
        client.send(envio.getBytes());
    }

    //------------------------------------------------------------------------------------------------------------------------
    //Funcion para conectarse con unos parametros determinados al servidor
    @FXML
    public void menuItemConnection(ActionEvent actionEvent) {

        Dialog<Pair<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Client configuration");
        dialog.setHeaderText("Dades per la connexió al servidor");
        ButtonType conButton = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(conButton, ButtonType.CANCEL);
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));
        TextField txtName = new TextField("player1");
        TextField txtIp = new TextField("127.0.0.1");
        TextField txtPort = new TextField("5555");
        gridPane.add(new Label("Nom:"), 0, 0);
        gridPane.add(txtName, 1, 0);
        gridPane.add(new Label("IP:"), 0, 1);
        gridPane.add(txtIp, 1, 1);
        gridPane.add(new Label("Port:"), 0, 2);
        gridPane.add(txtPort, 1, 2);
        dialog.getDialogPane().setContent(gridPane);
        Platform.runLater(txtName::requestFocus);
        dialog.setResultConverter(dButton -> {
            if (dButton == conButton) {
                nom = txtName.getText();
                posicionBarcos += nom + " ";
                return new Pair<>(txtIp.getText(), Integer.parseInt(txtPort.getText()));
            }
            return null;
        });
        Optional<Pair<String, Integer>> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                client.init(result.get().getKey(), result.get().getValue());
                Thread.sleep(500);
                circleClient.setFill(Color.BLUE);
                lblResponse.setText("Esperando Enemigo");
                infoGame.setText(" ");
                namePlayer.setText(nom);
                panelBotones(new ActionEvent(), true, gridPlayer);
                infoGame.setText("COLOCA 3 BARCOS DE 2 PCS.");
                infoGame.setTextFill(Color.PURPLE);
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------------
    //Funcion que levanta el servidor en el puerto determinado
    public void menuItemActiveServer(ActionEvent actionEvent) {
        showConfigServer();
        circleServer.setFill(Color.BLUE);

    }

    //------------------------------------------------------------------------------------------------------------------------
    //Funcion que CONECTA con el servidor en la direccion, el puerto  y el nombre de usuario introducidos
    public void showConfigServer() {
        TextInputDialog dialog = new TextInputDialog("5555");
        dialog.setTitle("Config Server");
        dialog.setHeaderText("Activació del servidor local");
        dialog.setContentText("Port");
        dialog.setGraphic(new ImageView(FlotaApp.class.getResource("images/server.png").toString()));
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            DatagramSocketServer server = new DatagramSocketServer();
            Thread thServer = new Thread(() -> {
                try {
                    server.init(Integer.parseInt(result.get()));
                    server.runServer();
                } catch (SocketException e) {
                    Platform.runLater(() ->
                            infoGame.setText("Servidor ABIERTO en localhost"));
                    infoGame.setTextFill(Color.RED);
                    circleServer.setFill(Color.RED);

                } catch (IOException e) {

                }
            });
            thServer.start();
        }
    }

    //------------------------------------------------------------------------------------------------------------------------
    //Funcion que APAGA el programa al darle al MENU ITEM CLOSE
    public void clickClose(ActionEvent actionEvent) {
        System.exit(0);
    }

    //------------------------------------------------------------------------------------------------------------------------
    //Funcion que MUESTRA el Dialogo de FIN DE PARTIDA, llamará a clickClose() para cerrar programa al presionar el botón OK
    public void showFinal(ActionEvent actionEvent) {
        Dialog dialog = new Dialog<>();
        // Obtener el panel del diálogo
        DialogPane dialogPane = dialog.getDialogPane();
        // Establecer el estilo CSS del panel para cambiar la fuente y el tamaño
        dialogPane.setStyle("-fx-font-family: Impact; -fx-font-size: 24px;");
        //Comprobar booleano para indicar el texto
        if (client.gameWin) {
            dialog.setTitle("Eres el Ganador");
            dialog.setHeaderText("Felicidades " + nom);
            dialog.setContentText("Has ganado a " + enemigo.toUpperCase() + " con mucha superioridad");
        } else {
            dialog.setTitle("Has Perdido " + nom);
            dialog.setHeaderText(responseGanador + " te ha ganado ");
            dialog.setContentText("Has sido aplastado por el enemigo");

        }
        // Crear un botón y agregarlo al diálogo
        ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
        // Mostrar el diálogo
        Optional result = dialog.showAndWait();
        // Agregar un listener al resultado del diálogo
        result.ifPresent(pair -> {
            // Llamar a la función clickClose
            System.exit(0);
        });
    }

    //////---------------------------------------------------------------
    //----------------------Funcion que redondea los botones-----------------------------
    //------------------------------------------------------------------------------------------------------

    public void gridPaneRound(GridPane g) {
        GridPane gridPane = g; // Obtener referencia al GridPane
        ObservableList<Node> children = gridPane.getChildren(); // Obtener todos los hijos del GridPane
        for (Node node : children) {
            if (node instanceof Button) { // Si el nodo es un botón
                Button button = (Button) node;
                Circle circle = new Circle(); // Crear un círculo
                circle.setRadius(20); // Establecer el radio del círculo
                button.setShape(circle); // Establecer el círculo como forma personalizada del botón
            }
        }

    }

    //// Funcion que pone una imagen al boton ------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------
    public void setButtonImage(Button button, String imagePath) {
        Image image = new Image(FlotaApp.class.getResource(imagePath).toString());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(button.getWidth() / 2);
        imageView.setFitHeight(button.getHeight() / 2);
        button.setGraphic(imageView);
    }
//   Esta funcion pone una animación en el botón, hace que un circulo de un color que pasamos por parametro,
// ------Y se va haciendo grande o pequeño a la velocidad que le pasamops por parámetro
// ------------------------------------------------------------------------------------------------------------------------
    public void animateButtonCircle(Button button, Color color, int duration) {
        Circle circle = new Circle(button.getWidth() / 4, button.getHeight() / 4, button.getWidth() / 4, color);
        Platform.runLater(() ->
                button.setGraphic(circle));

        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(duration), circle);
        scaleTransition.setFromX(1);
        scaleTransition.setFromY(1);
        scaleTransition.setToX(0.5);
        scaleTransition.setToY(0.5);
        scaleTransition.setCycleCount(Timeline.INDEFINITE);
        scaleTransition.setAutoReverse(true);
        scaleTransition.play();
    }

////----------------------------------------------------------


    //------------------------------------------------------------------------------------------------------------------------
    //Funciones INITIALIZES
    public void initialize() {
        panelBotones(new ActionEvent(), false, gridPlayer);
        counterPush.setText(String.valueOf(pulsaciones));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        panelBotones(new ActionEvent(), false, gridEnemy);
        panelBotones(new ActionEvent(), false, gridPlayer);
        infoGame.setText("CONECTARSE AL SERVIDOR");
        infoGame.setTextFill(Color.WHITE);
        americanos.setDisable(true);
        gridPaneRound(gridEnemy);
        gridPaneRound(gridPlayer);

    }

}

