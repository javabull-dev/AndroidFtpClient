package com.ljpc.createfile.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ProgressBar;

public class MyProgressBar extends ProgressBar {

    public MyProgressBar(Context context) {
        super(context);
        initPaint();
    }

    public MyProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public MyProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    public MyProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaint();
    }

    public void setProgress(float progress){
        setProgress((int)progress);
    }

    @Override
    public void setProgress(int progress) {
        super.setProgress(progress);
        setBarColor(Color.BLUE);
    }

    @Override
    public void setProgress(int progress, boolean animate) {
        super.setProgress(progress, animate);
    }

    public void setProgress(String msg){
        setText(msg);
        setProgress(0);
        setBarColor(Color.BLUE);
    }

    //在进度条上显示的值
    private String barText;

    public void setText(String messge) {
        this.barText = messge;
    }

    private Paint mPaint;

    public void initPaint() {
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);// 设置抗锯齿
        this.mPaint.setColor(Color.BLACK);
    }

    //设置进度条的颜色
    //默认颜色为
    public void setBarColor(int color) {
        ClipDrawable d = new ClipDrawable(new ColorDrawable(color), Gravity.LEFT, ClipDrawable.HORIZONTAL);
        setProgressDrawable(d);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect rect = new Rect();
        //在初始化的阶段就会调用onDraw,所以防止barText空指针异常
        if (barText == null) {
            barText = "";
        }
        this.mPaint.getTextBounds(this.barText, 0, this.barText.length(), rect);
        int x = (getWidth() / 2) - rect.centerX();// 让现实的字体处于中心位置;;
        int y = (getHeight() / 2) - rect.centerY();// 让显示的字体处于中心位置;;
        canvas.drawText(this.barText, x, y, this.mPaint);
    }
}
