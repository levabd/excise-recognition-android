package com.wipon.recognition;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;

/**
 * Created by Oleg Levitsky on 19.03.2018.
 */

public class MainActivity_test extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        if (!OpenCVLoader.initDebug()) {
            tv.setText(tv.getText() + "\n OpenCVLoader.initDebug() not working.");
        } else {
            tv.setText(tv.getText() + "\n OpenCVLoader.initDebug() WORKING.");
            tv.setText(tv.getText() + "\n" + validate(0L, 0L));
        }
    }

    public native String stringFromJNI();

    public native String validate(long matAddrGr, long matAddrRgba);
}
