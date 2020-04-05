package com.example.rgb_test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skydoves.colorpickerview.AlphaTileView;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.flag.FlagView;

public class CustomFlag extends FlagView {
    private TextView textView;
    private LinearLayout linearLayout;

    @SuppressLint("WrongViewCast")
    public CustomFlag(Context context, int layout) {
        super(context, layout);
        textView = findViewById(R.id.flag_color_code);
        linearLayout = findViewById(R.id.flag_color_layout);
    }

    @Override
    public void onRefresh(ColorEnvelope colorEnvelope) {
        textView.setText("#" + colorEnvelope.getHexCode().substring(2));
        linearLayout.setBackgroundColor(colorEnvelope.getColor());
    }
}
