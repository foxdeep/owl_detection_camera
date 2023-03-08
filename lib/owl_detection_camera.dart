import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'owl_camera_defines.dart';

typedef OnImageCallback = void Function(String,String);
typedef OnFaceRectangleCallback = void Function(Rect);
typedef OnQRcodeCallback = void Function(String);
typedef OnWriteSettingPermissionCallback = void Function(bool);

class OwlDetectionCamera extends StatefulWidget
{
  static OnImageCallback? onImageCallbacks;
  static OnFaceRectangleCallback? onFaceRectangleCallback;
  static OnQRcodeCallback? onQRcodeCallback;
  static OnWriteSettingPermissionCallback? onOnWriteSettingPermissionCallback;

  static const MethodChannel _channel = MethodChannel('owl_detection_camera');

  static void passFaceFrameSize(int aFaceWidth,int aFaceHeight) async
  {
    List<int> faceFrameSize = [aFaceWidth,aFaceHeight];
    await _channel.invokeMethod(OwlCameraDefine.METHOD_CHANNEL_FACE_FRAME_SIZE, [faceFrameSize]);
  }

  static void stopDetection() async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_STOP_DETECTION, [""]);
  }

  static void startDetection() async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_START_DETECTION, [""]);
  }

  static void onResumeEvent() async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_ID_ON_RESUME_EVENT, [""]);
  }

  static void onPauseEvent() async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_ID_ON_PAUSE_EVENT, [""]);
  }

  static void screenBright(int aValue) async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_ID_SCREEN_BRIGHT,[aValue]);
  }

  static void faceDetectHint(String aFitCenter,String aForward,String  aBackward) async
  {
    List<String> hint = [aFitCenter,aForward,aBackward];
    await _channel.invokeMethod(OwlCameraDefine.METHOD_ID_FACE_DETECT_HINT,[hint]);
  }

  /// Setting detect face,qrcode and both.
  ///
  /// aValue: OwlCameraDefine.FACE_MODE
  ///         OwlCameraDefine.QRCODE_MODE
  ///         OwlCameraDefine.BLEND_MODE
  static void setDetectionMode(int aValue) async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_SET_DETECTION_MODE, [aValue]);
  }

  /// Disable face detection hint.
  static void disableHint(bool aValue) async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_DISABLE_HINT, [aValue]);
  }

  static void switchCamera() async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_SWITCH_CAMERA, [""]);
  }

  ///only Android
  static void writeSettingPermission() async
  {
    await _channel.invokeMethod(OwlCameraDefine.METHOD_ID_WRITHE_SETTING_PERMISSION,[""]);
  }

  OwlDetectionCamera({Key? key}) : super(key: key);

  @override
  _OwlCameraView createState() => _OwlCameraView();
}

class _OwlCameraView extends State<OwlDetectionCamera>
{
  @override
  void initState()
  {
    super.initState();

    OwlDetectionCamera._channel.setMethodCallHandler((MethodCall call) async
    {
      if(call.method == OwlCameraDefine.INVOKE_ID_IMAGE_PATH_NAME)
      {
        String imageName = call.arguments["imageName"];

        String imagePath = call.arguments["imagePath"];

        OwlDetectionCamera.onImageCallbacks!(imageName,imagePath);
      }
      else if(call.method == OwlCameraDefine.INVOKE_ID_FACE_RECTANGLE)
      {
        var rectangle = call.arguments["face_rectangle"];

        Rect faceRect = Rect.fromLTRB(rectangle[0], rectangle[1], rectangle[2], rectangle[3]);
        OwlDetectionCamera.onFaceRectangleCallback!(faceRect);
      }
      else if(call.method == OwlCameraDefine.INVOKE_ID_QRCODE_TEXT)
      {
        String qrcodeResult = call.arguments["qrcode_result"];
        OwlDetectionCamera.onQRcodeCallback!(qrcodeResult);
      }
      else if(call.method == OwlCameraDefine.INVOKE_ID_WRITE_SETTING_PERMISSION)
      {
        bool qrcodeResult = call.arguments["writeSettingPermission"];
        OwlDetectionCamera.onOnWriteSettingPermissionCallback!(qrcodeResult);
      }
    });
  }

  @override
  Widget build(BuildContext context)
  {
    return
      Stack(
        children: [
          Theme.of(context).platform == TargetPlatform.android
          ? const AndroidView(
            viewType: 'com.foxconn.strc.owl/cameraview',
            creationParams: {
                'text': "Android"
            },
            creationParamsCodec: StandardMessageCodec(),
            )
          : const UiKitView(
            viewType: 'com.foxconn.strc.owl/cameraview',
            creationParams: {
                  'text': "IOS"
            },
          creationParamsCodec: StandardMessageCodec(),
          ),
        ],
      );
  }
}
