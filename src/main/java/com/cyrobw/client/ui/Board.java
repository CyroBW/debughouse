package com.cyrobw.client.ui;

import com.cyrobw.client.BughouseBoard;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class Board extends Application {
    public static double MIN_SQUARE_SIZE = 10;
    public static double MAX_SQUARE_SIZE = 100;
    public double squareSize;
    public double scale;
    public boolean underPromote = false;
    private boolean playing = false;
    private final BorderPane boardPane = new BorderPane();
    private Pocket bottomPocket, topPocket;
    private Pockets leftPockets, rightPockets;
    private int pocketLayoutSetting = 0;
    private Clock bottomClock, topClock;
    public BughouseBoard gameState = new BughouseBoard();

    public Position position;
    public Side userSide = Side.WHITE;

    private double decorationWidth;
    private double decorationHeight;
    private final List<String> moveHistory = new ArrayList<>();

    public Rectangle[] squares = new Rectangle[64];
    public Rectangle[] lastMoveSquares = new Rectangle[64];
    public Rectangle[] premoveSquares = new Rectangle[64];
    public Rectangle[] outlineSquares = new Rectangle[64];
    private final ImageView cursorImage = new ImageView();
    public int[] times = new int[2];

    public String fen = gameState.getFen();
    public boolean userBoard;
    private int toX, toY;
    public int getDropPieceSelected() {
        return dropPieceSelected;
    }
    private int dropPieceSelected = 0;
    public Board(boolean userBoard) {
        this.userBoard = userBoard;
    }
    public void reset() {
        moveHistory.clear();
    }

    public void render() {
        position.render();
        renderHands();
    }

    public void setUserSide(Side side) {
        this.userSide = side;
    }

    public void setHand(String hand, Side side) {
        gameState.setHand(hand, side);
        setFen(this.fen);
        renderHands();
    }

    /**
     * Redraw both hands
     *
     */
    public void renderHands() {
        bottomPocket.render();
        topPocket.render();
        leftPockets.render();
        rightPockets.render();
    }

    /**
     * Update which clocks are running
     *
     */
    public void updateClockTurns() {
        if (gameState.sideToMove() != userSide) {
            bottomClock.stop();
            topClock.start();
        }
        else {
            topClock.stop();
            bottomClock.start();
        }
    }

    /**
     * Stops all clocks on board
     *
     */
    public void stopClocks() {
        bottomClock.stop();
        topClock.stop();
    }

    public void setTimes(String[] times, boolean flip) {
        this.times[0] = Integer.parseInt(times[0]);
        this.times[1] = Integer.parseInt(times[1]);
        if (!flip) {
            bottomClock.setTime(this.times[0]);
            topClock.setTime(this.times[1]);
        } else {
            bottomClock.setTime(this.times[1]);
            topClock.setTime(this.times[0]);
        }
    }

    public void createComponents() {
        boardPane.getChildren().clear();
        bottomPocket = new Pocket(this, userSide);
        topPocket = new Pocket(this, userSide.flip());

        bottomClock = new Clock(this, 1800);
        topClock = new Clock(this, 1800);

        GridPane topControls = new GridPane();
        topControls.addRow(0, topPocket, topClock);
        boardPane.setTop(topControls);

        GridPane bottomControls = new GridPane();
        bottomControls.addRow(0, bottomPocket, bottomClock);
        boardPane.setBottom(bottomControls);

        leftPockets = new Pockets(this, userSide);
        boardPane.setLeft(leftPockets);

        rightPockets = new Pockets(this, userSide);
        boardPane.setRight(rightPockets);

        BorderPane center = new BorderPane();
        drawBoard(center);
        drawCoordinates(center);
        position = new Position(this);
        center.getChildren().add(position);
        center.setPrefHeight(squareSize * 8);
        center.setPrefWidth(squareSize * 8);

        boardPane.setCenter(center);

        BorderPane.setMargin(topControls, new Insets(squareSize * 7 / 30, 0, squareSize * 7 / 30, 20 * scale + squareSize * 4 / 5));
        BorderPane.setMargin(bottomControls, new Insets(0, 0, squareSize * 5/8, 20 * scale + squareSize * 4 / 5));
        BorderPane.setMargin(leftPockets, new Insets(0, 10 * scale, 0, 10 * scale));
        BorderPane.setMargin(rightPockets, new Insets(0, 10 * scale, 0, 10 * scale));
        GridPane.setMargin(topPocket, new Insets(0, squareSize * 7/4, 0, 0));
        GridPane.setMargin(bottomPocket,  new Insets(0, squareSize * 7/4, 0, 0));
        boardPane.setStyle("-fx-background-color: #232323;");

        cursorImage.setFitWidth(squareSize * 4 / 5);
        cursorImage.setFitHeight(squareSize * 4 / 5);
        cursorImage.setMouseTransparent(true);
        boardPane.getChildren().add(cursorImage);

        boardPane.setOnMouseMoved(e -> {
            cursorImage.setX(e.getSceneX() - cursorImage.getBoundsInLocal().getWidth() / 2);
            cursorImage.setY(e.getSceneY() - cursorImage.getBoundsInLocal().getHeight() / 2);

            if (dropPieceSelected != 0) {
                if (!(toX < 0 || toX > 7 || toY < 0 || toY > 7)) {
                    outlineSquares[Square.fromValue(Character.toString('A' + toX) + (1 + toY)).ordinal()].setVisible(false);
                }
                Bounds boundsInScene = position.localToScene(boardPane.getBoundsInLocal());
                if (userSide.equals(Side.BLACK)) {
                    toX = 7 - (int) Math.floor((e.getSceneX() - boundsInScene.getMinX()) / squareSize);
                    toY = (int) Math.floor((e.getSceneY() - boundsInScene.getMinY()) / squareSize);
                }
                else {
                    toX = (int) Math.floor((e.getSceneX() - boundsInScene.getMinX()) / squareSize);
                    toY = 7 - (int) Math.floor((e.getSceneY() - boundsInScene.getMinY()) / squareSize);
                }
                if (!(toX < 0 || toX > 7 || toY < 0 || toY > 7)) {
                    outlineSquares[Square.fromValue(Character.toString('A' + toX) + (1 + toY)).ordinal()].setVisible(true);
                }
            }
        });

        boardPane.setOnMouseDragged(e -> {
            cursorImage.setX(e.getSceneX() - cursorImage.getBoundsInLocal().getWidth() / 2);
            cursorImage.setY(e.getSceneY() - cursorImage.getBoundsInLocal().getHeight() / 2);

            if (!(toX < 0 || toX > 7 || toY < 0 || toY > 7)) {
                outlineSquares[Square.fromValue(Character.toString('A' + toX) + (1 + toY)).ordinal()].setVisible(false);
            }
            Bounds boundsInScene = position.localToScene(boardPane.getBoundsInLocal());
            if (userSide.equals(Side.BLACK)) {
                toX = 7 - (int) Math.floor((e.getSceneX() - boundsInScene.getMinX()) / squareSize);
                toY = (int) Math.floor((e.getSceneY() - boundsInScene.getMinY()) / squareSize);
            }
            else {
                toX = (int) Math.floor((e.getSceneX() - boundsInScene.getMinX()) / squareSize);
                toY = 7 - (int) Math.floor((e.getSceneY() - boundsInScene.getMinY()) / squareSize);
            }
            if (!(toX < 0 || toX > 7 || toY < 0 || toY > 7)) {
                outlineSquares[Square.fromValue(Character.toString('A' + toX) + (1 + toY)).ordinal()].setVisible(true);
            }
        });

        boardPane.setOnMouseReleased((MouseEvent e) -> {
            if (!(toX < 0 || toX > 7 || toY < 0 || toY > 7)) {
                outlineSquares[Square.fromValue(Character.toString('A' + toX) + (1 + toY)).ordinal()].setVisible(false);
            }
            int pieceIndex = dropPieceSelected - 1;
            setCursorImage(null);
            if (pieceIndex == -1) {
                return;
            }
            String move;
            Bounds boundsInScene = position.localToScene(position.getBoundsInLocal());
            String to;
            if (userSide.equals(Side.BLACK)) {
                toX = 7 - (int) Math.floor((e.getSceneX() - boundsInScene.getMinX()) / squareSize);
                toY = (int) Math.floor((e.getSceneY() - boundsInScene.getMinY()) / squareSize);
            } else {
                toX = (int) Math.floor((e.getSceneX() - boundsInScene.getMinX()) / squareSize);
                toY = 7 - (int) Math.floor((e.getSceneY() - boundsInScene.getMinY()) / squareSize);
            }
            if (toX < 0 || toX > 7 || toY < 0 || toY > 7) {
                return;
            }
            to = Character.toString('A' + toX) + (1 + toY);
            move = new char[]{'P', 'N', 'B', 'R', 'Q'}[pieceIndex] + "@" + to;
            position.doMove(move);
        });

        pocketLayoutSetting--;
        togglePocketLocation();

        this.setFen(this.fen);
        render();
    }

    public void highlightSquare(Square sq) {
        lastMoveSquares[sq.ordinal()].setVisible(true);
    }

    public void unhighlightSquare(Square sq) {
        lastMoveSquares[sq.ordinal()].setVisible(false);
    }

    public void highlightPremove(String move) {
        if (move == null) {
            return;
        }
        Square to = Square.fromValue(move.substring(2, 4).toUpperCase());
        lastMoveSquares[to.ordinal()].setVisible(false);
        premoveSquares[to.ordinal()].setVisible(true);
        if (move.charAt(1) != '@') {
            Square from = Square.fromValue(move.substring(0, 2).toUpperCase());
            lastMoveSquares[from.ordinal()].setVisible(false);
            premoveSquares[from.ordinal()].setVisible(true);
        }
    }

    public void highlightLastMove(String move) {
        if (move == null) {
            return;
        }
        pushMove(move);
        Square to = Square.fromValue(move.substring(2, 4).toUpperCase());
        lastMoveSquares[to.ordinal()].setVisible(true);
        if (move.charAt(1) != '@') {
            Square from = Square.fromValue(move.substring(0, 2).toUpperCase());
            lastMoveSquares[from.ordinal()].setVisible(true);
        }
    }

    public void unhighlightAll() {
        for (int i = 0; i < 64; i++) {
            premoveSquares[i].setVisible(false);
            lastMoveSquares[i].setVisible(false);
        }
    }

    private void drawBoard(BorderPane pane) {
        for (int i = 0; i < 64; i++) {
            int x = i % 8;
            int y = i / 8;
            Rectangle square = new Rectangle();
            square.setWidth(squareSize);
            square.setHeight(squareSize);
            if (userSide.equals(Side.BLACK)) {
                square.setX(squareSize * (7 - x));
                square.setY(squareSize * y);
            } else {
                square.setX(squareSize * x);
                square.setY(squareSize * (7 - y));
            }
            if ((x + y) % 2 != 0) {
                square.setFill(Color.web("#dbe3e3"));
            } else {
                square.setFill(Color.web("#8ba3ab"));
            }
            squares[i] = square;
            pane.getChildren().add(square);
        }
        for (int i = 0; i < 64; i++) {
            int x = i % 8;
            int y = i / 8;
            Rectangle square = new Rectangle();
            square.setWidth(squareSize);
            square.setHeight(squareSize);
            if (userSide.equals(Side.BLACK)) {
                square.setX(squareSize * (7 - x));
                square.setY(squareSize * y);
            } else {
                square.setX(squareSize * x);
                square.setY(squareSize * (7 - y));
            }
            square.setStyle("-fx-fill: rgba(155,199,0,0.41);");
            square.setVisible(false);
            lastMoveSquares[i] = square;
            pane.getChildren().add(square);
        }
        for (int i = 0; i < 64; i++) {
            int x = i % 8;
            int y = i / 8;
            Rectangle square = new Rectangle();
            square.setWidth(squareSize);
            square.setHeight(squareSize);
            if (userSide.equals(Side.BLACK)) {
                square.setX(squareSize * (7 - x));
                square.setY(squareSize * y);
            } else {
                square.setX(squareSize * x);
                square.setY(squareSize * (7 - y));
            }
            square.setStyle("-fx-fill: rgba(20,30,85,0.5);");
            square.setVisible(false);
            premoveSquares[i] = square;
            pane.getChildren().add(square);
        }
        for (int i = 0; i < 64; i++) {
            int x = i % 8;
            int y = i / 8;
            Rectangle square = new Rectangle();
            square.setWidth(squareSize);
            square.setHeight(squareSize);
            if (userSide.equals(Side.BLACK)) {
                square.setX(squareSize * (7 - x));
                square.setY(squareSize * y);
            } else {
                square.setX(squareSize * x);
                square.setY(squareSize * (7 - y));
            }
            square.setFill(Color.TRANSPARENT);
            square.setStrokeWidth(2 * scale);
            square.setStroke(Color.SLATEGRAY);
            square.setVisible(false);
            outlineSquares[i] = square;
            pane.getChildren().add(square);
        }
    }

    /**
     * Draw board coordinates
     *
     */
    private void drawCoordinates(BorderPane pane) {
        Font font = Font.font("Sans-Serif", FontWeight.BOLD, 20 * scale);
        for (int i = 0; i < 8; i++) {
            Text text;
            if (userSide.equals(Side.WHITE)) {
                text = new Text(squareSize, squareSize, Character.toString('8' - i));
            } else {
                text = new Text(squareSize, squareSize, Character.toString('1' + i));
            }
            text.setFont(font);
            text.setX(2.0 * scale);
            text.setY(20.0 * scale + squareSize * i);
            if (i % 2 == 0) {
                text.setFill(Color.web("#8ba3ab"));
            } else {
                text.setFill(Color.web("#dbe3e3"));
            }
            pane.getChildren().add(text);
        }
        for (int i = 0; i < 8; i++) {
            Text text;
            if (userSide.equals(Side.WHITE)) {
                text = new Text(squareSize, squareSize, Character.toString('a' + i));
            } else {
                text = new Text(squareSize, squareSize, Character.toString('h' - i));
            }
            text.setFont(font);
            text.setX(82.0 * scale + squareSize * i);
            text.setY(-5.0 * scale + squareSize * 8);
            if (i % 2 != 0) {
                text.setFill(Color.web("#8ba3ab"));
            } else {
                text.setFill(Color.web("#dbe3e3"));
            }
            pane.getChildren().add(text);
        }
    }

    public void pushMove(String move) {
        moveHistory.add(move);
    }
    public String getLastMove() {
        if (moveHistory.isEmpty()) {
            return null;
        }
        String move = moveHistory.get(moveHistory.size() - 1);
        if (move.length() == 0) {
            return null;
        }
        return move;
    }
    public void setFen(String fen) {
        unhighlightAll();
        gameState.loadFromFen(fen);
        this.fen = fen;
        position.executePremoves();
        if (playing) {
            updateClockTurns();
        }
    }
    public void setCursorImage(Piece piece) {
        Image image = BoardField.pieceToImage.get(piece);
        cursorImage.setImage(image);
        if (image == null) {
            dropPieceSelected = 0;
            boardPane.setCursor(null);
            cursorImage.setVisible(false);
        } else {
            dropPieceSelected = piece.getPieceType().ordinal() + 1;
            boardPane.setCursor(Cursor.NONE);
            cursorImage.setVisible(true);
        }
        if (!(toX < 0 || toX > 7 || toY < 0 || toY > 7)) {
            outlineSquares[Square.fromValue(Character.toString('A' + toX) + (1 + toY)).ordinal()].setVisible(false);
        }
    }
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    private void setSquareSize(double squareSize) {
        this.squareSize = clamp(squareSize, MIN_SQUARE_SIZE, MAX_SQUARE_SIZE);
        this.scale = this.squareSize / MAX_SQUARE_SIZE;
        Preferences prefs = Preferences.userRoot().node("preferences");
        if (userBoard) {
            prefs.putDouble("left_board_square_size", this.squareSize);
        } else {
            prefs.putDouble("right_board_square_size", this.squareSize);
        }
    }
    private void resizeScene(Stage stage, double newWidth, double newHeight) {
        stage.setWidth(newWidth + this.decorationWidth);
        stage.setHeight(newHeight + this.decorationHeight);
    }

    /**
     * Toggles the displayed location of the pieces in hand.
     */
    private void togglePocketLocation() {
        pocketLayoutSetting = (pocketLayoutSetting + 1) % 3;
        switch (pocketLayoutSetting) {
            case 0 -> {
                topPocket.setVisible(true);
                bottomPocket.setVisible(true);
                rightPockets.setVisible(false);
                leftPockets.setVisible(false);
            }
            case 1 -> {
                topPocket.setVisible(false);
                bottomPocket.setVisible(false);
                rightPockets.setVisible(false);
                leftPockets.setVisible(true);
            }
            case 2 -> {
                topPocket.setVisible(false);
                bottomPocket.setVisible(false);
                rightPockets.setVisible(true);
                leftPockets.setVisible(false);
            }
        }

        Preferences prefs = Preferences.userRoot().node("preferences");
        if (userBoard) {
            prefs.putInt("left_board_pocket_location", pocketLayoutSetting);
        } else {
            prefs.putInt("right_board_pocket_location", pocketLayoutSetting);
        }
    }
    @Override
    public void start(Stage stage) {
        stage.setTitle("Debughouse Client");
        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                setCursorImage(null);
            }
        });

        Preferences prefs = Preferences.userRoot().node("preferences");
        if (userBoard) {
            pocketLayoutSetting = prefs.getInt("left_board_pocket_location", 0);
        } else {
            pocketLayoutSetting = prefs.getInt("right_board_pocket_location",0);
        }
        if (userBoard) {
            squareSize = prefs.getDouble("left_board_square_size", MAX_SQUARE_SIZE);
        } else {
            squareSize = prefs.getDouble("right_board_square_size", MAX_SQUARE_SIZE);
        }
        setSquareSize(squareSize);
        double initialSceneWidth = squareSize * 10;
        double initialSceneHeight = squareSize * 11;
        Scene scene = new Scene(boardPane, initialSceneWidth, initialSceneHeight);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        this.decorationWidth = stage.getWidth() - initialSceneWidth;
        this.decorationHeight = stage.getHeight() - initialSceneHeight;

        createComponents();

        scene.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
            if (key.getCode() == KeyCode.EQUALS) {
                setSquareSize(squareSize+1);
                resizeScene(stage, squareSize * 10, squareSize * 11);
                createComponents();
            }
            if (key.getCode() == KeyCode.MINUS) {
                setSquareSize(squareSize-1);
                resizeScene(stage, squareSize * 10, squareSize * 11);
                createComponents();
            }
            if (key.getCode() == KeyCode.H) {
                togglePocketLocation();
            }
            if (key.getCode() == KeyCode.DIGIT1) {
                if (dropPieceSelected != 1) {
                    if (userSide.equals(Side.WHITE)) {
                        setCursorImage(Piece.WHITE_PAWN);
                    } else {
                        setCursorImage(Piece.BLACK_PAWN);
                    }
                }
            }
            if (key.getCode() == KeyCode.DIGIT2) {
                if (dropPieceSelected != 2) {
                    if (userSide.equals(Side.WHITE)) {
                        setCursorImage(Piece.WHITE_KNIGHT);
                    } else {
                        setCursorImage(Piece.BLACK_KNIGHT);
                    }
                }
            }
            if (key.getCode() == KeyCode.DIGIT3) {
                if (dropPieceSelected != 3) {
                    if (userSide.equals(Side.WHITE)) {
                        setCursorImage(Piece.WHITE_BISHOP);
                    } else {
                        setCursorImage(Piece.BLACK_BISHOP);
                    }
                }
            }
            if (key.getCode() == KeyCode.DIGIT4) {
                if (dropPieceSelected != 4) {
                    if (userSide.equals(Side.WHITE)) {
                        setCursorImage(Piece.WHITE_ROOK);
                    } else {
                        setCursorImage(Piece.BLACK_ROOK);
                    }
                }
            }
            if (key.getCode() == KeyCode.DIGIT5) {
                if (dropPieceSelected != 5) {
                    if (userSide.equals(Side.WHITE)) {
                        setCursorImage(Piece.WHITE_QUEEN);
                    } else {
                        setCursorImage(Piece.BLACK_QUEEN);
                    }
                }
            }
            if (key.getCode() == KeyCode.ALT) {
                underPromote = true;
            }
            if (key.getCode() == KeyCode.Q) {
                if (key.isShiftDown()) {
                    Client.sendToChat(":bughouse-no-bq");
                } else {
                    Client.sendToChat(":bughouse-bq");
                }
            }
            if (key.getCode() == KeyCode.R) {
                if (key.isShiftDown()) {
                    Client.sendToChat(":bughouse-no-br");
                } else {
                    Client.sendToChat(":bughouse-br");
                }
            }
            if (key.getCode() == KeyCode.B) {
                if (key.isShiftDown()) {
                    Client.sendToChat(":bughouse-no-bb");
                } else {
                    Client.sendToChat(":bughouse-bb");
                }
            }
            if (key.getCode() == KeyCode.N) {
                if (key.isShiftDown()) {
                    Client.sendToChat(":bughouse-no-bn");
                } else {
                    Client.sendToChat(":bughouse-bn");
                }
            }
            if (key.getCode() == KeyCode.P) {
                if (key.isShiftDown()) {
                    Client.sendToChat(":bughouse-no-bp");
                } else {
                    Client.sendToChat(":bughouse-bp");
                }
            }
            if (key.getCode() == KeyCode.S) {
                Client.sendToChat(":bughouse-st");
            }
            if (key.getCode() == KeyCode.G) {
                Client.sendToChat(":bughouse-ns");
            }
            if (key.getCode() == KeyCode.M) {
                Client.sendToChat(":bughouse-mo");
            }
            if (key.getCode() == KeyCode.T) {
                if (key.isShiftDown()) {
                    Client.sendToChat(":bughouse-nt");
                } else {
                    Client.sendToChat(":bughouse-tp");
                }
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, (key) -> {
            if (key.getCode() == KeyCode.F5) {
                createComponents();
            }
            if (key.getCode() == KeyCode.DIGIT1) {
                if (dropPieceSelected == 1) {
                    setCursorImage(null);
                }
            }
            if (key.getCode() == KeyCode.DIGIT2) {
                if (dropPieceSelected == 2) {
                    setCursorImage(null);
                }
            }
            if (key.getCode() == KeyCode.DIGIT3) {
                if (dropPieceSelected == 3) {
                    setCursorImage(null);
                }
            }
            if (key.getCode() == KeyCode.DIGIT4) {
                if (dropPieceSelected == 4) {
                    setCursorImage(null);
                }
            }
            if (key.getCode() == KeyCode.DIGIT5) {
                if (dropPieceSelected == 5) {
                    setCursorImage(null);
                }
            }
            if (key.getCode() == KeyCode.ALT) {
                underPromote = false;
            }
        });
    }

    /**
     * Sets if board is active i.e. game in session
     *
     * @param value
     */
    public void setPlaying(boolean value) {
        this.playing = value;
    }

    /**
     * Returns if there is a game in session.
     *
     */
    public boolean isPlaying() {
        return this.playing;
    }
}