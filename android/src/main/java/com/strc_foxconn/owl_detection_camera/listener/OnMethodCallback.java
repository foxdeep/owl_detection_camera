package com.strc_foxconn.owl_detection_camera.listener;

public interface OnMethodCallback
{
    int SCREEN_BRIGHT = 0;
    int SCREEN_DARK = 1;

    void onStartDetectFace();
    void onStopDetectFace();
    void onResumeEvent();
    void onPauseEvent();
    void onScreenBright(int aValue);
    void onSetFaceDetectionHintText(String aFitCenter, String aHitForward, String aHintBackward);
}
