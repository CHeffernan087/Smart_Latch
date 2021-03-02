package com.example.smart_latch_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MySimpleArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;

    public MySimpleArrayAdapter(Context context, String[] values) {
        super(context, R.layout.doordisplaybox, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.doordisplaybox, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.doorIdTextView);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.doorImage);
        textView.setText(values[position]);
        imageView.setImageResource(R.drawable.ic_door);

        return rowView;
    }
}