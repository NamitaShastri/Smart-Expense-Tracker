package com.example.smartexpensetracker;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    EditText etAmount, etNote;
    TextView catEssentials, catLifestyle, catTransport, catUtilities;
    Button btnSave, btnScan;
    private Uri cameraImageUri;
    DatabaseHelper dbHelper;
    int currentUserId;

    String selectedCategory = "Essentials";
    Uri imageUri;
    ProgressDialog progressDialog;

    private static final int PICK_IMAGE = 1;
    private static final int CAMERA_REQUEST = 101;
    private static final int GALLERY_REQUEST = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Views
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);

        catEssentials = findViewById(R.id.catEssentials);
        catLifestyle = findViewById(R.id.catLifestyle);
        catTransport = findViewById(R.id.catTransport);
        catUtilities = findViewById(R.id.catUtilities);

        btnSave = findViewById(R.id.btnSave);
        btnScan = findViewById(R.id.btnScan);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Extracting amount...");
        progressDialog.setCancelable(false);

        dbHelper = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        setupCategorySelection();
        selectCategory("Essentials");

        btnSave.setOnClickListener(v -> saveExpense());

        // 📸 Scan button
        btnScan.setOnClickListener(v -> showImagePickerDialog());
    }

    // 💎 CATEGORY LOGIC
    private void setupCategorySelection() {
        catEssentials.setOnClickListener(v -> selectCategory("Essentials"));
        catLifestyle.setOnClickListener(v -> selectCategory("Lifestyle"));
        catTransport.setOnClickListener(v -> selectCategory("Transport"));
        catUtilities.setOnClickListener(v -> selectCategory("Utilities"));
    }

    private void selectCategory(String category) {
        selectedCategory = category;

        catEssentials.setBackgroundResource(R.drawable.card_bg);
        catLifestyle.setBackgroundResource(R.drawable.card_bg);
        catTransport.setBackgroundResource(R.drawable.card_bg);
        catUtilities.setBackgroundResource(R.drawable.card_bg);

        switch (category) {
            case "Essentials":
                catEssentials.setBackgroundResource(R.drawable.selected_card);
                break;
            case "Lifestyle":
                catLifestyle.setBackgroundResource(R.drawable.selected_card);
                break;
            case "Transport":
                catTransport.setBackgroundResource(R.drawable.selected_card);
                break;
            case "Utilities":
                catUtilities.setBackgroundResource(R.drawable.selected_card);
                break;
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option");

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else {
                openGallery();
            }
        });

        builder.show();
    }
    private void scanTextFromBitmap(Bitmap bitmap) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            TextRecognizer recognizer = TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
            );

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String text = result.getText();
                        String amount = extractAmount(text);

                        etAmount.setText(amount);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Scan Failed", Toast.LENGTH_SHORT).show()
                    );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        try {
            File file = new File(getExternalFilesDir(null), "camera_img.jpg");
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(intent, CAMERA_REQUEST);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // 💎 OPEN GALLERY
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST);
    }
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                "Temp",
                null
        );

        return Uri.parse(path);
    }
    // 💎 GET IMAGE RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == GALLERY_REQUEST && data != null) {
                Uri uri = data.getData();
                scanTextFromImage(uri);  // ✅ already working

            } else if (requestCode == CAMERA_REQUEST) {

                // 👉 USE SAVED URI (NOT data)
                if (cameraImageUri != null) {
                    scanTextFromImage(cameraImageUri);  // 🔥 THIS WAS MISSING
                } else {
                    Toast.makeText(this, "Camera image not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // 💎 OCR SCAN
    private void scanTextFromImage(Uri uri) {
        progressDialog.show();
        try {
            InputImage image = InputImage.fromFilePath(this, uri);

            TextRecognizer recognizer = TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
            );

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String text = result.getText();

                        Log.d("OCR_DEBUG", text);   //
                        String amount = extractAmount(text);

                        etAmount.setText(amount);
                        progressDialog.dismiss();

                        if (amount.isEmpty()) {
                            Toast.makeText(this, "Couldn't detect amount", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Scan Failed", Toast.LENGTH_SHORT).show();
                    });;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private double safeParse(String num) {
        try {
            if (num == null || num.isEmpty()) return 0;
            return Double.parseDouble(num);
        } catch (Exception e) {
            return 0;
        }
    }
    // 💎 KEYWORD + FALLBACK LOGIC
    private String extractAmount(String text) {

        String[] lines = text.split("\n");

        // STEP 1: Strong priority → TOTAL line
        for (String line : lines) {
            String lower = line.toLowerCase();

            if (lower.contains("total")) {
                String num = extractNumber(line);

                if (!num.isEmpty()) {
                    double val = safeParse(num);

                    if (val < 10000) {
                        return String.valueOf(val);
                    }
                }
            }
        }

        // STEP 2: fallback → filter valid numbers
        double best = 0;

        for (String line : lines) {

            // ❌ Ignore time lines
            if (line.contains(":")) continue;

            String num = extractNumber(line);

            if (!num.isEmpty()) {
                double val = safeParse(num);

                // Ignore huge numbers + small garbage
                if (val > 10 && val < 10000 && val > best) {
                    best = val;
                }
            }
        }

        return best > 0 ? String.valueOf(best) : "";
    }

    private String extractNumber(String line) {
        String[] words = line.split(" ");

        for (String word : words) {
            try {
                word = word.replaceAll("[^0-9.]", "");
                if (!word.isEmpty()) {
                    return word;
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    private double getMaxValidNumber(String text) {

        String[] words = text.split("\\s+");
        double max = 0;

        for (String word : words) {
            try {
                word = word.replaceAll("[^0-9.]", "");

                if (!word.isEmpty()) {
                    double val = safeParse(word);

                    // Ignore huge numbers
                    if (val < 10000 && val > max) {
                        max = val;
                    }
                }
            } catch (Exception ignored) {}
        }

        return max;
    }


    // 💎 SAVE EXPENSE
    private void saveExpense() {

        String amountStr = etAmount.getText().toString();
        String note = etNote.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = safeParse(amountStr);

        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", currentUserId);
        values.put("amount", amount);
        values.put("category", selectedCategory);
        values.put("note", note);
        values.put("date", date);
        values.put("imageUri", imageUri != null ? imageUri.toString() : "");

        long result = db.insert("expenses", null, values);

        if (result != -1) {
            Toast.makeText(this, "Expense Added", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error saving expense", Toast.LENGTH_SHORT).show();
        }
    }
}