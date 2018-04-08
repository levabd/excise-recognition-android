package com.wipon.recognition;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

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
    private static Paint sAdditionalTextPaint;
    private static Paint sResultPaint;
    private final TextBlock mText;
    private ExciseStohasticVerifier numberVerifier;

    OcrGraphic(GraphicOverlay overlay, TextBlock text, ExciseStohasticVerifier verifier) {
        super(overlay);

        mText = text;
        numberVerifier = verifier;

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

        if (sAdditionalTextPaint == null) {
            sAdditionalTextPaint = new Paint();
            sAdditionalTextPaint.setColor(TEXT_COLOR);
            sAdditionalTextPaint.setTextSize(54.0f);
        }

        if (sTextPaint == null) {
            sTextPaint = new Paint();
            sTextPaint.setColor(Color.YELLOW);
            sTextPaint.setTextSize(54.0f);
        }

        if (sResultPaint == null) {
            sResultPaint = new Paint();
            sResultPaint.setColor(Color.RED);
            sResultPaint.setTextSize(54.0f);
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

    /**
     * Checks whether a point is within the bounding box of this graphic.
     * The provided point should be relative to this graphic's containing overlay.
     * @param x An x parameter in the relative context of the canvas.
     * @param y A y parameter in the relative context of the canvas.
     * @return True if the provided point is contained within this graphic's bounding box.
     */
    @SuppressWarnings("unused")
    public boolean contains(float x, float y) {
        // This unused method required in interface. Check if this graphic's text contains this point.
        return false;
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        RectF baseRect = new RectF(310, 300, 310+100, 300+330);

        baseRect.left = translateX(baseRect.left);
        baseRect.top = translateY(baseRect.top);
        baseRect.right = translateX(baseRect.right);
        baseRect.bottom = translateY(baseRect.bottom);

        canvas.drawRect(baseRect, sMainRectPaint);
        canvas.drawText("Frame height: " + canvas.getHeight() + "; Frame width: " + canvas.getWidth(), translateX(10), translateY(160), sAdditionalTextPaint);

        if (mText == null) {
            return;
        }

        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(mText.getBoundingBox());

        rect.left = translateX(rect.top + 310);
        rect.top = translateY(330 - rect.right + 300);
        rect.right = translateX(rect.bottom + 310);
        rect.bottom = translateY(330 - rect.left + 300);
        // canvas.drawRect(rect, sRectPaint);

        canvas.drawText("Number left: " + rect.left + "; Number top: " + rect.top, translateX(10), translateY(230), sAdditionalTextPaint);

        canvas.drawText("Number: " + mText.getValue(), translateX(10), translateY(300), sTextPaint);

        String answer = numberVerifier.lastPossibleNumber;

        if (numberVerifier.getCandidatesCount() > 9){
            answer = numberVerifier.calculatePossibleNumber();
        }

        Log.d("Processor", "Answer calculated! " + answer);

        // Draw Possible answer
        canvas.drawText("Answer: " + answer, translateX(10), translateY(370), sResultPaint);
    }
}
