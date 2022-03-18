package com.strc_foxconn.owl_detection_camera.listener;

import android.graphics.Bitmap;
import com.google.zxing.Result;

public interface CameraCaptures
{
    void onCaptureCallback(Bitmap aBitmap, int aAction, Result aQRCodeResult, int aScalaFactor);
}
