package com.cyrobw.client.ui;

import com.cyrobw.client.SoundPlayer;
import com.cyrobw.client.WebsocketClientEndpoint;
import com.github.bhlangonijr.chesslib.Side;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.prefs.Preferences;

public class Client extends Application {
    public static WebsocketClientEndpoint clientEndPoint;
    public static boolean connect = true;
    public static Board leftBoard;
    public static Board rightBoard;
    public static Chat chat;
    public static String username;
    public static String password;
    public static String ip;
    public static String host = "8080";

    @Override
    public void start(Stage stage) throws Exception {
        leftBoard = new Board(true);
        rightBoard = new Board(false);
        chat = new Chat();

        Stage stage1 = new Stage();
        stage1.initOwner(stage);

        Stage stage2 = new Stage();
        stage2.initOwner(stage);

        Stage stage3 = new Stage();
        stage3.initOwner(stage);

        leftBoard.start(stage1);
        rightBoard.start(stage2);
        chat.start(stage3);

        Preferences prefs = Preferences.userRoot().node("preferences");
        stage1.setOnCloseRequest(
                event -> {
                    prefs.putDouble("stage1_x", stage1.getX());
                    prefs.putDouble("stage1_y", stage1.getY());
                });

        stage2.setOnCloseRequest(
                event -> {
                    prefs.putDouble("stage2_x", stage2.getX());
                    prefs.putDouble("stage2_y", stage2.getY());
                });

        stage3.setOnCloseRequest(
                event -> {
                    prefs.putDouble("stage3_x", stage3.getX());
                    prefs.putDouble("stage3_y", stage3.getY());
                });

        stage1.setX(prefs.getDouble("stage1_x", 0));
        stage1.setY(prefs.getDouble("stage1_y", 0));

        stage2.setX(prefs.getDouble("stage2_x", leftBoard.squareSize * 10));
        stage2.setY(prefs.getDouble("stage2_y", 0));

        stage3.setX(prefs.getDouble("stage3_x", (leftBoard.squareSize * 10 + rightBoard.squareSize * 10 + Chat.WIDTH / 2)));
        stage3.setY(prefs.getDouble("stage3_y", (leftBoard.squareSize * 10.42 - Chat.HEIGHT) / 2));

        if (connect) {
            chat.receivedMessaged("Connecting to server...");
            connect(ip, host);
        }
    }

    /**
     * Relay message to chat.
     *
     * @param message
     */
    public static void sendToChat(String message) {
        chat.sendMessage(message);
    }

    /**
     * Send message to server.
     *
     * @param message
     */
    public static void sendToServer(String message) {
        if (connect) {
            clientEndPoint.sendMessage(message);
        }
    }

    /**
     * Connect to server and handle logic for callbacks.
     *
     * @param ip
     * @param host
     * @throws URISyntaxException
     */
    private void connect(String ip, String host) throws URISyntaxException {
        clientEndPoint = new WebsocketClientEndpoint(new URI("ws://" + ip + ":" + host + "/?username=" + username + "&password=" + password));
        clientEndPoint.addMessageHandler(message -> {
            if (message.equals("connected")) {
                Platform.runLater(() -> {
                    chat.receivedMessaged("Connected!");
                });
            }
            String[] args = message.split(" ", 2);
            switch (args[0]) {
                case "message" -> Platform.runLater(() -> {
                    chat.receivedMessaged(args[1]);
                });
                case "finished" -> Platform.runLater(() -> {
                    leftBoard.setPlaying(false);
                    leftBoard.position.cancelPremoves();
                    leftBoard.position.render();
                    leftBoard.renderHands();
                    leftBoard.reset();
                    leftBoard.stopClocks();
                    rightBoard.setPlaying(false);
                    rightBoard.reset();
                    rightBoard.stopClocks();
                    SoundPlayer.playSound("Checkmate.wav");
                });
                case "userside" -> {
                    Side userSide = Side.fromValue(args[1].toUpperCase());
                    Platform.runLater(() -> {
                        leftBoard.setPlaying(true);
                        leftBoard.setUserSide(userSide);
                        rightBoard.setPlaying(true);
                        rightBoard.setUserSide(userSide.flip());
                        leftBoard.createComponents();
                        rightBoard.createComponents();
                        SoundPlayer.playSound("Gamestart.wav");
                    });
                }
                case "fen1" -> Platform.runLater(() -> {
                    leftBoard.setFen(args[1]);
                    leftBoard.render();
                });
                case "fen2" -> Platform.runLater(() -> {
                    rightBoard.setFen(args[1]);
                    rightBoard.render();
                });
                case "move1" -> Platform.runLater(() -> {
                    if (args[1].length() > 0) {
                        leftBoard.pushMove(args[1]);
                    }
                });
                case "move2" -> Platform.runLater(() -> {
                    if (args[1].length() > 0) {
                        rightBoard.pushMove(args[1]);
                    }
                });
                case "whitehand1" -> Platform.runLater(() -> {
                    leftBoard.setHand(args[1].toUpperCase(), Side.WHITE);
                });
                case "blackhand1" -> Platform.runLater(() -> {
                    leftBoard.setHand(args[1].toLowerCase(), Side.BLACK);
                });
                case "whitehand2" -> Platform.runLater(() -> {
                    rightBoard.setHand(args[1].toUpperCase(), Side.WHITE);
                });
                case "blackhand2" -> Platform.runLater(() -> {
                    rightBoard.setHand(args[1].toLowerCase(), Side.BLACK);
                });
                case "times1" -> Platform.runLater(() -> {
                    leftBoard.setTimes(args[1].split(","), !leftBoard.userSide.equals(Side.WHITE));
                });
                case "times2" -> Platform.runLater(() -> {
                    rightBoard.setTimes(args[1].split(","), !rightBoard.userSide.equals(Side.WHITE));
                });
                case "players1" -> Platform.runLater(() -> {
                    String[] players = args[1].split(",");
                    if (leftBoard.userSide.equals(Side.WHITE)) {
                        leftBoard.setOurUser(players[0]);
                        leftBoard.setTheirUser(players[1]);
                    } else {
                        leftBoard.setOurUser(players[1]);
                        leftBoard.setTheirUser(players[0]);
                    }
                });
                case "players2" -> Platform.runLater(() -> {
                    String[] players = args[1].split(",");
                    if (rightBoard.userSide.equals(Side.WHITE)) {
                        rightBoard.setOurUser(players[0]);
                        rightBoard.setTheirUser(players[1]);
                    } else {
                        rightBoard.setOurUser(players[1]);
                        rightBoard.setTheirUser(players[0]);
                    }
                });
            }
        });
    }
    public static void main(String[] args) {
        launch(args);
    }
}
