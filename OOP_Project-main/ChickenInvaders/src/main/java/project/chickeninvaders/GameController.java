package project.chickeninvaders;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import project.chickeninvaders.entities.Chicken;
import project.chickeninvaders.entities.Ship;
import project.chickeninvaders.entities.ShipBullet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class GameController extends SceneController{
    Stage stage = MStage.getInstance().loadStage();
    private final Random RAND = new Random();
    public static int score, playerScore, chickenScore;
    public static GraphicsContext gc;
    int liveTicks;
    int maxChickens = 10, maxShots = maxChickens * 2;
    int playerSize = 60;
    boolean gamePause;
    double mouseX;
    Ship player;
    List<ShipBullet> bulletContainer;
    List<Chicken> chickenContainer;
    Image playerImg = new Image(GameController.class.getResource("img/other/ship.png").toString());
    Image[] chickenImg = {
            new Image(GameController.class.getResource("img/chicken/black.png").toString()),
            new Image(GameController.class.getResource("img/chicken/yellow.png").toString()),
            new Image(GameController.class.getResource("img/chicken/red.png").toString()),
            new Image(GameController.class.getResource("img/chicken/blue.png").toString()),
    };
    Image backgroundImg = new Image(GameController.class.getResource("img/other/background1.png").toString());

    // Waiting to add SFX

    //--Game Start--
    public void play() {
        Canvas canvas = new Canvas(MScene.width, MScene.height);
        gc = canvas.getGraphicsContext2D();
        Timeline timeline = new Timeline();
        KeyFrame frame = new KeyFrame(Duration.millis(50), e -> {
            try {
                if (run(gc)) {
                    timeline.stop();
                    OverController oc = new OverController();
                    oc.showScore();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        timeline.getKeyFrames().add(frame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Scene ingame = new Scene(new StackPane(canvas));

        //--Ship shoot via key pressed--
        ingame.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                switch (keyEvent.getCode()) {
                    case A, S:
                        if (bulletContainer.size() < maxShots)
                            //add bullet if current shots array size does not exceed maxShots
                            bulletContainer.add(player.shoot());
                        break;
                    case ESCAPE:
                        if (!gamePause) {
                            gamePause = true;
                            timeline.pause();
                            gc.setFont(Font.loadFont(getClass().getResource("font/upheavtt.ttf").toExternalForm(), 50));
                            gc.setTextAlign(TextAlignment.CENTER);
                            gc.setFill(Color.WHITE);
                            gc.fillText("PAUSE GAME", MScene.width / 2, MScene.height / 2);
                        } else {
                            gamePause = false;
                            gc.setFill(Color.TRANSPARENT);
                            timeline.play();
                        }
                }
            }
        });

        //--Check if stage is focus or not--
        stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                gc.setFill(Color.TRANSPARENT);
                timeline.play();
            } else {
                timeline.pause();
                gc.setFont(Font.loadFont(getClass().getResource("font/upheavtt.ttf").toExternalForm(), 50));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFill(Color.WHITE);
                gc.fillText("PAUSE GAME", MScene.width / 2, MScene.height / 2);
            }
        });

        //--Ship movement via mouse--
        ingame.setCursor(Cursor.MOVE);
        ingame.setOnMouseMoved(e -> mouseX = e.getX());
        ingame.setOnMouseClicked(e -> {
            if (bulletContainer.size() < maxShots)
                bulletContainer.add(player.shoot());
        });

        setup();
        stage.setScene(ingame);
        stage.show();
    }

    //--Game setup--
    private Chicken newChicken() { //function to create a new chicken object
        return new Chicken(50 + RAND.nextInt(MScene.width - 100), 0, playerSize,
                chickenImg[RAND.nextInt(chickenImg.length)]);
    }

    public void setup() {
        bulletContainer = new ArrayList<>();
        chickenContainer = new ArrayList<>();
        player = new Ship(MScene.width / 2, MScene.height - playerSize - 10, playerSize, playerImg);
        liveTicks = 6;
        playerScore = 0;
        chickenScore = 0;
        IntStream.range(0, maxChickens).mapToObj(i -> this.newChicken()).forEach(chickenContainer::add);
        //The IntStream.range() method is used to generate a sequence of integers from 0 to maxChickens - 1.
        //For each integer in the sequence, a new chicken object is created using the newChicken() method.
        //Then each get added to the chickens ArrayList using the forEach() method.
    }

    //--Run Graphics
    public boolean run(GraphicsContext gc) throws IOException {
        // setup background
        gc.drawImage(backgroundImg, 0, 0, MScene.width, MScene.height);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.loadFont(getClass().getResource("font/upheavtt.ttf").toExternalForm(), 20));
        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + score, 30, 20);
        gc.fillText("Lives: " + liveTicks / 2, 30, 40);

        // draw player
        player.update();
        player.draw();
        player.setX((int) mouseX);

        chickenContainer.stream().peek(Chicken::update).peek(Chicken::draw).forEach(e -> {
            if (player.collide(e) && !player.exploding) {
                e.explode();
                liveTicks--;
            }
            if (liveTicks == 1) {
                //sfx play here
                player.explode();
            }
        });

        for (int i = bulletContainer.size() - 1; i >= 0; i--) {
            ShipBullet shot = bulletContainer.get(i);
            if (shot.getY() < 0 || shot.getStatus()) {
                bulletContainer.remove(i);
                continue;
            }
            shot.update();
            shot.draw();
            for (Chicken chicken : chickenContainer) {
                if (shot.collide(chicken) && !chicken.exploding) {
                    playerScore += 2;
                    chicken.explode();
                    shot.setStatus(true);
                }
            }
        }

        for (Chicken chicken : chickenContainer) {
            if (chicken.getY() == MScene.height) {
                chickenScore += 4;
            }
        }

        for (int i = chickenContainer.size() - 1; i >= 0; i--) {
            if (chickenContainer.get(i).destroyed) {
                chickenContainer.set(i, newChicken());
            }
        }

        // assign total score
        score = playerScore - chickenScore;

        return player.destroyed || score < 0;
    }
}
