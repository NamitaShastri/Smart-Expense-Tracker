package com.example.smartexpensetracker;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword;
    Button btnAction;
    TextView tvToggle;

    boolean isLogin = true;

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        if (userId != -1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return; // IMPORTANT
        }
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnAction = findViewById(R.id.btnAction);
        tvToggle = findViewById(R.id.tvToggle);

        dbHelper = new DatabaseHelper(this);

        // Toggle Login / Signup
        tvToggle.setOnClickListener(v -> {
            isLogin = !isLogin;

            if (isLogin) {
                etName.setVisibility(View.GONE);
                btnAction.setText("Login");
                tvToggle.setText("New user? Sign up");
            } else {
                etName.setVisibility(View.VISIBLE);
                btnAction.setText("Sign Up");
                tvToggle.setText("Already have account? Login");
            }
        });

        btnAction.setOnClickListener(v -> {
            if (isLogin) {
                loginUser();
            } else {
                signupUser();
            }
        });
    }

    // 💎 LOGIN FUNCTION
    private void loginUser() {

        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE email=? AND password=?",
                new String[]{email, password}
        );

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(0);

            // Save logged in user
            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
            prefs.edit().putInt("user_id", userId).apply();

            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class));
            finish();

        } else {
            Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
    }

    // 💎 SIGNUP FUNCTION
    private void signupUser() {

        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);

        long result = db.insert("users", null, values);

        if (result != -1) {
            Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show();

            // Switch to login mode
            isLogin = true;
            etName.setVisibility(View.GONE);
            btnAction.setText("Login");
            tvToggle.setText("New user? Sign up");

        } else {
            Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
        }
    }
}
