//
//  CameraCV.swift
//  owl_detection_camera
//
//  Created by Josh on 2022/3/18.
//

import Foundation
import UIKit
import Photos
import Vision

protocol CameraVCDelegate
{
    func captureHeadshot(_ image:UIImage,_ memberId:String)
}

class CameraCV : UIViewController
{
    @IBOutlet weak var captureCameraPreview: UIView!
    
    @IBOutlet weak var mFaceResultIV: UIImageView!
    
    let cameraController = CameraController()
    
    var mHintCenter:String = "請將臉部，對應人像框";
    var mHintForward:String = "請向前一點";
    var mHintBackward:String = "有點太近了";
    
    var mFullScreenSize:CGSize?;
    
    var mCameraVCDelegate:CameraVCDelegate?;
    
    var mCameraLensPosition = Define.FRONT_CAMERA;
    
    var mSwiftFlutterOwlCameraPlugin:SwiftOwlDetectionCameraPlugin?;
    
    //隱藏StatusBar
    //    override var prefersStatusBarHidden: Bool{ return true }
}

extension CameraCV
{
    override func viewDidLoad()
    {
        // 取得螢幕的尺寸
        mFullScreenSize = UIScreen.main.bounds.size;
        print("mFullScreenSize width: \(mFullScreenSize?.width) height: \(mFullScreenSize?.height)");
        cameraController.setCameraLens(aLens: mCameraLensPosition)
        configureCameraController();
        self.cameraController.setFaceResultCallback(getFacePhoto); //人臉照片
        self.cameraController.setFaceVisionCallback(drawFaceboundingBox); //畫人臉框
        self.cameraController.setQRcodeCallback(qrcodeCallback);//QRcode
        self.cameraController.setFaceDetectionHintCallback(showFaceDetectionHint);
        
        self.cameraController.setFullScreenSize(aSize: self.mFullScreenSize!);
        
        deleteDirFile();
        
        SwiftOwlDetectionCameraPlugin.setMethodCallback(aMethodCallback: MethodCallback(
            { ()->() in //Stop face detection
                self.cameraController.setFaceHandleStatus(aValue: true)
            },
            { ()->() in //Start face detection
                self.cameraController.setFaceHandleStatus(aValue: false)
            },
            { (aValue:Int)->() in //screen bright
                self.setScreenBright(aValue);
            },
            {(aCenter:String,aForward:String,aBackward:String) -> () in
                self.mHintCenter = aCenter;
                self.mHintForward = aForward;
                self.mHintBackward = aBackward;
            }
        ));
    }
    
    func deleteDirFile()
    {
        let path = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]
        guard let items = try? FileManager.default.contentsOfDirectory(atPath: path) else { return }
        
        for item in items {
            // This can be made better by using pathComponent
            let completePath = path.appending("/").appending(item)
            try? FileManager.default.removeItem(atPath: completePath)
        }
        
