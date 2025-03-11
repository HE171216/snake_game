package vn.edu.fpt.snakegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    // list of snake points / snake length
    private final List<SnakePoints> snakePointsList = new ArrayList<>();
    private SurfaceView surfaceView;
    private TextView scoreTV;
    private TextView timerTV;
    private TextView highScoreTV;

    // surface holder to draw snake on surface's canvas
    private SurfaceHolder surfaceHolder;

    // snake moving position. Values must be right, left, top, bottom.
    // By default snake moves to the right
    private String movingPosition = "right";

    // score
    private int score = 0;

    // snake size / point size
    private static final int pointSize = 28;

    // default snake tale
    private static final int defaultTalePoints = 3;

    // snake color - now dynamic based on settings
    private int snakeColor = Color.YELLOW;

    // snake moving speed. Value must lie between 1 - 1000
    private int snakeMovingSpeed = 800;

    // random point position coordinates on the surfaceView
    private int positionX, positionY;

    // timer to move snake / change snake position after a specific time (snakeMovingSpeed)
    private Timer timer;

    // Timer for game countdown
    private CountDownTimer gameTimer;
    private boolean isTimeLimited = false;
    private long gameTimeMillis = 0;

    // Game settings
    private SharedPreferences preferences;
    private String currentDifficulty;
    private String timeMode;
    private int highScore;

    // canvas to draw snake and show on surface view
    private Canvas canvas = null;

    // point color / single point color of a snake
    private Paint pointColor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // getting surfaceview and score TextView from xml file
        surfaceView = findViewById(R.id.surfaceView);
        scoreTV = findViewById(R.id.scoreTV);
        timerTV = findViewById(R.id.timerTV);
        highScoreTV = findViewById(R.id.highScoreTV);

        // Add this line to register the SurfaceHolder.Callback
        surfaceView.getHolder().addCallback(this);

        // Load settings
        preferences = getSharedPreferences("SnakeGamePrefs", MODE_PRIVATE);
        currentDifficulty = preferences.getString("difficulty", "normal");

        // Set snake color
        String colorPref = preferences.getString("snakeColor", "yellow");
        switch (colorPref) {
            case "green":
                snakeColor = Color.GREEN;
                break;
            case "blue":
                snakeColor = Color.BLUE;
                break;
            default:
                snakeColor = Color.YELLOW;
                break;
        }

        // Set snake speed based on difficulty
        switch (currentDifficulty) {
            case "easy":
                snakeMovingSpeed = 600; // Slower
                break;
            case "hard":
                snakeMovingSpeed = 900; // Faster
                break;
            default:
                snakeMovingSpeed = 800; // Normal
                break;
        }

        // Check if game is time-limited
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isTimeLimited = extras.getBoolean("isTimeLimited", false);
            if (isTimeLimited) {
                int minutes = extras.getInt("gameTimeMinutes", 2);
                gameTimeMillis = minutes * 60 * 1000;
                timeMode = "limited";
                timerTV.setVisibility(View.VISIBLE);
                startGameTimer();
            } else {
                timeMode = "unlimited";
                timerTV.setVisibility(View.GONE);
            }
        }

        // Load high score for current difficulty and time mode
        highScore = preferences.getInt("highScore_" + timeMode + "_" + currentDifficulty, 0);
        highScoreTV.setText("Best: " + highScore);

        // getting ImageButtons from xml file
        final AppCompatImageButton topBtn = findViewById(R.id.topBtn);
        final AppCompatImageButton leftBtn = findViewById(R.id.leftBtn);
        final AppCompatImageButton rightBtn = findViewById(R.id.rightBtn);
        final AppCompatImageButton bottomBtn = findViewById(R.id.bottomBtn);

        topBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!movingPosition.equals("bottom")) {
                    movingPosition = "top";
                }
            }
        });

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!movingPosition.equals("right")) {
                    movingPosition = "left";
                }
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!movingPosition.equals("left")) {
                    movingPosition = "right";
                }
            }
        });

        bottomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!movingPosition.equals("top")) {
                    movingPosition = "bottom";
                }
            }
        });
    }

    private void startGameTimer() {
        if (isTimeLimited && gameTimeMillis > 0) {
            gameTimer = new CountDownTimer(gameTimeMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    gameTimeMillis = millisUntilFinished;
                    updateTimer(millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    gameOver();
                }
            }.start();
        }
    }

    private void updateTimer(long millisUntilFinished) {
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        int minutes = (int) (millisUntilFinished / (1000 * 60));
        timerTV.setText(String.format("%02d:%02d", minutes, seconds));
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        // when surface is created then get surfaceHolder from it and assign to surfaceholder
        this.surfaceHolder = surfaceHolder;

        // init for snake / surfaceview
        init();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // Not needed for this implementation
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // Cancel the timer when surface is destroyed to avoid memory leaks
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }

    private void init() {
        // clear snake points / snake length
        snakePointsList.clear();

        // set default score as 0
        scoreTV.setText("0");

        // make score 0
        score = 0;

        // setting default moving position
        movingPosition = "right";

        // default snake stating position on the screen
        int startPositionX = (pointSize) * defaultTalePoints;

        // making snake's default length / points
        for (int i = 0; i < defaultTalePoints; i++) {
            // adding point to snake's tale
            SnakePoints snakePoints = new SnakePoints(startPositionX, pointSize);
            snakePointsList.add(snakePoints);

            // increasing value for next point as snake's tale
            startPositionX = startPositionX - (pointSize * 2);
        }

        // add random point on the screen to be eaten by the snake
        addPoint();

        // start moving snake / start game
        moveSnake();
    }

    private void addPoint() {
        // getting surfaceView width and height to add point on the surface to be eaten by the snake
        int surfaceWidth = surfaceView.getWidth() - (pointSize * 2);
        int surfaceHeight = surfaceView.getHeight() - (pointSize * 2);

        int randomXPosition = new Random().nextInt(surfaceWidth / pointSize);
        int randomYPosition = new Random().nextInt(surfaceHeight / pointSize);

        // check if randomXPosition is even or odd value. We need only even number
        if ((randomXPosition % 2) != 0) {
            randomXPosition = randomXPosition + 1;
        }

        if ((randomYPosition % 2) != 0) {
            randomYPosition = randomYPosition + 1;
        }

        positionX = (pointSize * randomXPosition) + pointSize;
        positionY = (pointSize * randomYPosition) + pointSize;
    }

    private void moveSnake() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // getting head position
                int headPositionX = snakePointsList.get(0).getPositionX();
                int headPositionY = snakePointsList.get(0).getPositionY();

                // check if snake eaten a point
                if (headPositionX == positionX && headPositionY == positionY) {
                    // grow snake after eaten point
                    growSnake();

                    // add another random point on the screen
                    addPoint();

                    // If time-limited mode, reduce time by 30 seconds for each point eaten
                    if (isTimeLimited && gameTimer != null && gameTimeMillis > 30000) {
                        gameTimer.cancel();
                        gameTimeMillis -= 30000; // Reduce by 30 seconds
                        startGameTimer();
                    }
                }

                // check of which side snake is moving
                switch (movingPosition) {
                    case "right":
                        // move snake's head to right.
                        // other points follow snake's head point to move the snake
                        snakePointsList.get(0).setPositionX(headPositionX + (pointSize * 2));
                        snakePointsList.get(0).setPositionY(headPositionY);
                        break;
                    case "left":
                        // move snake's head to left.
                        // other points follow snake's head point to move the snake
                        snakePointsList.get(0).setPositionX(headPositionX - (pointSize * 2));
                        snakePointsList.get(0).setPositionY(headPositionY);
                        break;
                    case "top":
                        // move snake's head to top.
                        // other points follow snake's head point to move the snake
                        snakePointsList.get(0).setPositionX(headPositionX);
                        snakePointsList.get(0).setPositionY(headPositionY - (pointSize * 2));
                        break;
                    case "bottom":
                        // move snake's head to bottom.
                        // other points follow snake's head point to move the snake
                        snakePointsList.get(0).setPositionX(headPositionX);
                        snakePointsList.get(0).setPositionY(headPositionY + (pointSize * 2));
                        break;
                }

                // check if game over. Whether snake touches edges or itself
                if (checkGameOver(headPositionX, headPositionY)) {
                    // Game over logic is now in a separate method
                    gameOver();
                } else {
                    try {
                        // lock canvas on surfaceHolder to draw on it
                        canvas = surfaceHolder.lockCanvas();

                        // clear canvas with black color
                        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
                        // Set a background color
                        canvas.drawColor(Color.BLACK);

                        // change snake's head position
                        canvas.drawCircle(snakePointsList.get(0).getPositionX(), snakePointsList.get(0).getPositionY(), pointSize, createPointColor());

                        // draw random point circle on the surface to be eaten by the snake
                        Paint foodPaint = new Paint();
                        foodPaint.setColor(Color.RED);  // Make food a different color
                        foodPaint.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(positionX, positionY, pointSize, foodPaint);

                        // other points following snake's head
                        for (int i = 1; i < snakePointsList.size(); i++) {
                            int getTempPositionX = snakePointsList.get(i).getPositionX();
                            int getTempPositionY = snakePointsList.get(i).getPositionY();

                            // move points across the head
                            snakePointsList.get(i).setPositionX(headPositionX);
                            snakePointsList.get(i).setPositionY(headPositionY);
                            canvas.drawCircle(snakePointsList.get(i).getPositionX(), snakePointsList.get(i).getPositionY(), pointSize, createPointColor());

                            // change head position
                            headPositionX = getTempPositionX;
                            headPositionY = getTempPositionY;
                        }

                        // unlock canvas to draw on surfaceView
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 1000 - snakeMovingSpeed, 1000 - snakeMovingSpeed);
    }

    private void growSnake() {
        // create new snake point
        SnakePoints snakePoints = new SnakePoints(0, 0);

        // add point to the snake's tale
        snakePointsList.add(snakePoints);

        // increase score
        score++;

        // setting score to TextView
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scoreTV.setText(String.valueOf(score));

                // Check if current score is higher than high score
                if (score > highScore) {
                    highScore = score;
                    highScoreTV.setText("Best: " + highScore);
                }
            }
        });
    }

    private boolean checkGameOver(int headPositionX, int headPositionY) {
        boolean gameOver = false;

        // check if snake's head touches edges
        if (snakePointsList.get(0).getPositionX() < 0 ||
                snakePointsList.get(0).getPositionY() < 0 ||
                snakePointsList.get(0).getPositionX() >= surfaceView.getWidth() ||
                snakePointsList.get(0).getPositionY() >= surfaceView.getHeight()) {
            gameOver = true;
        } else {
            // check if snake's head touches snake itself
            for (int i = 1; i < snakePointsList.size(); i++) {
                if (headPositionX == snakePointsList.get(i).getPositionX() &&
                        headPositionY == snakePointsList.get(i).getPositionY()) {
                    gameOver = true;
                    break;
                }
            }
        }

        return gameOver;
    }

    private void gameOver() {
        // stop timer / stop moving snake
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }

        // show game over dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        builder.setMessage("Your Score = " + score);
        builder.setTitle("Game Over");
        builder.setCancelable(false);
        builder.setPositiveButton("Start Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // restart game / re-init data
                init();
            }
        });

        // timer runs in background so we need to show dialog on main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.show();
            }
        });

        // Save high score if it's greater than the current high score
        SharedPreferences.Editor editor = preferences.edit();
        if (score > highScore) {
            editor.putInt("highScore_" + timeMode + "_" + currentDifficulty, score);
        }
        editor.apply();
    }

    private Paint createPointColor() {
        // check if color not defined before
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(snakeColor);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true); // smoothness
        }

        return pointColor;
    }
}