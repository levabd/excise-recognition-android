package com.wipon.recognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;


public class ExciseTextRecognizer extends Detector<TextBlock> {
    private TextRecognizer mDelegate;

    ExciseTextRecognizer(TextRecognizer delegate) {
        mDelegate = delegate;
    }

    public SparseArray<TextBlock> detect(Frame frame) {

        // Frame processing
        int width = frame.getMetadata().getWidth();
        int height = frame.getMetadata().getHeight();

        // Bitmap coloredImage4Backend = frame.getBitmap(); <-- This is for you Max !!!

        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(300, 310, 300+330, 310+100), 100, byteArrayOutputStream);
        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

        Frame croppedFrame =
                new Frame.Builder()
                        .setBitmap(bitmap)
                        .setRotation(Surface.ROTATION_180) // Faked rotation 180 degree for correct excise recognition
                        // .setRotation(frame.getMetadata().getRotation()) That was right rotation !!!
                        .build();

        return mDelegate.detect(croppedFrame);
        //return mDelegate.detect(frame);
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}