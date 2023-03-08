
class OwlCameraDefine
{
  static const int SCREEN_BRIGHT = 0;
  static const int SCREEN_DARK = 1;

  static const int FACE_MODE = 0;
  static const int QRCODE_MODE= 1;
  static const int BLEND_MODE= 2;

  static const String METHOD_CHANNEL_FACE_FRAME_SIZE="face_frame_size";
  static const String METHOD_CHANNEL_FACE_FRAME_HEIGHT="face_frame_height";

  static const String METHOD_STOP_DETECTION="stop_detection";
  static const String METHOD_START_DETECTION= "start_detection";

  static const String METHOD_ID_ON_RESUME_EVENT ="on_resume_event";
  static const String METHOD_ID_ON_PAUSE_EVENT ="on_pause_event";

  static const String METHOD_ID_SCREEN_BRIGHT ="screen_bright";
  static const String METHOD_ID_FACE_DETECT_HINT ="face_detect_hint";

  static const String METHOD_SET_DETECTION_MODE="set_detection_mode";

  static const String METHOD_DISABLE_HINT="disable_hint";

  static const String METHOD_SWITCH_CAMERA="switch_camera";

  static const String METHOD_ID_WRITHE_SETTING_PERMISSION ="write_setting_permission";

  //invoke meant from native to flutter.
  static const String INVOKE_ID_IMAGE_PATH_NAME = "image_path_name_callback";
  static const String INVOKE_ID_FACE_RECTANGLE = "face_rectangle_callback";
  static const String INVOKE_ID_QRCODE_TEXT = "qrcode_text_callback";
  static const String INVOKE_ID_WRITE_SETTING_PERMISSION = "write_setting_permission_callback";
}