        //        let fileURLss = Utility.documentsUrl.appendingPathComponent("OwlFaceIdClockIn.jpg")
        //        do {
        //            try FileManager.default.removeItem(at: fileURLss)
        //            print("delete success")
        //        } catch let error as NSError {
        //            print("delete error: ", error.localizedDescription)
        //        }
    }
    
    func showFaceDetectionHint(aValue:Int)
    {
        switch(aValue)
        {
            case Define.DETECTION_HINT_FIT_CENTER:
                Utility.showToast(controller: self, message : self.mHintCenter, seconds: 1.0)
                break
            case Define.DETECTION_HINT_FORWARD:
                Utility.showToast(controller: self, message : self.mHintForward, seconds: 1.0)
                break
            case Define.DETECTION_HINT_BACKWARD:
                Utility.showToast(controller: self, message : self.mHintBackward, seconds: 1.0)
                break
            default:
                return;
        }
    }
    
    func setScreenBright(_ aValue:Int)
    {
        switch(aValue)
        {
            case Define.SCREEN_BRIGHT:
                UIScreen.main.brightness = CGFloat(0.9);
                break;
            case Define.SCREEN_DARK:
                UIScreen.main.brightness = CGFloat(0.3);
                break;
            default:
                break;
        }
    }
    
    func setSwiftFlutterOwlCameraPlugin(aSwiftFlutterOwlCameraPlugin: SwiftOwlDetectionCameraPlugin)
    {
        mSwiftFlutterOwlCameraPlugin = aSwiftFlutterOwlCameraPlugin;
    }
    
    override func viewWillAppear(_ animated: Bool)
    {
        super.viewWillAppear(animated)
        //隱藏actionBar
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
    
    override func viewDidDisappear(_ animated: Bool)
    {
        self.cameraController.captureSession?.stopRunning()
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        print("segue.identifier: \(segue.identifier) segue.destination: \(segue.destination)")
    }
    
    func configureCameraController()
    {
        cameraController.prepareCamera
        { (errors) in
            if let error = errors
            {
                print(error)
            }
            
            try? self.cameraController.displayPreview(to: self.captureCameraPreview)
        }
    }
    
    func getFacePhoto(_ aResult: UIImage)->Void
    {
        let result = Utility.save(image: aResult)
        
        let faceChannel = FlutterMethodChannel(name: Define.flutter_owl_camera_plugin, binaryMessenger: SwiftOwlDetectionCameraPlugin.mFlutterPluginRegistrar!.messenger())
        
        var dic = Dictionary<String, Any>();
        dic["imageName"] = result[1]
        dic["imagePath"] = result[2]
        faceChannel.invokeMethod(Define.INVOKE_ID_IMAGE_PATH_NAME, arguments:dic)
        
        //PHPhotoLibary類別來儲存圖片到內置的相片資料庫。
        //        try? PHPhotoLibrary.shared().performChangesAndWait
        //        {
        //            PHAssetChangeRequest.creationRequestForAsset(from: aResult)
        //        }
        
    }
    
    func getFaceResult(_ faceResult:CIImage)->Void
    {
        let accuracy = [CIDetectorAccuracy: CIDetectorAccuracyHigh] //Dictionary
        let faceDetector = CIDetector(ofType: CIDetectorTypeFace, context: nil, options: accuracy)
        let faces = faceDetector?.features(in: faceResult)
        
        if(mCameraLensPosition == Define.FRONT_CAMERA)
        {
            // 用來將 Core Image 座標轉換成 UIView 座標
            let ciImageSize = faceResult.extent.size //1920x1080
            if !faces!.isEmpty
            {
                let oneFace:CIFaceFeature = faces?[0] as! CIFaceFeature
                
                var faceViewBounds:CGRect = CGRect(x: oneFace.bounds.origin.y,y: oneFace.bounds.origin.x,width: oneFace.bounds.height,height: oneFace.bounds.width);
                
                // 計算矩形在 imageView 中的實際位置和大小
                let viewSize = mFaceResultIV.bounds.size //螢幕的大小 (375.0, 667.0)
                
                let newX = (viewSize.width - faceViewBounds.origin.x-1)/2;
                faceViewBounds.origin.x = newX;
                
                let scale = min(viewSize.width / ciImageSize.height,viewSize.height / ciImageSize.width)
                
                faceViewBounds = faceViewBounds.applying(CGAffineTransform(scaleX: scale, y: scale))
                
                mFaceResultIV.image = nil
                drawFaceFrame(faceViewBounds)
            }
        }
        else
        {
            // 用來將 Core Image 座標轉換成 UIView 座標
            let ciImageSize = faceResult.extent.size //1920x1080
            if !faces!.isEmpty
            {
                let oneFace:CIFaceFeature = faces?[0] as! CIFaceFeature
                
                var faceViewBounds:CGRect = CGRect(x: oneFace.bounds.origin.y,y: oneFace.bounds.origin.x,width: oneFace.bounds.height,height: oneFace.bounds.width);
                
                // 計算矩形在 imageView 中的實際位置和大小
                let viewSize = mFaceResultIV.bounds.size //螢幕的大小 (375.0, 667.0)
                
                let scale = min(viewSize.width / ciImageSize.height,viewSize.height / ciImageSize.width)
                
                faceViewBounds = faceViewBounds.applying(CGAffineTransform(scaleX: scale, y: scale))
                mFaceResultIV.image = nil
                drawFaceFrame(faceViewBounds)
            }
        }
    }
    
    func drawFaceFrame(_ faceFrame:CGRect)
    {
        // set the context to that of an image
        UIGraphicsBeginImageContext(mFaceResultIV.frame.size)
        let context = UIGraphicsGetCurrentContext()
        let drawingRect = faceFrame
        //創建並設置路徑
        let path = CGMutablePath()
        path.addRect(drawingRect)
        context!.addPath(path)
        //設置筆觸顏色
        context!.setStrokeColor(UIColor.orange.cgColor)
        //設置筆觸寬度
        context!.setLineWidth(2)
        
        //繪製路徑並填充
        context!.strokePath()
        // obtain a UIImage object from the context
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        // set the UIImageView's image to the new, generated image
        mFaceResultIV.image = newImage
    }
    
    func drawFaceboundingBox(face : VNFaceObservation)
    {
        let transform = CGAffineTransform(scaleX: 1, y: -1).translatedBy(x: 0, y: -mFullScreenSize!.height)
        
        let translate = CGAffineTransform.identity.scaledBy(x: mFullScreenSize!.width, y: mFullScreenSize!.height)
        
        // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
        let facebounds:CGRect = face.boundingBox.applying(translate).applying(transform)
        
        let faceChannel = FlutterMethodChannel(name: Define.flutter_owl_camera_plugin, binaryMessenger: SwiftOwlDetectionCameraPlugin.mFlutterPluginRegistrar!.messenger())
        
        let rectangle:[Double] = [Double(facebounds.origin.x),Double(facebounds.origin.y),Double(facebounds.origin.x+facebounds.width),Double(facebounds.origin.y+facebounds.height)]
        
        var dictionary = Dictionary<String, [Double]>();
        dictionary["face_rectangle"] = rectangle;
        
        //The face rectangle to Flutter for drawing detect face frame.
        faceChannel.invokeMethod(Define.INVOKE_ID_FACE_RECTANGLE, arguments:dictionary)
        
        //IOS native draw face frame.
        //   drawFaceFrame(facebounds);
    }
    
    func qrcodeCallback(aMessage: String)
    {
        let faceChannel = FlutterMethodChannel(name: Define.flutter_owl_camera_plugin, binaryMessenger: SwiftOwlDetectionCameraPlugin.mFlutterPluginRegistrar!.messenger())
        
        var dictionary = Dictionary<String, String>();
        dictionary["qrcode_result"] = aMessage;
        
        //The face rectangle to Flutter for drawing detect face frame.
        faceChannel.invokeMethod(Define.INVOKE_ID_QRCODE_TEXT, arguments:dictionary)
    }
    
}
