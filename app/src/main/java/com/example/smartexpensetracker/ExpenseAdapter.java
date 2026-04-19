package com.example.smartexpensetracker;


import android.content.Context;
import android.view.*;
import android.widget.TextView;

import java.util.ArrayList;

public class ExpenseAdapter extends android.widget.BaseAdapter {

    Context context;
    ArrayList<String> amountList, categoryList, dateList;

    public ExpenseAdapter(Context context,
                          ArrayList<String> amountList,
                          ArrayList<String> categoryList,
                          ArrayList<String> dateList) {

        this.context = context;
        this.amountList = amountList;
        this.categoryList = categoryList;
        this.dateList = dateList;
    }

    @Override
    public int getCount() {
        return amountList.size();
    }

    @Override
    public Object getItem(int position) {
        return amountList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_expense, parent, false);
        }

        TextView tvAmount = convertView.findViewById(R.id.tvAmount);
        TextView tvCategory = convertView.findViewById(R.id.tvCategory);
        TextView tvDate = convertView.findViewById(R.id.tvDate);

        String dateFromDB = dateList.get(position);

        try {
            java.text.SimpleDateFormat input = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.text.SimpleDateFormat output = new java.text.SimpleDateFormat("dd MMM yyyy");

            String formatted = output.format(input.parse(dateFromDB));
            tvDate.setText(formatted);

        } catch (Exception e) {
            tvDate.setText(dateFromDB);
        }

        tvAmount.setText("₹" + amountList.get(position));
        tvCategory.setText(categoryList.get(position));
        String category = categoryList.get(position);

        if (category.equalsIgnoreCase("Essentials")) {
            tvCategory.setTextColor(context.getResources().getColor(R.color.essential_color));

        } else if (category.equalsIgnoreCase("Lifestyle")) {
            tvCategory.setTextColor(context.getResources().getColor(R.color.lifestyle_color));

        } else if (category.equalsIgnoreCase("Transport")) {
            tvCategory.setTextColor(context.getResources().getColor(R.color.transport_color));

        } else if (category.equalsIgnoreCase("Utilities")) {
            tvCategory.setTextColor(context.getResources().getColor(R.color.utilities_color));
        }

        return convertView;
    }
}