package com.wipon.recognition;

import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.wipon.recognition.ui.camera.GraphicOverlay;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private ExciseStohasticVerifier numberVerifier;
    private SharedPreferences mPrefs;

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, ExciseStohasticVerifier verifier, SharedPreferences prefs) {
        mGraphicOverlay = ocrGraphicOverlay;
        numberVerifier = verifier;
        mPrefs = prefs;
    }

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean("LastDetectSuccessful", true);
        editor.apply();

        mGraphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        if (items.size() > 0){
            if (items.valueAt(0) != null){
                editor.putBoolean("WasRecognition", true);
                editor.apply();
            }
        }

        int numberCandidate = 0;
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                Log.d("Processor", "Text detected! " + item.getValue());
                if (numberVerifier.addNumber(item.getValue())) {
                    numberCandidate = i;
                }
            }
        }

        if (items.size() == 0) {
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, null, null);
            mGraphicOverlay.add(graphic);
        } else {
            TextBlock item = items.valueAt(numberCandidate);
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item, numberVerifier);
            mGraphicOverlay.add(graphic);
        }

        // if (numberVerifier.getCandidatesCount() > 9){
        //     answer = numberVerifier.calculatePossibleNumber(); <-- This is for you Max !!!
        // }
    }

    @Override
    public void release() {
        mGraphicOverlay.clear();
    }
}
