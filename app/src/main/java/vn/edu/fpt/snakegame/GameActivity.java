package vn.edu.fpt.snakegame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
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

    private final List<SnakePoints> snakePointsList = new ArrayList<>();
    private SurfaceView surfaceView;
    private TextView scoreTV;
    private TextView timerTV;
    private TextView highScoreTV;

    private SurfaceHolder surfaceHolder;
    private String movingPosition = "right";
    private int score = 0;
    private static final int pointSize = 28;
    private static final int defaultTalePoints = 3;
    private int snakeColor = Color.YELLOW;
    private int snakeMovingSpeed = 800;
    private int positionX, positionY;
    private Timer timer;
    private CountDownTimer gameTimer;
    private boolean isTimeLimited = false;
    private long gameTimeMillis = 0;

    private SharedPreferences preferences;
    private String currentDifficulty;
    private String timeMode;
    private int highScore;

    private Canvas canvas = null;
    private Paint pointColor = null;

    private boolean isPaused = false;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        surfaceView = findViewById(R.id.surfaceView);
        scoreTV = findViewById(R.id.scoreTV);
        timerTV = findViewById(R.id.timerTV);
        highScoreTV = findViewById(R.id.highScoreTV);

        surfaceView.getHolder().addCallback(this);

        preferences = getSharedPreferences("SnakeGamePrefs", MODE_PRIVATE);
        currentDifficulty = preferences.getString("difficulty", "normal");

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

        switch (currentDifficulty) {
            case "easy":
                snakeMovingSpeed = 600;
                break;
            case "hard":
                snakeMovingSpeed = 900;
                break;
            default:
                snakeMovingSpeed = 800;
                break;
        }

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

        dbHelper = new DatabaseHelper(this);
        highScore = dbHelper.getHighScore(timeMode, currentDifficulty);
        highScoreTV.setText("Best: " + highScore);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_pause) {
            showPauseDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPauseDialog() {
        isPaused = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        builder.setTitle("Game Paused");
        builder.setMessage("Do you want to continue or exit?");
        builder.setCancelable(false);
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isPaused = false;
                startGameTimer();
                moveSnake();
            }
        });
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(GameActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.show();
            }
        });
    }

    private void startGameTimer() {
        if (isTimeLimited && gameTimeMillis > 0) {
            gameTimer = new CountDownTimer(gameTimeMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (!isPaused) {
                        gameTimeMillis = millisUntilFinished;
                        updateTimer(millisUntilFinished);
                    }
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
        this.surfaceHolder = surfaceHolder;
        init();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }

    private void init() {
        snakePointsList.clear();
        scoreTV.setText("0");
        score = 0;
        movingPosition = "right";
        int startPositionX = (pointSize) * defaultTalePoints;

        for (int i = 0; i < defaultTalePoints; i++) {
            SnakePoints snakePoints = new SnakePoints(startPositionX, pointSize);
            snakePointsList.add(snakePoints);
            startPositionX = startPositionX - (pointSize * 2);
        }

        addPoint();
        moveSnake();
    }

    private void addPoint() {
        int surfaceWidth = surfaceView.getWidth() - (pointSize * 2);
        int surfaceHeight = surfaceView.getHeight() - (pointSize * 2);

        int randomXPosition = new Random().nextInt(surfaceWidth / pointSize);
        int randomYPosition = new Random().nextInt(surfaceHeight / pointSize);

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
                if (isPaused) return;

                int headPositionX = snakePointsList.get(0).getPositionX();
                int headPositionY = snakePointsList.get(0).getPositionY();

                if (headPositionX == positionX && headPositionY == positionY) {
                    growSnake();
                    addPoint();

                    if (isTimeLimited && gameTimer != null && gameTimeMillis > 30000) {
                        gameTimer.cancel();
                        gameTimeMillis -= 30000;
                        startGameTimer();
                    }
                }

                switch (movingPosition) {
                    case "right":
                        snakePointsList.get(0).setPositionX(headPositionX + (pointSize * 2));
                        snakePointsList.get(0).setPositionY(headPositionY);
                        break;
                    case "left":
                        snakePointsList.get(0).setPositionX(headPositionX - (pointSize * 2));
                        snakePointsList.get(0).setPositionY(headPositionY);
                        break;
                    case "top":
                        snakePointsList.get(0).setPositionX(headPositionX);
                        snakePointsList.get(0).setPositionY(headPositionY - (pointSize * 2));
                        break;
                    case "bottom":
                        snakePointsList.get(0).setPositionX(headPositionX);
                        snakePointsList.get(0).setPositionY(headPositionY + (pointSize * 2));
                        break;
                }

                if (checkGameOver(headPositionX, headPositionY)) {
                    gameOver();
                } else {
                    try {
                        canvas = surfaceHolder.lockCanvas();
                        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
                        canvas.drawColor(Color.BLACK);
                        canvas.drawCircle(snakePointsList.get(0).getPositionX(), snakePointsList.get(0).getPositionY(), pointSize, createPointColor());

                        Paint foodPaint = new Paint();
                        foodPaint.setColor(Color.RED);
                        foodPaint.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(positionX, positionY, pointSize, foodPaint);

                        for (int i = 1; i < snakePointsList.size(); i++) {
                            int getTempPositionX = snakePointsList.get(i).getPositionX();
                            int getTempPositionY = snakePointsList.get(i).getPositionY();
                            snakePointsList.get(i).setPositionX(headPositionX);
                            snakePointsList.get(i).setPositionY(headPositionY);
                            canvas.drawCircle(snakePointsList.get(i).getPositionX(), snakePointsList.get(i).getPositionY(), pointSize, createPointColor());
                            headPositionX = getTempPositionX;
                            headPositionY = getTempPositionY;
                        }

                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 1000 - snakeMovingSpeed, 1000 - snakeMovingSpeed);
    }

    private void growSnake() {
        SnakePoints snakePoints = new SnakePoints(0, 0);
        snakePointsList.add(snakePoints);
        score++;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scoreTV.setText(String.valueOf(score));
                if (score > highScore) {
                    highScore = score;
                    highScoreTV.setText("Best: " + highScore);
                }
            }
        });

        if (isTimeLimited && gameTimer != null && gameTimeMillis > 30000) {
            gameTimer.cancel();
            gameTimeMillis -= 30000;
            startGameTimer();
        }
    }

    private boolean checkGameOver(int headPositionX, int headPositionY) {
        boolean gameOver = false;

        if (snakePointsList.get(0).getPositionX() < 0 ||
                snakePointsList.get(0).getPositionY() < 0 ||
                snakePointsList.get(0).getPositionX() >= surfaceView.getWidth() ||
                snakePointsList.get(0).getPositionY() >= surfaceView.getHeight()) {
            gameOver = true;
        } else {
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
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        builder.setMessage("Your Score = " + score);
        builder.setTitle("Game Over");
        builder.setCancelable(false);
        builder.setPositiveButton("Start Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                init();
            }
        });
        builder.setNegativeButton("Back to Menu", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(GameActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.show();
            }
        });

        if (score > highScore) {
            dbHelper.insertHighScore(timeMode, currentDifficulty, score);
        }
    }

    private Paint createPointColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(snakeColor);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }

        return pointColor;
    }
}