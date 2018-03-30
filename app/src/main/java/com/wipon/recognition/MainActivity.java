package com.wipon.recognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Always load OpenCV globally
    static {
        System.loadLibrary("opencv_java3");
    }

    private static final String TAG = "OCR::Activity";
    private CameraBridgeViewBase _cameraBridgeViewBase;

    Mat frameResult;

    private BaseLoaderCallback _baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load ndk built module, as specified in moduleName in build.gradle
                    // after opencv initialization
                    System.loadLibrary("native-lib");
                    _cameraBridgeViewBase.setMinimumHeight(720);
                    _cameraBridgeViewBase.setMinimumWidth(1280);
                    _cameraBridgeViewBase.setMaxFrameSize(1280, 720);
                    _cameraBridgeViewBase.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // Permissions for Android 6+
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                1);

        _cameraBridgeViewBase = findViewById(R.id.main_surface);
        _cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        _cameraBridgeViewBase.enableFpsMeter();
        _cameraBridgeViewBase.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, _baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            _baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length <= 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED)
                {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    public void disableCamera() {
        if (_cameraBridgeViewBase != null)
            _cameraBridgeViewBase.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        // mRgba = new Mat(height, width, CvType.CV_8UC3);
        // mByte = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        frameResult.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat matGray = inputFrame.gray();
        // Mat mROIGray = cropROI(matGray); For production
        Mat mROIGray = OCRHelper.submatROI(matGray);

        Mat thresholded = OCRHelper.baseROIThreshold(mROIGray);
        Rect newROI = OCRHelper.segmentNumber(thresholded);

        // If detected ROI not very dope ;)
        if ((newROI.width > 0.4*OCRHelper.roiWidth) && (newROI.width < OCRHelper.roiWidth) &&
                (newROI.width / newROI.height > 7) && (newROI.width / newROI.height < 13))
        {
            /*Imgproc.rectangle(
                    matGray,
                    new Point(newROI.x + OCRHelper.roiX, newROI.y + OCRHelper.roiY),
                    new Point(newROI.x+newROI.width + OCRHelper.roiX,newROI.y+newROI.height + OCRHelper.roiY),
                    new Scalar(255, 255, 255, 255),
                    2);*/

            Mat mROIGray2 = OCRHelper.submatNumber(newROI.x + OCRHelper.roiX, newROI.y + OCRHelper.roiY, newROI.width, newROI.height, matGray);
            Mat goodMask = OCRHelper.binarizeNumber(mROIGray2);
            Mat mask = goodMask.clone();
            goodMask.copyTo(mROIGray2, mask);

            // Draw base rect for numbers
            OCRHelper.drawRect(matGray, new Scalar(170, 170, 170, 255));
            frameResult = matGray.clone();

            mROIGray2.release();
            mask.release();
            goodMask.release();
        } else {

            // Draw base rect for numbers
            OCRHelper.drawRect(matGray, new Scalar(170, 170, 170, 255));
            frameResult = matGray.clone();
        }

        // frameResult = matGray.clone();

        // Clean up after yourself
        thresholded.release();
        mROIGray.release();
        matGray.release();
        Log.d(TAG, "Recognized:");

        return frameResult;

    }

    //public native void salt(long matAddrGray, int nbrElem);
}
