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
  bool mIsDisableHint = true;

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

    ///IOS maybe need times devicePixelRatio for real screen pixel.
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

    OwlDetectionCamera.writeSettingPermission();
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
    await Future.delayed(const Duration(milliseconds: 2000), ()
    {
      setState(() {
        mIsDetectSuccess = false;
        mIsQRcode= false;
        OwlDetectionCamera.startDetection();
      });
    });
  }

  Widget preview(var size,var ratio)
  {
    var faceFrameWidth=0.0,faceFrameHeight=0.0;
    if(size.width< size.height)
    {
      faceFrameWidth = size.width/1.5;
      faceFrameHeight = faceFrameWidth * 1.25;
    }
    else{
      faceFrameHeight = size.height/1.5;
      faceFrameWidth = faceFrameHeight * 1.25;
    }

    var pendingWidth = size.width - faceFrameWidth;
    var pendingHeight = size.height - faceFrameHeight;

    if(Theme.of(context).platform == TargetPlatform.android)
    {
      OwlDetectionCamera.passFaceFrameSize((faceFrameWidth.toInt()*ratio).toInt(),(faceFrameHeight.toInt()*ratio).toInt());
    }
    else{
      OwlDetectionCamera.passFaceFrameSize(faceFrameWidth.toInt(),faceFrameHeight.toInt());
    }

    return Stack(
        children: [
          OwlDetectionCamera(),
          Positioned(
              left: pendingWidth/2,
              top: pendingHeight/2,
              child:
              mIsDetectSuccess?
              Image.asset("assets/face_v3_success.png",
                  fit: BoxFit.scaleDown,
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
              // height: bottomheight,
              child:
                  Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          Expanded(
                            flex: 1,
                            child:  ElevatedButton(
                                child:
                                Text("Start",style: TextStyle(fontSize: 10),),
                                onPressed: (){
                                  OwlDetectionCamera.startDetection();
                                }),
                          ),
                          Expanded(
                            flex: 1,
                            child:  ElevatedButton(
                                child:
                                Text("Stop",style: TextStyle(fontSize: 10)),
                                onPressed: (){
                                  OwlDetectionCamera.stopDetection();
                                }),
                          ),
                          Expanded(
                            flex: 1,
                            child: ElevatedButton(
                                child:
                                Text("Brightness",style: TextStyle(fontSize: 10)),
                                onPressed: (){
                                  OwlDetectionCamera.screenBright(mIsBrightness?OwlCameraDefine.SCREEN_DARK:OwlCameraDefine.SCREEN_BRIGHT);
                                  mIsBrightness = !mIsBrightness;
                                }),
                          ),
                          Expanded(
                            flex: 1,
                            child: ElevatedButton(
                                child:
                                Text("Disable Hint",style: TextStyle(fontSize: 10)),
                                onPressed: (){
                                  OwlDetectionCamera.disableHint(mIsDisableHint);
                                  mIsDisableHint = !mIsDisableHint;
                                }),
                          ),
                        ],
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          Expanded(
                            flex: 1,
                            child: ElevatedButton(
                                child:
                                Text("Detect QRcode",style: TextStyle(fontSize: 10)),
                                onPressed: ()
                                {
                                  OwlDetectionCamera.setDetectionMode(OwlCameraDefine.QRCODE_MODE);
                                }) ,
                          ),
                          Expanded(
                            flex: 1,
                            child: ElevatedButton(
                                child:
                                Text("Detect Face",style: TextStyle(fontSize: 10)),
                                onPressed: ()
                                {
                                  OwlDetectionCamera.setDetectionMode(OwlCameraDefine.FACE_MODE);
                                }),
                          ),
                          Expanded(
                            flex: 1,
                            child: ElevatedButton(
                                child:
                                Text("Blend mode",style: TextStyle(fontSize: 10)),
                                onPressed: ()
                                {
                                  OwlDetectionCamera.setDetectionMode(OwlCameraDefine.BLEND_MODE);
                                }),
                          ),
                          Expanded(
                            flex: 1,
                            child: ElevatedButton(
                                child:
                                Text("Switch Camera",style: TextStyle(fontSize: 10)),
                                onPressed: ()
                                {
                                  OwlDetectionCamera.switchCamera();
                                }),
                          ),
                        ],
                      ),
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
