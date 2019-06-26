package com.example.xch.scanzbar.zbar.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

/**
 * 描述信息
 *
 * @author chengwei11
 * @date 2019/6/26
 */
public class TextConfigNumberPicker extends NumberPicker {

    public TextConfigNumberPicker(Context context) {
        super(context);
    }

    public TextConfigNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextConfigNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        updateView(child);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
        updateView(child);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        updateView(child);
    }

    private void updateView(View view) {
        if (view instanceof EditText) {
            ((EditText) view).setTextSize(40);
        }
    }
}
