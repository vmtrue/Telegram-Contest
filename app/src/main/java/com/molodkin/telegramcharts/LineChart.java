package com.molodkin.telegramcharts;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

//TODO: rtl support
public final class LineChart extends View {

    private static final boolean LOG_IS_ENABLED = true;
    private static final String LOG_TAG = "LineChart";


    private ChartGraph [] graphs;

    private long [] xPoints = ChartData.X;

    public final Matrix chartMatrix = new Matrix();
    public final Matrix scrollMatrix = new Matrix();

    private int start = 0;
    private int end = xPoints.length;

    private int maxYValueTemp;
    private int maxYValue;

    private int rowNumber = 6;
    private int columnNumber = 5;

    private String [] yAxisTexts = new String[rowNumber];
    private String [] xAxisTexts = new String[columnNumber + 1];

    private int scrollHeight = Utils.dpToPx(this, 40);
    private final int scrollWindowMinWidth = Utils.dpToPx(this, 40);
    private int scrollWindowMinWidthInSteps;
    private int scrollTouchBorderPadding = Utils.dpToPx(this, 10);
    private int scrollBorderTopBottomWidth = Utils.dpToPx(this, 2);
    private int scrollBorderLeftRightWidth = Utils.dpToPx(this, 5);
    private int xAxisHeight = Utils.dpToPx(this, 33);
    private int xAxisWidth = Utils.dpToPx(this, 1);

    private float xAxisTextHeight;

    private int xAxisSideMargin = Utils.dpToPx(this, 20);

    private int graphLineWidth = Utils.dpToPx(this, 2);

    private float availableChartHeight;

    private int axesTextSize = Utils.spToPx(this, 14);
    private int xTextMargin = Utils.dpToPx(this, 4);

    private Paint axisPaint = new Paint();
    private TextPaint axisTextPaint = new TextPaint();

    private Paint scrollCoverPaint = new Paint();
    private Paint scrollBorderPaint = new Paint();


    private float rowHeight;
    private float stepX;

    private Rect xTextBounds = new Rect();

    private Date tempDate = new Date();
    private DateFormat xAxisDateFormat;

    private float xTextsStep;
    private float[] tempPoint = new float[2];

    private final InfoView infoView = new InfoView(getContext());
    private float stepY;

    public LineChart(Context context) {
        super(context);
        init();
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        xAxisDateFormat = new SimpleDateFormat("MMM d", Utils.getLocale(getContext()));

        initPaints();
        initGraphs();
    }

    private void initPaints() {
        axisPaint.setColor(Utils.getColor(getContext(), R.color.gray));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(xAxisWidth);

        axisTextPaint.setColor(Utils.getColor(getContext(), R.color.gray_text));
        axisTextPaint.setTextSize(axesTextSize);
        xAxisTextHeight = Utils.getFontHeight(axisTextPaint);

        scrollCoverPaint.setStyle(Paint.Style.FILL);
        scrollCoverPaint.setColor(Utils.getColor(getContext(), R.color.scroll_cover));

        scrollBorderPaint.setStyle(Paint.Style.FILL);
        scrollBorderPaint.setColor(Utils.getColor(getContext(), R.color.scroll_border));
    }

