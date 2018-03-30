package com.wipon.recognition;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by Oleg Levitsky on 31.03.2018.
 */
public class OCRHelper {

    public static int roiX = 300; //300;
    public static int roiY = 310; //310;
    public static int roiWidth = 330; //330;
    @SuppressWarnings("WeakerAccess")
    public static int roiHeight = 100; //100;

    // DraW white rectangle around ROI
    // White color `new Scalar(220, 220, 220, 255)`
    public static  void drawRect(Mat inputFrame, Scalar color){
        Imgproc.rectangle(inputFrame, new Point(roiX-3, roiY-3), new Point(roiX+roiWidth+6,roiY+roiHeight+6), color, 3);
    }

    // Crop preassigned ROI for recognition into new matrix
    public static  Mat submatNumber(int newROIX, int newROIY, int newROIWidth, int newROIHeight, Mat baseROI){
        Rect numberROI = new Rect(newROIX, newROIY, newROIWidth, newROIHeight);
        return baseROI.submat(numberROI);
    }

    // Crop preassigned ROI for recognition into new matrix
    @SuppressWarnings("unused")
    public static  Mat cropNumber(int newROIX, int newROIY, int newROIWidth, int newROIHeight, Mat baseROI){
        Rect numberROI = new Rect(newROIX, newROIY, newROIWidth, newROIHeight);
        return new Mat(baseROI, numberROI);
    }

    // Crop ROI for copyTo method using
    public static  Mat submatROI(Mat inputSrc){
        Rect baseROI = new Rect(roiX, roiY, roiWidth, roiHeight);
        return inputSrc.submat(baseROI);
    }

    // Crop ROI for to new matrix
    @SuppressWarnings("unused")
    public static  Mat cropROI(Mat inputSrc){
        Rect baseROI = new Rect(roiX, roiY, roiWidth, roiHeight);
        return new Mat(inputSrc, baseROI);
    }

    public static  Mat baseROIThreshold(Mat grayInput){

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

    public static  Rect segmentNumber(Mat thresholdedInput){

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

    public static Mat binarizeNumber(Mat grayInput){
        Mat clh = new Mat();
        Mat blurred = new Mat();
        Mat truncated = new Mat();
        Mat thresholded = new Mat();
        Mat maskedThreshold = new Mat();
        Mat threshold2 = new Mat();

        // Adoptive Histohram equalization
        CLAHE clahe = Imgproc.createCLAHE(3.5, new Size(8, 8));
        clahe.apply(grayInput, clh);

        // Gauss noise removing
        Imgproc.GaussianBlur(clh, blurred, new Size(5, 5), 0);
        // Truncate too light objects from ROI (Numbers might be black)
        Imgproc.threshold(blurred, truncated, 150, 255, Imgproc.THRESH_TRUNC);
        // Adoptive binarization
        Imgproc.adaptiveThreshold(truncated, thresholded, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 101, 29);

        // Add mask to source image
        Core.add(grayInput, thresholded, maskedThreshold);
        double hole = Core.minMaxLoc(maskedThreshold).minVal;
        Imgproc.threshold(maskedThreshold, threshold2, hole + 70, 255, Imgproc.THRESH_BINARY);

        // Clean up
        maskedThreshold.release();
        truncated.release();
        blurred.release();
        clh.release();

        return threshold2;
    }

    // Vertical one-dimensional projection of binary image (1D vertical histohram)
    // Much faster than foreach loop over Mat
    private static Mat projectionVer(Mat binaryInput){
        Mat rowProjection = new Mat();
        Core.reduce(binaryInput, rowProjection, 1, Core.REDUCE_AVG, CvType.CV_8UC1);
        return rowProjection;
    }
}
