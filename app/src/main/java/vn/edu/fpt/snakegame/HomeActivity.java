package vn.edu.fpt.snakegame;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class HomeActivity extends AppCompatActivity {

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        preferences = getSharedPreferences("SnakeGamePrefs", MODE_PRIVATE);

        Button startGameBtn = findViewById(R.id.startGameBtn);
        Button settingsBtn = findViewById(R.id.settingsBtn);

        startGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGameTimeDialog();
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        // Display high scores
        updateHighScoreDisplay();
    }

    private void updateHighScoreDisplay() {
        TextView unlimitedHighScoreTV = findViewById(R.id.unlimitedHighScoreTV);
        TextView limitedHighScoreTV = findViewById(R.id.limitedHighScoreTV);

        String difficulty = preferences.getString("difficulty", "normal");

        int unlimitedHighScore = preferences.getInt("highScore_unlimited_" + difficulty, 0);
        int limitedHighScore = preferences.getInt("highScore_limited_" + difficulty, 0);

        unlimitedHighScoreTV.setText("Unlimited Mode: " + unlimitedHighScore);
        limitedHighScoreTV.setText("Timed Mode: " + limitedHighScore);
    }

    private void showGameTimeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_time);

        RadioGroup timeRadioGroup = dialog.findViewById(R.id.timeRadioGroup);
        RadioButton unlimitedRadio = dialog.findViewById(R.id.unlimitedRadio);
        RadioButton limitedRadio = dialog.findViewById(R.id.limitedRadio);
        final NumberPicker minutesPicker = dialog.findViewById(R.id.minutesPicker);
        Button nextBtn = dialog.findViewById(R.id.nextBtn);
        Button decreaseBtn = dialog.findViewById(R.id.decreaseBtn);
        Button increaseBtn = dialog.findViewById(R.id.increaseBtn);

        // Setup minutes picker
        minutesPicker.setMinValue(1);
        minutesPicker.setMaxValue(5);
        minutesPicker.setValue(2); // Default 2 minutes
        minutesPicker.setEnabled(false);

        timeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                minutesPicker.setEnabled(checkedId == R.id.limitedRadio);
                decreaseBtn.setEnabled(checkedId == R.id.limitedRadio);
                increaseBtn.setEnabled(checkedId == R.id.limitedRadio);
            }
        });

        // Default selection
        unlimitedRadio.setChecked(true);

        decreaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (minutesPicker.getValue() > minutesPicker.getMinValue()) {
                    minutesPicker.setValue(minutesPicker.getValue() - 1);
                }
            }
        });

        increaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (minutesPicker.getValue() < minutesPicker.getMaxValue()) {
                    minutesPicker.setValue(minutesPicker.getValue() + 1);
                }
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GameActivity.class);
                boolean isTimeLimited = limitedRadio.isChecked();
                intent.putExtra("isTimeLimited", isTimeLimited);

                if (isTimeLimited) {
                    int minutes = minutesPicker.getValue();
                    intent.putExtra("gameTimeMinutes", minutes);
                }

                dialog.dismiss();
                startActivity(intent);
            }
        });

        dialog.show();
    }

    private void showSettingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_settings);

        RadioGroup difficultyGroup = dialog.findViewById(R.id.difficultyGroup);
        RadioButton easyRadio = dialog.findViewById(R.id.easyRadio);
        RadioButton normalRadio = dialog.findViewById(R.id.normalRadio);
        RadioButton hardRadio = dialog.findViewById(R.id.hardRadio);

        RadioGroup colorGroup = dialog.findViewById(R.id.colorGroup);
        RadioButton yellowRadio = dialog.findViewById(R.id.yellowRadio);
        RadioButton greenRadio = dialog.findViewById(R.id.greenRadio);
        RadioButton blueRadio = dialog.findViewById(R.id.blueRadio);

        Button saveBtn = dialog.findViewById(R.id.saveBtn);
        Button cancelBtn = dialog.findViewById(R.id.cancelBtn);

        // Load current settings
        String currentDifficulty = preferences.getString("difficulty", "normal");
        String currentColor = preferences.getString("snakeColor", "yellow");

        // Set radio buttons based on saved settings
        switch (currentDifficulty) {
            case "easy":
                easyRadio.setChecked(true);
                break;
            case "normal":
                normalRadio.setChecked(true);
                break;
            case "hard":
                hardRadio.setChecked(true);
                break;
        }

        switch (currentColor) {
            case "yellow":
                yellowRadio.setChecked(true);
                break;
            case "green":
                greenRadio.setChecked(true);
                break;
            case "blue":
                blueRadio.setChecked(true);
                break;
        }

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();

                // Save difficulty
                int difficultyId = difficultyGroup.getCheckedRadioButtonId();
                if (difficultyId == R.id.easyRadio) {
                    editor.putString("difficulty", "easy");
                } else if (difficultyId == R.id.normalRadio) {
                    editor.putString("difficulty", "normal");
                } else if (difficultyId == R.id.hardRadio) {
                    editor.putString("difficulty", "hard");
                }

                // Save color
                int colorId = colorGroup.getCheckedRadioButtonId();
                if (colorId == R.id.yellowRadio) {
                    editor.putString("snakeColor", "yellow");
                } else if (colorId == R.id.greenRadio) {
                    editor.putString("snakeColor", "green");
                } else if (colorId == R.id.blueRadio) {
                    editor.putString("snakeColor", "blue");
                }

                editor.apply();
                dialog.dismiss();

                // Update the high score display with the new difficulty
                updateHighScoreDisplay();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}