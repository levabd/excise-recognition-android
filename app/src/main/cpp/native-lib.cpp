#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

using namespace std;
using namespace cv;

extern "C"
jstring
Java_com_wipon_recognition_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    cv::Rect();
    cv::Mat();
    std::string hello = "Hello from C++ and OpenCV";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
jstring
Java_com_wipon_recognition_MainActivity_validate(JNIEnv *env, jobject, jlong addrGray, jlong addrRgba) {
    cv::Rect();
    cv::Mat();
    std::string hello2 = "Hello from validate";
    return env->NewStringUTF(hello2.c_str());
}

//extern "C"
/*jstring
Java_com_wipon_recognition_MainActivity_salt(JNIEnv *env, jobject instance,
                                             jlong matAddrGray,
                                             jint nbrElem) {
    Mat &mGr = *(Mat *) matAddrGray;
    for (int k = 0; k < nbrElem; k++) {
        int i = rand() % mGr.cols;
        int j = rand() % mGr.rows;
        mGr.at<uchar>(j, i) = 255;
    }
}*/