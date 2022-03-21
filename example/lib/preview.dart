import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:owl_detection_camera/owl_camera_defines.dart';
import 'package:owl_detection_camera/owl_detection_camera.dart';
import 'package:permission_handler/permission_handler.dart';

class Preview extends StatefulWidget
{
  @override
  _PreviewState createState() => _PreviewState();
}

class _PreviewState  extends State<Preview>
{
  bool mIsDetectSuccess = false;
  bool mIsQRcode = false;

  bool mIsBrightness = true;

  String mImagePath = "";
  String mQRcodeResult = "";

  PermissionData mPermissionData = PermissionData(PermissionsState.checking);
  List<Permission> mPermission = [];

  @override
  void initState()
  {
    super.initState();

    OwlDetectionCamera.onImageCallbacks = (name,path)
    {
      if(!mIsDetectSuccess)
      {
        mImagePath = path;
        mIsDetectSuccess = true;
        setState(() {});
      }
    };

    ///IOS maybe need times devicePixelRatio for real screen piexl.
    OwlDetectionCamera.onFaceRectangleCallback = (Rect aValue)
    {
      print("Face Rectangle :$aValue");
    };

    OwlDetectionCamera.onQRcodeCallback = (String aValue)
    {
      if(!mIsQRcode)
      {
        mQRcodeResult = aValue;
        mIsQRcode = true;
        setState(() {});
      }
    };

    OwlDetectionCamera.onOnWriteSettingPermissionCallback = (bool aValue)
    {
      print("WriteSettingPermissionCallback has:${aValue}");
    };

    OwlDetectionCamera?.writeSettingPermission();
  }

  void checkPermission(List<Permission> aPermissions) async
  {
    if(mPermission.isEmpty)
    {
      mPermission = aPermissions;
    }

    Permission temp = mPermission.removeLast();

    if(await temp.request().isGranted)
    {
      if(!mPermission.isEmpty)
      {
        checkPermission(mPermission);
      }
      else{
        mPermissionData.mPermissionsState = PermissionsState.grainted;
        setState(() {
        });
      }
    }else
    {
      mPermissionData.mPermissionsState = PermissionsState.deny;
      mPermissionData.mPermissionName = temp.toString();
      setState(() {
      });
    }
  }

  Widget check(List<Permission> aPermission)
  {
    checkPermission(aPermission);
    return const Center(
      child:
      Text("Loading...")
      ,);
  }

  Widget pleaseGivePermission()
  {
    return const Center(
      child: Text("Please give permissions."),
    );
  }

  Widget successImage(String aFilePath)
  {
    updateView();
    return Image.file(File(aFilePath),
      scale: 4,
      fit: BoxFit.fill,
    );
  }

  Widget centerText(String aText)
  {
    updateView();
    return Center(
      child: Text(aText),
    );
  }

  void updateView() async
  {
    await Future.delayed(Duration(milliseconds: 2000), ()
    {
      setState(() {
        mIsDetectSuccess = false;
        mIsQRcode= false;
        OwlDetectionCamera.startFaceDetection();
      });
    });
  }

  Widget preview(var size,var ratio)
  {
    var faceFrameWidth = size.width/1.5;
    var faceFrameHeight = faceFrameWidth * 1.25;
    var pendingWidth = size.width - faceFrameWidth;
    var pendingHeight = size.height - faceFrameHeight;

    if(Theme.of(context).platform == TargetPlatform.android)
    {
      OwlDetectionCamera.passFaceFrameSize((faceFrameWidth.toInt()*ratio).toInt(),(faceFrameHeight.toInt()*ratio).toInt());
    }
    else{
      OwlDetectionCamera.passFaceFrameSize(faceFrameWidth.toInt(),faceFrameHeight.toInt());
    }

    var bottomheight = ratio * 48;

    return Stack(
        children: [
          OwlDetectionCamera(),
          Positioned(
              left: pendingWidth/2,
              top: pendingHeight/2,
              child:
              mIsDetectSuccess?
              Image.asset("assets/face_v3_success.png",
                  fit: BoxFit.fill,
                  width: faceFrameWidth,
                  height: faceFrameHeight)
                  :
              Image.asset("assets/face_v3_normal.png",
                  fit: BoxFit.scaleDown,
                  width: faceFrameWidth,
                  height: faceFrameHeight)
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Container(
              width: size.width,
              height: bottomheight,
              child:
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  RaisedButton(
                      child:
                      Text("Start"),
                      onPressed: (){
                        OwlDetectionCamera.startFaceDetection();
                      }),
                  RaisedButton(
                      child:
                      Text("Stop"),
                      onPressed: (){
                        OwlDetectionCamera.stopFaceDetection();
                      }),
                  RaisedButton(
                      child:
                      Text("Brightness"),
                      onPressed: (){
                        OwlDetectionCamera.screenBright(mIsBrightness?OwlCameraDefine.SCREEN_DARK:OwlCameraDefine.SCREEN_BRIGHT);
                        mIsBrightness = !mIsBrightness;
                      }),
                ],
              ),
            ),
          ),
          Align(
            child: mIsDetectSuccess ? successImage(mImagePath) : Text(""),
          ),
          Align(
            child: mIsQRcode? centerText(mQRcodeResult): Text(""),
          )
        ]
    );
  }

  @override
  Widget build(BuildContext context)
  {
    Size size = MediaQuery.of(context).size;

    var ratio = MediaQuery.of(context).devicePixelRatio;

    return Theme.of(context).platform == TargetPlatform.android
        ?
    Scaffold(
      resizeToAvoidBottomInset: false,
      body:
      mPermissionData.mPermissionsState == PermissionsState.checking ?
      check(Constants.sPermission)
          :
      mPermissionData.mPermissionsState == PermissionsState.grainted ?
      preview(size,ratio)
          :
      centerText("Please give permissions."),
    )
        :
    Scaffold(
      resizeToAvoidBottomInset: false,
      body: preview(size,ratio),
    );
  }
}

enum PermissionsState
{
  checking,
  grainted,
  deny
}

class PermissionData
{
  String? mPermissionName;
  PermissionsState mPermissionsState;
  PermissionData(this.mPermissionsState,{this.mPermissionName});
}

class Constants
{
  static List<Permission> sPermission = [Permission.camera,Permission.storage,Permission.phone];

}
