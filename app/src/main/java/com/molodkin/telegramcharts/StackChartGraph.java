package com.molodkin.telegramcharts;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

class StackChartGraph extends BaseChartGraph {

    @Override
    int getMax(int start, int end) {
        return 0;
    }

    @Override
    int getMin(int start, int end) {
        return 0;
    }

    StackChartGraph(int[] values, int color, float width, String name) {
        super(values, name, color);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(color);

        scrollPaint = new Paint(paint);
        scrollPaint.setStrokeWidth(width / 2);

        points = new float[values.length * 4];
        tempPoints = new float[values.length * 4];
    }

    @Override
    void draw(Canvas canvas, Matrix matrix, int start, int end) {
        drawLines(canvas, matrix, paint, start, end + 1);
    }

    @Override
    void drawScroll(Canvas canvas, Matrix matrix) {
        drawLines(canvas, matrix, scrollPaint, 0, values.length);
    }
}