    private void initGraphs() {
        graphs = new ChartGraph[4];

        ChartGraph chartGraph0 = new ChartGraph(ChartData.Y0, Utils.getColor(getContext(), R.color.graph1), graphLineWidth);
        ChartGraph chartGraph1 = new ChartGraph(ChartData.Y1, Utils.getColor(getContext(), R.color.graph2), graphLineWidth);
        ChartGraph chartGraph2 = new ChartGraph(ChartData.Y2, Utils.getColor(getContext(), R.color.graph3), graphLineWidth);
        ChartGraph chartGraph3 = new ChartGraph(ChartData.Y3, Utils.getColor(getContext(), R.color.graph4), graphLineWidth);

        graphs[0] = chartGraph0;
        graphs[1] = chartGraph1;
        graphs[2] = chartGraph2;
        graphs[3] = chartGraph3;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        availableChartHeight = (float) getHeight() - scrollHeight - xAxisHeight;
        rowHeight = availableChartHeight / rowNumber;

        maxYValue = getMaxYValue();
        maxYValueTemp = maxYValue;

        updateYAxis();

        stepY = availableChartHeight / maxYValue;
        stepX = ((float) getWidth()) / (xPoints.length - 1);

        scrollWindowMinWidthInSteps = Math.round(scrollWindowMinWidth / stepX);

        updateXAxis();

        for (ChartGraph graph : graphs) {
            graph.path.reset();
            graph.path.moveTo(0, availableChartHeight - graph.values[0] * stepY);
        }

        for (int i = 1; i < end; i++) {
            for (ChartGraph graph : graphs) {
                graph.path.lineTo(i * stepX, availableChartHeight - graph.values[i] * stepY);
            }
        }

        scrollMatrix.postScale(1, scrollHeight / availableChartHeight, 0, 0);
    }

    private float prevScrollX = 0;
    private int prevScrollXPoint = 0;

    private boolean isScrollLeftBorderGrabbed = false;
    private boolean isScrollRightBorderGrabbed = false;
    private boolean isScrollWindowGrabbed = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        log("onTouchEvent ------------------------------------");

        float x = event.getX();
        float y = event.getY();

        if (y < availableChartHeight) {
            handleChartTouch(event);
        }

        boolean isScrollMoving = event.getAction() == MotionEvent.ACTION_MOVE &&
                (isScrollLeftBorderGrabbed || isScrollRightBorderGrabbed || isScrollWindowGrabbed);

        if (y > availableChartHeight + xAxisHeight || isScrollMoving) {
            handleScrollTouch(event);
        }

