package com.strc_foxconn.owl_detection_camera.listener;

public interface OnMethodCallback
{
    int SCREEN_BRIGHT = 0;
    int SCREEN_DARK = 1;

    int FACE_MODE = 0;
    int QRCODE_MODE= 1;
    int BLEND_MODE= 2;

    void onStartDetectFace();
    void onStopDetectFace();
    void onResumeEvent();
    void onPauseEvent();
    void onScreenBright(int aValue);
    void onSetFaceDetectionHintText(String aFitCenter, String aHitForward, String aHintBackward);
    void onSetDetectionMode(int aValue);
    void onDisableHint(boolean aValue);

}
