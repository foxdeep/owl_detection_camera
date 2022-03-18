package com.strc_foxconn.owl_detection_camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;

import androidx.annotation.NonNull;


import com.strc_foxconn.owl_detection_camera.enums.MethodChannels;
import com.strc_foxconn.owl_detection_camera.listener.OnMethodCallback;
import com.strc_foxconn.owl_detection_camera.utils.Utility;

import java.util.ArrayList;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.StandardMessageCodec;

/** FlutterOwlCameraPlugin */
public class OwlDetectionCameraPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener
{
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  public static MethodChannel mMethodChannel;

  public static ArrayList<Integer> mFaceFrameSize;

  public static OnMethodCallback mOnMethodCallback;

  public static MethodChannels mMethodChannels = MethodChannels.OwlCameraPlugin;

  public static Context mContext;
  public static Activity mActivity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding)
  {
    mMethodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), mMethodChannels.CHANNEL_ID);
    mMethodChannel.setMethodCallHandler(this);

    flutterPluginBinding.getPlatformViewRegistry().registerViewFactory(mMethodChannels.PLATFORM_VIEW_ID, new CameraFactory(StandardMessageCodec.INSTANCE));
    mContext = flutterPluginBinding.getApplicationContext();
  }

  public static void setOnMethodCallBack(OnMethodCallback aOnMethodCallback)
  {
    mOnMethodCallback = aOnMethodCallback;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result)
  {
    if (call.method.equals("getPlatformVersion"))
    {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_FACE_FRAME_SIZE))
    {
      if(call.arguments!=null)
      {
        if(call.arguments instanceof ArrayList)
        {
          ArrayList<ArrayList<Integer>> args = (ArrayList<ArrayList<Integer>>) call.arguments;
           mFaceFrameSize = args.get(0);
        }
      }
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_START_DETECTION_FACE))
    {
      if(mOnMethodCallback!=null)
        mOnMethodCallback.onStartDetectFace();
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_STOP_DETECTION_FACE))
    {
      if(mOnMethodCallback!=null)
        mOnMethodCallback.onStopDetectFace();
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_ON_RESUME_EVENT))
    {
      if(mOnMethodCallback!=null)
        mOnMethodCallback.onResumeEvent();
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_ON_PAUSE_EVENT))
    {
      if(mOnMethodCallback!=null)
        mOnMethodCallback.onPauseEvent();
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_SCREEN_BRIGHT))
    {
      if(call.arguments!=null)
      {
        if(call.arguments instanceof ArrayList)
        {
          ArrayList<Integer> args = (ArrayList<Integer>) call.arguments;
          int bright = args.get(0);

          if(mOnMethodCallback!=null)
            mOnMethodCallback.onScreenBright(bright);
        }
      }
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_FACE_DETECT_HINT))
    {
      if(call.arguments!=null)
      {
        if (call.arguments instanceof ArrayList)
        {
          ArrayList<ArrayList<String>> args = (ArrayList<ArrayList<String>>) call.arguments;
          String center = args.get(0).get(0);
          String forward = args.get(0).get(1);
          String backward = args.get(0).get(2);

          if(mOnMethodCallback!=null)
            mOnMethodCallback.onSetFaceDetectionHintText(center,forward,backward);
        }
      }
    }
    else if(call.method.equals(mMethodChannels.METHOD_ID_WRITHE_SETTING_PERMISSION))
    {
       boolean hasPermission = Utility.checkSystemWriteSettings(mActivity);
       if(hasPermission)
       {
         writeSettingPermission(hasPermission);
       }
    }
    else{
      result.notImplemented();
    }
  }

  private void writeSettingPermission(boolean aValue)
  {
    HashMap<String,Boolean> arguments = new HashMap<>();
    arguments.put("writeSettingPermission",aValue);
    OwlDetectionCameraPlugin.mMethodChannel.invokeMethod(OwlDetectionCameraPlugin.mMethodChannels.INVOKE_ID_WRITE_SETTING_PERMISSION,arguments);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding)
  {
    mMethodChannel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding)
  {
    mActivity = binding.getActivity();
    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges()
  {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding)
  {

  }

  @Override
  public void onDetachedFromActivity()
  {

  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if(requestCode == Defines.SYSTEM_WRITE_SETTING_REQUEST_CODE)
    {
      if(resultCode == Activity.RESULT_OK)
      {
        writeSettingPermission(true);
      }
      else
      {
        writeSettingPermission(false);
      }
    }
    return false;
  }
}
