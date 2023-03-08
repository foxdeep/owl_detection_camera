import Flutter
import UIKit

public class SwiftOwlDetectionCameraPlugin: NSObject, FlutterPlugin
{
    static var sFaceFrameWidth = 0
    static var sFaceFrameHeight = 0
    static var sFaceChannel : FlutterMethodChannel!
    
    public static var mFlutterPluginRegistrar:FlutterPluginRegistrar?;
    public static var mMethodCallback:MethodCallback?;
    
    public static func setMethodCallback(aMethodCallback: MethodCallback)
    {
        mMethodCallback = aMethodCallback;
    }
    
    public static func register(with registrar: FlutterPluginRegistrar)
    {
        mFlutterPluginRegistrar = registrar
        self.sFaceChannel = FlutterMethodChannel(name: "owl_detection_camera", binaryMessenger: registrar.messenger())
        let instance = SwiftOwlDetectionCameraPlugin()
        registrar.addMethodCallDelegate(instance, channel: sFaceChannel)
        
        let factory = FLCameraFactory(messenger: registrar.messenger(),aSwiftFlutterOwlCameraPlugin: instance)
        registrar.register(factory, withId: "com.foxconn.strc.owl/cameraview")
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult)
    {
        var faceFrameSize:[NSNumber];
        
        if(call.method == Define.METHOD_CHANNEL_FACE_FRAME_SIZE)
        {
            guard let args = call.arguments as? NSArray else
            {
                return
            }
            
            faceFrameSize = args[0] as? NSArray as! [NSNumber]
            SwiftOwlDetectionCameraPlugin.sFaceFrameWidth = Int(truncating: faceFrameSize[0])
            SwiftOwlDetectionCameraPlugin.sFaceFrameHeight = Int(truncating: faceFrameSize[1])
            print("getFaceFrameSize width: \(faceFrameSize[0]) height: \(faceFrameSize[1])");
        }
        else if(call.method == Define.METHOD_STOP_DETECTION)
        {
            guard let methodCall = SwiftOwlDetectionCameraPlugin.mMethodCallback else
            {
                return;
            }
            
            methodCall.onStopFaceDetection();
        }
        else if(call.method == Define.METHOD_START_DETECTION)
        {
            guard let methodCall = SwiftOwlDetectionCameraPlugin.mMethodCallback else
            {
                return;
            }
            
            methodCall.onStartFaceDetection();
        }
        else if(call.method == Define.METHOD_ID_SCREEN_BRIGHT)
        {
            guard let args = call.arguments as? NSArray else
            {
                return
            }
            
            guard let methodCall = SwiftOwlDetectionCameraPlugin.mMethodCallback else
            {
                return;
            }
            
            let birght:Int = args[0] as? NSNumber as! Int;
            
            methodCall.onScreenBright(birght);
        }
        else if(call.method == Define.METHOD_SET_DETECTION_MODE)
        {
            guard let args = call.arguments as? NSArray else
            {
                return
            }
            
            guard let methodCall = SwiftOwlDetectionCameraPlugin.mMethodCallback else
            {
                return;
            }
            
            let detectMode:Int = args[0] as? NSNumber as! Int;
            
            methodCall.onSetDetectionMode(detectMode);
        }
        else if(call.method == Define.METHOD_DISABLE_HINT)
        {
            guard let args = call.arguments as? NSArray else
            {
                return
            }
            
            guard let methodCall = SwiftOwlDetectionCameraPlugin.mMethodCallback else
            {
                return;
            }
            
            let disableHint:Bool = args[0] as? NSNumber as! Bool;
            
            methodCall.onDisableHint(disableHint);
        }
        else if(call.method == Define.METHOD_SWITCH_CAMERA)
        {
            guard let methodCall = SwiftOwlDetectionCameraPlugin.mMethodCallback else
            {
                return;
            }
                
            methodCall.onSwitchCamera();
        }
        else if(call.method == Define.METHOD_ID_FACE_DETECT_HINT)
        {
            guard let args = call.arguments as? NSArray else
            {
                return
            }
            
            guard let methodCall = SwiftOwlDetectionCameraPlugin.mMethodCallback else
            {
                return;
            }
            
            let hint:[String] = args[0] as? NSArray as! [String]
            
            methodCall.onSetFaceDetectionHintText(hint[0],hint[1],hint[2]);
            
            print("hint center: \(hint[0]) forward: \(hint[1]) barkward: \(hint[2])");
        }
        //    result("iOS " + UIDevice.current.systemVersion)
    }
    
}