        if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
            isScrollLeftBorderGrabbed = false;
            isScrollRightBorderGrabbed = false;
            isScrollWindowGrabbed = false;
            infoView.cancelMoving();
        }

        return event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE;
    }


    private void handleChartTouch(MotionEvent event) {
        infoView.handleTouch(event);

        float x = event.getX();
        int chartLineXPoint = xIndexByCoord(x);
        infoView.measure(xPoints[chartLineXPoint], chartLineXPoint, graphs);
        invalidate();
    }

    private void handleScrollTouch(MotionEvent event) {
        float x = event.getX();
        float left = start * stepX;
        float right = (end - 1) * stepX;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                prevScrollX = x;
                if (Math.abs(left + scrollBorderLeftRightWidth / 2f - x) < scrollTouchBorderPadding) {
                    isScrollLeftBorderGrabbed = true;
                } else if (Math.abs(right - scrollBorderLeftRightWidth / 2f - x) < scrollTouchBorderPadding) {
                    isScrollRightBorderGrabbed = true;
                } else if (x > left && x < right) {
                    isScrollWindowGrabbed = true;
                    prevScrollXPoint = Math.round(x / stepX);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int newPoint = Math.round(x / stepX);

                float scrollDistance = x - prevScrollX;
                int scrollDistanceInSteps = newPoint - prevScrollXPoint;

                prevScrollX = x;
                prevScrollXPoint = newPoint;

                if (isScrollLeftBorderGrabbed) {
                    if (x < scrollBorderLeftRightWidth) {
                        setStart(0);
                    } else if (newPoint > end - scrollWindowMinWidthInSteps - 1) {
                        setStart(end - scrollWindowMinWidthInSteps - 1);
                    } else {
                        setStart(newPoint);
                    }
                } else if (isScrollRightBorderGrabbed) {
                    if (x + scrollBorderLeftRightWidth > getWidth()) {
                        setEnd(xPoints.length);
                    } else if (newPoint < start + scrollWindowMinWidthInSteps) {
                        setEnd(start + scrollWindowMinWidthInSteps);
                    } else {
                        setEnd(newPoint);
                    }
                } else if (isScrollWindowGrabbed) {
                    int range = end - start;
                    if (start + scrollDistanceInSteps <= 0) {
                        setStartEnd(0, range);
                    } else if (end + scrollDistanceInSteps >= xPoints.length) {
                        setStartEnd(xPoints.length - range, xPoints.length);
                    } else {
                        setStartEnd(start + scrollDistanceInSteps, end + scrollDistanceInSteps);
                    }
                }

                log("scrollDistance: " + scrollDistance);
                break;
            }
            default:
                adjustYAxis();
        }
        invalidate();
    }

    private int xIndexByCoord(float x) {
        tempPoint[0] = x;
        Matrix invert = Utils.invertMatrix(chartMatrix);

        invert.mapPoints(tempPoint);

        return Math.round(tempPoint[0] / stepX);
    }

    private float xCoordByIndex(float x) {
        tempPoint[0] = x;
        chartMatrix.mapPoints(tempPoint);

        return tempPoint[0];
    }

    private void updateYAxis() {
        int rowStep = (int) (Math.ceil(maxYValueTemp * 1f / rowNumber));

        for (int i = 0; i < rowNumber; i++) {
            yAxisTexts[i] = String.valueOf(rowStep * i);
        }
    }

    private void updateXAxis() {
        int startIndex = xIndexByCoord(xAxisSideMargin);
        int endIndex = xIndexByCoord(getWidth() - xAxisSideMargin);

        int range = endIndex - startIndex;
        float step = range * 1f / columnNumber;
        if (step >= 1) {
            for (int i = 0; i <= columnNumber; i++) {
                long millsec = xPoints[startIndex + Math.round(i * step)];
                tempDate.setTime(millsec);
                xAxisTexts[i] = xAxisDateFormat.format(tempDate);
            }
            xTextsStep = (getWidth() - (xAxisSideMargin * 2f)) / columnNumber;
        } else {
            for (int i = 0; i < range; i++) {
                long millsec = xPoints[startIndex +i];
                tempDate.setTime(millsec);
                xAxisTexts[i] = xAxisDateFormat.format(tempDate);
            }
            xAxisTexts[range] = null;
            xTextsStep = getWidth() * 1f / range;
        }

        axisTextPaint.getTextBounds(xAxisTexts[0], 0, xAxisTexts[0].length(), xTextBounds);

    }

    private int getMaxYValue() {
        ArrayList<Integer> maxValues = new ArrayList<>(graphs.length);
        for (ChartGraph graph : graphs) {
            maxValues.add(graph.getMax(start, end));
        }

        return Collections.max(maxValues);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        drawXAxes(canvas);

        drawXTexts(canvas);

        drawPoints(canvas);

        drawScroll(canvas);


        infoView.draw(canvas, availableChartHeight, chartMatrix, stepX, stepY);
    }

    private void drawXAxes(Canvas canvas) {
        canvas.save();

        canvas.translate(0f, availableChartHeight);

        for (int i = 0; i < rowNumber; i++) {
            canvas.drawLine(0f, 0f, getWidth(), 0f, axisPaint);

            canvas.save();

            canvas.translate(0f, -xTextMargin);

            canvas.drawText(yAxisTexts[i], 0, 0, axisTextPaint);

            canvas.restore();

            canvas.translate(0f, -rowHeight);
        }

        canvas.restore();
    }

    private void drawXTexts(Canvas canvas) {
        if (xAxisTexts[0] == null) return;

        canvas.save();

        canvas.translate(0, availableChartHeight + xTextMargin + xAxisTextHeight);

        for (String xAxisText : xAxisTexts) {
            if (xAxisText == null) break;
            canvas.drawText(xAxisText, 0, 0, axisTextPaint);
            canvas.translate(xTextsStep, 0);
        }

        canvas.restore();

    }

    private void drawPoints(Canvas canvas) {
        for (ChartGraph graph : graphs) {
            graph.draw(canvas, chartMatrix);
        }
    }

    private void drawScroll(Canvas canvas) {
        canvas.save();

        canvas.translate(0, availableChartHeight + xAxisHeight);

        for (ChartGraph graph : graphs) {
            graph.drawScroll(canvas, scrollMatrix);
        }


        float left = start * stepX;
        float right = (end - 1) * stepX;

        if (start != 0) {
            canvas.drawRect(0, 0, left, scrollHeight, scrollCoverPaint);
        }

        if (end != xPoints.length) {
            canvas.drawRect(right, 0, getWidth(), scrollHeight, scrollCoverPaint);
        }

        //draw left right borders
        canvas.drawRect(left, 0, left + scrollBorderLeftRightWidth, scrollHeight, scrollBorderPaint);
        canvas.drawRect(right - scrollBorderLeftRightWidth, 0, right, scrollHeight, scrollBorderPaint);

        //draw top bottom borders
        canvas.drawRect(left + scrollBorderLeftRightWidth, 0, right - scrollBorderLeftRightWidth, scrollBorderTopBottomWidth, scrollBorderPaint);
        canvas.drawRect(left + scrollBorderLeftRightWidth, scrollHeight - scrollBorderTopBottomWidth, right - scrollBorderLeftRightWidth, scrollHeight, scrollBorderPaint);

        canvas.restore();
    }

    private void drawInfoWindow(Canvas canvas) {
        infoView.draw(canvas, availableChartHeight, chartMatrix, stepX, stepY);
    }

    public void setStart(int start) {
        if (start >= end - 2) return;

        float toScale = xPoints.length / (end - start * 1f);
        startScaleAnimation(toScale, true);
        this.start = start;
        updateXAxis();
        invalidate();
    }

    public void adjustYAxis() {
        int newTempMaxYValue = getMaxYValue();

        float toScale = this.maxYValue * 1f / newTempMaxYValue;

        float fromScale = this.maxYValue * 1f / this.maxYValueTemp;

        this.maxYValueTemp = newTempMaxYValue;

        Log.d("LineChart_adjustYAxis", "fromScale: " + fromScale);
        Log.d("LineChart_adjustYAxis", "toScale: " + toScale);

        final float [] prev = new float[1];
        prev[0] = fromScale;

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(fromScale, toScale);
        valueAnimator.setDuration(250L);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                chartMatrix.postScale(1, value / prev[0], 0f, availableChartHeight);
                prev[0] = value;
                invalidate();

            }
        });
        valueAnimator.start();

        updateYAxis();
    }

    public void setEnd(int end) {
        if (end > xPoints.length) return;
        if (end <= start) return;

        float toScale = xPoints.length / (end - start * 1f);
        startScaleAnimation(toScale, false);

        this.end = end;
        updateXAxis();
        invalidate();
    }

    public void setStartEnd(int start, int end) {
        if (end > xPoints.length) return;
        if (start >= end - 1) return;

        final float [] prev = new float[1];
        prev[0] = 0f;

        float fromCoordStart = xCoordByIndex(this.start * stepX);
        float toCoordStart = xCoordByIndex(start * stepX);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, fromCoordStart - toCoordStart);
        valueAnimator.setDuration(250L);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                chartMatrix.postTranslate(value - prev[0], 0);
                prev[0] = value;
                invalidate();

            }
        });
        valueAnimator.start();

        this.start = start;
        this.end = end;
        updateXAxis();
        invalidate();
    }

    private void startScaleAnimation(float toScale, final boolean isStart) {
        float fromScale = xPoints.length / (end - start * 1f);

        Log.d("LineChart", "fromScale: " + fromScale);
        Log.d("LineChart", "toScale: " + toScale);

        final float [] prev = new float[1];
        prev[0] = fromScale;

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(fromScale, toScale);
        valueAnimator.setDuration(250L);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                chartMatrix.postScale(value / prev[0], 1, isStart ? getWidth() : 0, 0f);
                prev[0] = value;
                invalidate();

            }
        });
        valueAnimator.start();
    }

    private void log(String text) {
        if (LOG_IS_ENABLED) Log.d(LOG_TAG, text);
    }
}
