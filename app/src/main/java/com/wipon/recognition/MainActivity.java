package com.wipon.recognition;

import java.util.ArrayList;

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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Point;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
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

    int roiX = 300; //300;
    int roiY = 310; //310;
    int roiWidth = 330; //330;
    int roiHeight = 100; //100;

    // DraW white rectangle around ROI
    // White color `new Scalar(220, 220, 220, 255)`
    public void drawRect(Mat inputFrame, Scalar color){
        Imgproc.rectangle(inputFrame, new Point(roiX-3, roiY-3), new Point(roiX+roiWidth+6,roiY+roiHeight+6), color, 3);
    }

    // Crop preassigned ROI for recognition into new matrix
    public Mat cropNumber(int newROIX, int newROIY, int newROIWidth, int newROIHeight, Mat baseROI){
        Rect numberROI = new Rect(newROIX, newROIY, newROIWidth, newROIHeight);
        return new Mat(baseROI, numberROI);
    }

    // Crop ROI for copyTo method using
    public Mat submatROI(Mat inputSrc){
        Rect baseROI = new Rect(roiX, roiY, roiWidth, roiHeight);
        return inputSrc.submat(baseROI);
    }

    // Crop ROI for to new matrix
    public Mat cropROI(Mat inputSrc){
        Rect baseROI = new Rect(roiX, roiY, roiWidth, roiHeight);
        return new Mat(inputSrc, baseROI);
    }

    public Mat baseROIThreshold(Mat grayInput){

        Mat clh = new Mat();
        Mat blurred = new Mat();
        Mat truncated = new Mat();
        Mat thresholded = new Mat();

        // Adoptive Histohram equalization
        CLAHE clahe = Imgproc.createCLAHE(3.5, new Size(8, 8));
        clahe.apply(grayInput, clh);

        // Truncate too light objects from ROI (Numbers might be black)
        Imgproc.threshold(clh, truncated, 100, 255, Imgproc.THRESH_TRUNC);
        // Gentle noise removing (best filter for text images)
        Imgproc.bilateralFilter(truncated, blurred, 6, 25, 25);
        // Adoptive binarization
        Imgproc.adaptiveThreshold(blurred, thresholded, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 21, 5);

        // Clean up
        truncated.release();
        blurred.release();
        clh.release();

        return thresholded;
    }

    public Rect segmentNumber(Mat thresholdedInput){

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(thresholdedInput, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        // Create new black image same as ROI size for Mask
        Mat goodMask = new Mat(roiHeight, roiWidth, CvType.CV_8UC1, new Scalar(0, 0, 0));
        ArrayList<Rect> goodMaskCandidates = new ArrayList<>();
        ArrayList<MatOfPoint> goodContours = new ArrayList<>();

        // Iterate over all contours
        for ( int contourIdx=0; contourIdx < contours.size(); contourIdx++ ) {
            MatOfPoint currentContour = contours.get(contourIdx);
            Rect r = Imgproc.boundingRect(currentContour);
            if ((r.width < 100) && (r.height > 10) && (r.width > 5) && (r.height < 90)){
                goodMaskCandidates.add(r);
                goodContours.add(currentContour);
            }
        }

        // Draw good contours white color over prepared black mask
        Imgproc.drawContours(goodMask, goodContours, -1, new Scalar(255, 255, 255), -1);

        // Calculate vertical projection and density maximum for goodMask
        Mat verp = projectionVer(goodMask);
        double peak = Core.minMaxLoc(verp).maxVal;

        // Iterate over vertical projection and crop it by density
        int topLine = 0;
        int bottomLine = 0;
        for (int i = 0; i < verp.height(); i++){
            double[] pixel;
            pixel = verp.get(i, 0);
            if (pixel[0] > peak*0.5){
                if (topLine == 0){
                    topLine = i-1;
                }
                if (bottomLine < i+1){
                    bottomLine = i+1;
                }
            }
        }

        int leftLine = 0;
        int rightLine = 0;
        for (Rect r: goodMaskCandidates)
        {
            if (((r.height + r.y > topLine) || (r.y < bottomLine)) &&
                    (topLine - r.y < 20) && (r.y + r.height - bottomLine < 20) &&
                    (r.width * r.height > 200))
            {
                if ((leftLine == 0) || (r.x < leftLine)){
                    leftLine = r.x;
                }
                if (rightLine < r.x + r.width){
                    rightLine = r.x + r.width;
                }
            }
        }

        topLine = (topLine > 4) ? topLine-4 : 0;
        bottomLine = (bottomLine < 92) ? bottomLine+8 : 100;
        leftLine = (leftLine > 16) ? leftLine-16 : 0;
        rightLine = (rightLine < 322) ? rightLine+8 : 330;

        // Clean up
        verp.release();
        goodMask.release();
        hierarchy.release();

        return new Rect(leftLine, topLine, rightLine - leftLine, bottomLine - topLine);
    }

    // Vertical one-dimensional projection of binary image (1D vertical histohram)
    // Much faster than foreach loop over Mat
    public Mat projectionVer(Mat binaryInput){
        Mat rowProjection = new Mat();
        Core.reduce(binaryInput, rowProjection, 1, Core.REDUCE_AVG, CvType.CV_8UC1);
        return rowProjection;
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat matGray = inputFrame.gray();
        // Mat mROIGray = cropROI(matGray); For production
        Mat mROIGray = submatROI(matGray);

        Mat thresholded = baseROIThreshold(mROIGray);
        /*Mat goodMask = segmentNumber(thresholded);
        Mat mask = goodMask.clone();
        goodMask.copyTo(mROIGray, mask);*/
        Rect newROI = segmentNumber(thresholded);
        thresholded.release();

        // Draw base rect for numbers
        drawRect(matGray, new Scalar(170, 170, 170, 255));
        // If detected ROI not very dope ;)
        if ((newROI.width > 0.4*roiWidth) && (newROI.width < roiWidth) &&
                (newROI.width / newROI.height > 7) && (newROI.width / newROI.height < 13))
        {
            Imgproc.rectangle(
                    matGray,
                    new Point(newROI.x + roiX, newROI.y + roiY),
                    new Point(newROI.x+newROI.width + roiX,newROI.y+newROI.height + roiY),
                    new Scalar(255, 255, 255, 255),
                    2);
        }

        frameResult = matGray.clone();

        // Clean up after yourself
        // mask.release();
        // goodMask.release();
        mROIGray.release();
        matGray.release();
        Log.d(TAG, "Recognized:");

        return frameResult;

    }

    //public native void salt(long matAddrGray, int nbrElem);
}
