package com.example.smartexpensetracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ExpenseDB";
    private static final int DATABASE_VERSION = 1;

    // USERS TABLE
    public static final String TABLE_USERS = "users";
    public static final String U_ID = "id";
    public static final String U_NAME = "name";
    public static final String U_EMAIL = "email";
    public static final String U_PASSWORD = "password";
    public static final String U_PHONE = "phone";
    public static final String U_IMAGE = "profileImage";

    // EXPENSE TABLE
    public static final String TABLE_EXPENSES = "expenses";
    public static final String E_ID = "id";
    public static final String E_USER_ID = "user_id";
    public static final String E_AMOUNT = "amount";
    public static final String E_CATEGORY = "category";
    public static final String E_NOTE = "note";
    public static final String E_DATE = "date";
    public static final String E_IMAGE = "imageUri";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create Users Table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                U_NAME + " TEXT, " +
                U_EMAIL + " TEXT UNIQUE, " +
                U_PASSWORD + " TEXT, " +
                U_PHONE + " TEXT, " +
                U_IMAGE + " TEXT" +
                ")";

        // Create Expenses Table
        String createExpensesTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                E_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                E_USER_ID + " INTEGER, " +
                E_AMOUNT + " REAL, " +
                E_CATEGORY + " TEXT, " +
                E_NOTE + " TEXT, " +
                E_DATE + " TEXT, " +
                E_IMAGE + " TEXT" +
                ")";

        db.execSQL(createUsersTable);
        db.execSQL(createExpensesTable);

        insertDummyUser(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        onCreate(db);
    }

    public void insertDummyUser(SQLiteDatabase db) {

        db.execSQL("INSERT OR IGNORE INTO users (name, email, password, phone, profileImage) VALUES (" +
                "'Namita'," +
                "'namita.shastri@itmbu.ac.in'," +
                "'1234'," +
                "'9106307620'," +
                "''" +
                ")");
    }
}