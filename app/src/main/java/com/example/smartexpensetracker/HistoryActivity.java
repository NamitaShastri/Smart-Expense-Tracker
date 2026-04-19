package com.example.smartexpensetracker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

public class HistoryActivity extends AppCompatActivity {

    ListView listView;
    DatabaseHelper dbHelper;

    ArrayList<String> expenseList;
    ArrayList<Integer> expenseIdList = new ArrayList<>();

    int currentUserId;

    TextView tvEmpty;
    String selectedCustomDate = "";
    Spinner spinnerFilter;
    String selectedFilter = "All";

    String rangeStartDate = "";
    String rangeEndDate = "";
    TextView tvFilter;
    ArrayList<String> amountList = new ArrayList<>();
    ArrayList<String> categoryList = new ArrayList<>();
    ArrayList<String> dateList = new ArrayList<>();
    ArrayList<String> noteList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        tvEmpty = findViewById(R.id.tvEmpty);
        listView = findViewById(R.id.listView);
        listView.setOnItemLongClickListener((parent, view, position, id) -> {

            showDeleteDialog(position);

            return true;
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            showDetailDialog(position);
        });

        dbHelper = new DatabaseHelper(this);

        tvFilter = findViewById(R.id.tvFilter);

        tvFilter.setOnClickListener(v -> {

            String[] options = {"All", "Today", "This Month", "Pick Date", "Date Range"};

            new AlertDialog.Builder(this, R.style.MyDialogTheme)
                    .setTitle("Filter")
                    .setItems(options, (dialog, which) -> {

                        if (options[which].equals("Pick Date")) {

                            Calendar calendar = Calendar.getInstance();

                            DatePickerDialog datePicker = new DatePickerDialog(
                                    this,
                                    R.style.MyDialogTheme,
                            (view, year, month, dayOfMonth) -> {

                                        String selectedDate = year + "-" +
                                                String.format("%02d", month + 1) + "-" +
                                                String.format("%02d", dayOfMonth);

                                        selectedFilter = "CUSTOM";
                                        selectedCustomDate = selectedDate;

                                        tvFilter.setText(selectedDate + " ⌄");

                                        loadExpenses();
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                            );


                            datePicker.show();

                        } else if (options[which].equals("Date Range")) {

                            Calendar calendar = Calendar.getInstance();

                            DatePickerDialog startPicker = new DatePickerDialog(
                                    this,
                                    R.style.MyDialogTheme,
                                    (view, year, month, day) -> {

                                        String startDate = year + "-" +
                                                String.format("%02d", month + 1) + "-" +
                                                String.format("%02d", day);

                                        DatePickerDialog endPicker = new DatePickerDialog(
                                                this,
                                                R.style.MyDialogTheme,
                                                (view2, year2, month2, day2) -> {

                                                    String endDate = year2 + "-" +
                                                            String.format("%02d", month2 + 1) + "-" +
                                                            String.format("%02d", day2);

                                                    selectedFilter = "RANGE";
                                                    rangeStartDate = startDate;
                                                    rangeEndDate = endDate;

                                                    tvFilter.setText(startDate + " → " + endDate);

                                                    loadExpenses();
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                        );

                                        endPicker.show();
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                            );
                            startPicker.setTitle("Select Start Date");
                            startPicker.show();
                        }else {
                            selectedFilter = options[which];
                            tvFilter.setText(selectedFilter + " ⌄");
                            loadExpenses();
                        }
                    })
                    .show();
        });

        // Get logged-in user
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", -1);

        loadExpenses();
    }

    private void showEndDatePicker(String startDate) {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog endPicker = new DatePickerDialog(
                this,
                R.style.MyDialogTheme,
                (view, year, month, day) -> {

                    String endDate = year + "-" +
                            String.format("%02d", month + 1) + "-" +
                            String.format("%02d", day);

                    selectedFilter = "RANGE";
                    rangeStartDate = startDate;
                    rangeEndDate = endDate;

                    tvFilter.setText("From " + startDate + " to " + endDate);

                    loadExpenses();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        endPicker.setTitle("Select End Date"); // 👈 THIS LINE
        endPicker.show();
    }

    private void showDetailDialog(int position) {

        Dialog dialog = new Dialog(this, R.style.MyDialogTheme);
        dialog.setContentView(R.layout.dialog_expense_detail);

        TextView tvAmount = dialog.findViewById(R.id.tvAmount);
        TextView tvCategory = dialog.findViewById(R.id.tvCategory);
        TextView tvDate = dialog.findViewById(R.id.tvDate);
        EditText etNote = dialog.findViewById(R.id.etNote);
        Button btnDelete = dialog.findViewById(R.id.btnDelete);

        tvAmount.setText("₹" + amountList.get(position));
        tvCategory.setText(categoryList.get(position));
        tvDate.setText(dateList.get(position));


        String note = noteList.get(position);
        etNote.setText(note != null && !note.isEmpty() ? note : "");

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        Button btnSave = dialog.findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {

            String updatedNote = etNote.getText().toString();

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            int id = expenseIdList.get(position);

            db.execSQL(
                    "UPDATE expenses SET note=? WHERE id=?",
                    new Object[]{updatedNote, id}
            );

            Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();

            loadExpenses(); // refresh
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            deleteExpense(position);
            dialog.dismiss();
        });
    }
    private void showDeleteDialog(int position) {

        new AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteExpense(position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteExpense(int position) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int id = expenseIdList.get(position);

        db.delete("expenses", "id=?", new String[]{String.valueOf(id)});

        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();

        loadExpenses(); // refresh
    }


    private void loadExpenses() {

        amountList.clear();
        categoryList.clear();
        dateList.clear();
        noteList.clear();
        expenseIdList.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query;
        String[] args;
        if (selectedFilter.equals("RANGE")) {

            query = "SELECT id, amount, category, date, note FROM expenses " +
                    "WHERE user_id=? AND date BETWEEN ? AND ? ORDER BY id DESC";

            args = new String[]{
                    String.valueOf(currentUserId),
                    rangeStartDate,
                    rangeEndDate
            };
        }
        else if (selectedFilter.equals("CUSTOM")) {

            query = "SELECT id, amount, category, date, note FROM expenses " +
                    "WHERE user_id=? AND date=? ORDER BY id DESC";

            args = new String[]{
                    String.valueOf(currentUserId),
                    selectedCustomDate
            };

        } else if (selectedFilter.equals("Today")) {

            query = "SELECT id, amount, category, date, note FROM expenses " +
                    "WHERE user_id=? AND date = date('now') ORDER BY id DESC";

            args = new String[]{String.valueOf(currentUserId)};

        } else if (selectedFilter.equals("This Month")) {

            query = "SELECT id, amount, category, date, note FROM expenses " +
                    "WHERE user_id=? AND strftime('%m', date) = strftime('%m', 'now') " +
                    "AND strftime('%Y', date) = strftime('%Y', 'now') ORDER BY id DESC";

            args = new String[]{String.valueOf(currentUserId)};

        } else {

            query = "SELECT id, amount, category, date, note FROM expenses " +
                    "WHERE user_id=? ORDER BY id DESC";

            args = new String[]{String.valueOf(currentUserId)};
        }

        Cursor cursor = db.rawQuery(query, args);

        cursor = db.rawQuery(query, new String[]{String.valueOf(currentUserId)});

        while (cursor.moveToNext()) {

            int id = cursor.getInt(0);
            double amount = cursor.getDouble(1);
            String category = cursor.getString(2);
            String date = cursor.getString(3);
            String note = cursor.getString(4);

            expenseIdList.add(id);

            amountList.add(String.valueOf(amount));
            categoryList.add(category);
            dateList.add(date);
            noteList.add(note);
        }

        cursor.close();

        ExpenseAdapter adapter = new ExpenseAdapter(
                this, amountList, categoryList, dateList
        );

        if (amountList.isEmpty()) {
            listView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }

        listView.setAdapter(adapter);
    }
}
