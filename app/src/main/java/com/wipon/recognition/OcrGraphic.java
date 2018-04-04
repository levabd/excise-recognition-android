package com.wipon.recognition;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.wipon.recognition.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.text.TextBlock;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class OcrGraphic extends GraphicOverlay.Graphic {

    private int mId;

    private static final int TEXT_COLOR = Color.WHITE;

    private static Paint sRectPaint;
    private static Paint sMainRectPaint;
    private static Paint sTextPaint;
    private final TextBlock mText;

    OcrGraphic(GraphicOverlay overlay, TextBlock text) {
        super(overlay);

        mText = text;

        if (sRectPaint == null) {
            sRectPaint = new Paint();
            sRectPaint.setColor(Color.RED);
            sRectPaint.setStyle(Paint.Style.STROKE);
            sRectPaint.setStrokeWidth(4.0f);
        }

        if (sMainRectPaint == null) {
            sMainRectPaint = new Paint();
            sMainRectPaint.setColor(TEXT_COLOR);
            sMainRectPaint.setStyle(Paint.Style.STROKE);
            sMainRectPaint.setStrokeWidth(4.0f);
        }

        if (sTextPaint == null) {
            sTextPaint = new Paint();
            sTextPaint.setColor(TEXT_COLOR);
            sTextPaint.setTextSize(54.0f);
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public TextBlock getTextBlock() {
        return mText;
    }

    /**
     * Checks whether a point is within the bounding box of this graphic.
     * The provided point should be relative to this graphic's containing overlay.
     * @param x An x parameter in the relative context of the canvas.
     * @param y A y parameter in the relative context of the canvas.
     * @return True if the provided point is contained within this graphic's bounding box.
     */
    public boolean contains(float x, float y) {
        // TODO: Check if this graphic's text contains this point.
        return false;
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        RectF baseRect = new RectF(300, 310, 300+330, 310+100);
        baseRect.left = translateX(baseRect.left);
        baseRect.top = translateY(baseRect.top);
        baseRect.right = translateX(baseRect.right);
        baseRect.bottom = translateY(baseRect.bottom);
        canvas.drawRect(baseRect, sMainRectPaint);

        // TODO: Draw the text onto the canvas.
        if (mText == null) {
            return;
        }

        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(mText.getBoundingBox());
        rect.left = translateX(rect.left + 300);
        rect.top = translateY(rect.top + 310);
        rect.right = translateX(rect.right + 300);
        rect.bottom = translateY(rect.bottom + 310);
        canvas.drawRect(rect, sRectPaint);

        // Render the text at the bottom of the box.
        canvas.drawText(mText.getValue(), rect.left, rect.bottom - 130, sTextPaint);
    }
}
