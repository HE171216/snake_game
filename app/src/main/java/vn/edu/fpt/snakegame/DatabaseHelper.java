package vn.edu.fpt.snakegame;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "snake_game.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_HIGH_SCORES = "high_scores";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_MODE = "mode";
    public static final String COLUMN_DIFFICULTY = "difficulty";
    public static final String COLUMN_SCORE = "score";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_HIGH_SCORES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MODE + " TEXT, " +
                    COLUMN_DIFFICULTY + " TEXT, " +
                    COLUMN_SCORE + " INTEGER" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HIGH_SCORES);
        onCreate(db);
    }

    public void insertHighScore(String mode, String difficulty, int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MODE, mode);
        values.put(COLUMN_DIFFICULTY, difficulty);
        values.put(COLUMN_SCORE, score);
        db.insert(TABLE_HIGH_SCORES, null, values);
    }

    public int getHighScore(String mode, String difficulty) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HIGH_SCORES,
                new String[]{COLUMN_SCORE},
                COLUMN_MODE + "=? AND " + COLUMN_DIFFICULTY + "=?",
                new String[]{mode, difficulty},
                null, null, COLUMN_SCORE + " DESC",
                "1");
        if (cursor != null && cursor.moveToFirst()) {
            int highScore = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE));
            cursor.close();
            return highScore;
        }
        return 0;
    }
}