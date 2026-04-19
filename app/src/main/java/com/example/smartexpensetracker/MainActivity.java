package com.example.smartexpensetracker;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    TextView tvName, tvTotal;
    ShapeableImageView profileIcon;
    Button btnAdd, btnHistory;

    DatabaseHelper dbHelper;
    int currentUserId;
    private static final int PICK_IMAGE_PROFILE = 100;
    private Uri selectedImageUri;
    private ImageView dialogProfileImage; // IMPORTANT

    TextView tvCount, tvAvg;
    TextView tvDailyBudget;
    ProgressBar progressBudget;

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_PROFILE);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_SmartExpenseTracker);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        // Initialize views
        tvName = findViewById(R.id.tvName);
        tvTotal = findViewById(R.id.tvTotal);
        LinearLayout headerLayout = findViewById(R.id.headerLayout);
        headerLayout.setOnClickListener(v -> showProfileDialog());
        btnAdd = findViewById(R.id.btnAdd);
        tvCount = findViewById(R.id.tvCount);
        tvAvg = findViewById(R.id.tvAvg);
        btnHistory = findViewById(R.id.btnHistory);
        tvDailyBudget = findViewById(R.id.tvDailyBudget);
        progressBudget = findViewById(R.id.progressBudget);
        TextView tvGreeting = findViewById(R.id.tvGreeting);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour < 12) tvGreeting.setText("Good Morning ☀️");
        else if (hour < 18) tvGreeting.setText("Good Afternoon 🌤");
        else tvGreeting.setText("Good Evening 🌙");

        dbHelper = new DatabaseHelper(this);
        TextView tvSetLimit = findViewById(R.id.tvSetLimit);

        tvSetLimit.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            EditText input = new EditText(this);
            input.setHint("Enter daily limit");

            builder.setTitle("Set Daily Limit");
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {

                float limit = Float.parseFloat(input.getText().toString());

                SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                prefs.edit().putFloat("daily_limit", limit).apply();

                loadDailyBudget();
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        // 💎 Get logged-in user
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        // Load user name
        loadUserName();

        // Load total spending (basic for now)
        loadTotal();


        // loading daily limiy
        loadDailyBudget();
        // Profile click
//        profileIcon.setOnClickListener(v -> showProfileDialog());

        // Navigation
        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class))
        );

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class))
        );
    }

    // 💎 Load user name from DB
    private void loadUserName() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT name FROM users WHERE id=?",
                new String[]{String.valueOf(currentUserId)}
        );

        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            tvName.setText("Hi, " + name + " 👋");
        }

        cursor.close();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadTotal();      // 🔥 refresh total
        loadUserName();   // optional (for profile updates)
        loadDailyBudget(); // daily score loading
    }

    // 💎 Load total expense
    private void loadTotal() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount), COUNT(*) FROM expenses " +
                        "WHERE user_id=? " +
                        "AND date >= date('now','start of month') " +
                        "AND date <= date('now')",
                new String[]{String.valueOf(currentUserId)}
        );

        double total = 0;
        int count = 0;

        if (cursor.moveToFirst()) {

            if (!cursor.isNull(0)) {
                total = cursor.getDouble(0);
            }

            count = cursor.getInt(1);
        }

        tvTotal.setText("₹" + total);
        tvCount.setText(count + " expenses");

        double avg = count == 0 ? 0 : total / count;
        tvAvg.setText("Avg ₹" + String.format("%.2f", avg));

        cursor.close();
    }


    private void checkDailyLimitAndNotify(double total, float limit) {

        if (limit > 0 && total >= limit && total < limit + 50) {
            showBudgetNotification();
        }
    }

    // 💎 Profile Dialog
    private void showProfileDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_profile);
//        dialogProfileImage = dialog.findViewById(R.id.profileImage);
//        dialogProfileImage.setOnClickListener(v -> openGallery());
        dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etName = dialog.findViewById(R.id.etName);
        EditText etEmail = dialog.findViewById(R.id.etEmail);
        EditText etPhone = dialog.findViewById(R.id.etPhone);
        Button btnSave = dialog.findViewById(R.id.btnSaveProfile);
        Button btnLogout = dialog.findViewById(R.id.btnLogout);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT name, email, phone FROM users WHERE id=?",
                new String[]{String.valueOf(currentUserId)}
        );

        if (cursor.moveToFirst()) {
            etName.setText(cursor.getString(0));
            etEmail.setText(cursor.getString(1));
            etPhone.setText(cursor.getString(2));
        }
        cursor.close();
        btnLogout.setOnClickListener(v -> {

            // Clear session
            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
            prefs.edit().clear().apply();

            // Close dialog
            dialog.dismiss();

            // Go to login screen
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {

            SQLiteDatabase dbWrite = dbHelper.getWritableDatabase();

            dbWrite.execSQL(
                    "UPDATE users SET name=?, email=?, phone=? WHERE id=?",
                    new Object[]{
                            etName.getText().toString(),
                            etEmail.getText().toString(),
                            etPhone.getText().toString(),
                            currentUserId

                    }
            );

            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();

            loadUserName(); // update UI
            dialog.dismiss();
        });

        dialog.show();
    }
    private void loadDailyBudget() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount) FROM expenses WHERE user_id=? AND date = date('now')",
                new String[]{String.valueOf(currentUserId)}
        );

        double total = 0;

        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }

        cursor.close();

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        float limit = prefs.getFloat("daily_limit", 0);

        tvDailyBudget.setText("₹" + total + " / ₹" + limit);

        if (limit > 0) {
            int progress = (int) ((total / limit) * 100);
            progressBudget.setProgress(progress);

            checkDailyLimitAndNotify(total, limit); // 🔥 THIS WAS MISSING
        }
    }
    private void showBudgetNotification() {

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "budget_alert")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Budget Limit Reached ⚠️")
                .setContentText("You have exceeded your daily spending limit!")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(this).notify(1, builder.build());
    }

    private void createNotificationChannel() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    "budget_alert",
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Alerts when budget exceeds");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }


}