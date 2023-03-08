package com.strc_foxconn.owl_detection_camera.enums;

public enum MethodChannels
{
    OwlCameraPlugin;

    public final String CHANNEL_ID = "owl_detection_camera";
    public final String PLATFORM_VIEW_ID = "com.foxconn.strc.owl/cameraview";

    public final String METHOD_ID_FACE_FRAME_SIZE ="face_frame_size";
    public final String METHOD_ID_START_DETECTION ="start_detection";
    public final String METHOD_ID_STOP_DETECTION ="stop_detection";
    public final String METHOD_ID_ON_RESUME_EVENT ="on_resume_event";
    public final String METHOD_ID_ON_PAUSE_EVENT ="on_pause_event";
    public final String METHOD_ID_SCREEN_BRIGHT ="screen_bright";
    public final String METHOD_ID_FACE_DETECT_HINT ="face_detect_hint";
    public final String METHOD_ID_WRITHE_SETTING_PERMISSION ="write_setting_permission";
    public final String METHOD_SET_DETECTION_MODE="set_detection_mode";
    public final String METHOD_DISABLE_HINT="disable_hint";
    public final String METHOD_SWITCH_CAMERA="switch_camera";

    //invoke meant from native to flutter.
    public final String INVOKE_ID_IMAGE_PATH_NAME = "image_path_name_callback";
    public final String INVOKE_ID_FACE_RECTANGLE = "face_rectangle_callback";
    public final String INVOKE_ID_QRCODE_TEXT = "qrcode_text_callback";
    public final String INVOKE_ID_WRITE_SETTING_PERMISSION = "write_setting_permission_callback";
}
