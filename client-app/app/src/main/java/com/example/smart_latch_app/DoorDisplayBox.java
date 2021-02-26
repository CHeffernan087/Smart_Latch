package com.example.smart_latch_app;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.Image;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StyleableRes;

public class DoorDisplayBox extends LinearLayout {

    @StyleableRes
    int index0 = 0;
    @StyleableRes
    int index1 = 1;
    @StyleableRes
    int index2 = 2;

    TextView doorID;
    TextView doorDesc;
    Button controlDoorButton;

    public DoorDisplayBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.doordisplaybox, this);

        int[] sets = {R.attr.doorId, R.attr.doorDesc, R.attr.controlDoorButton, R.attr.doorImage};
        TypedArray typedArray = context.obtainStyledAttributes(attrs, sets);
        CharSequence id = typedArray.getText(index0);
        CharSequence description = typedArray.getText(index1);
        CharSequence controlButton = typedArray.getText(index2);

        typedArray.recycle();

        initComponents();

        setDoorID(id);
        setDoorDesc(description);

    }

    private void initComponents() {
        doorID = (TextView) findViewById(R.id.doorIdTextView);
        doorDesc = (TextView) findViewById(R.id.doorDescView);
    }

    public CharSequence getDoorID() {
        return doorID.getText();
    }

    public void setDoorID(CharSequence value) {
        doorID.setText(value);
    }

    public CharSequence getDoorDescText() {
        return doorDesc.getText();
    }

    public void setDoorDesc(CharSequence value) {
        doorDesc.setText(value);
    }

    public CharSequence getButton() {
        return controlDoorButton.getText();
    }

    public void setButton(CharSequence value) {
        controlDoorButton.setText(value);
    }
}