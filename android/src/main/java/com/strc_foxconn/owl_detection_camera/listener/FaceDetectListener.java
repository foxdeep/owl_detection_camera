package com.strc_foxconn.owl_detection_camera.listener;

import android.graphics.RectF;
import android.hardware.camera2.params.Face;

import java.util.List;

public interface FaceDetectListener
{
    void onFaceDetect(List<Face> faces, RectF facesRect);
}
