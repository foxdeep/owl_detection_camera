//
//  CameraController.swift
//  owl_detection_camera
//
//  Created by Josh on 2022/3/18.
//

import Foundation
import CoreImage
import UIKit
import AVFoundation
import Vision

class CameraController: NSObject,AVCaptureMetadataOutputObjectsDelegate
{
    // TODO: Decide camera position --- front or back
    private var devicePosition: AVCaptureDevice.Position = .front
    
    var captureSession: AVCaptureSession?
    
    var currentCameraPosition: CameraPosition?
    
    var frontCamera: AVCaptureDevice?
    var frontCameraInput: AVCaptureDeviceInput?
    
    var photoOutputs: AVCapturePhotoOutput?
    
    var videoOutputs: AVCaptureVideoDataOutput?
    
    var rearCamera: AVCaptureDevice?
    var rearCameraInput: AVCaptureDeviceInput?
    
    var previewLayer: AVCaptureVideoPreviewLayer?
    
    var flashMode = AVCaptureDevice.FlashMode.off
    
    var photoCaptureCompletionBlock: ((UIImage?, Error?) -> Void)?
    
    var faceResult:((UIImage)->Void)!;
    
    var faceVisionFrameResult:((VNFaceObservation)->Void)?;
    
    var mQRcodeResult:((String)->Void)?;
    var mFaceDetectionHintCallback:((Int)->Void)?;
    
    var mCameraLensPostion:Int = Define.FRONT_CAMERA;
    
    var mDetectionMode:Int = Define.BLEND_MODE;
    
    var mScreenCGSize:CGSize?;
    
    var mHasFace = false;
    var mIsHandleResultToServer = true;
    
    var mFacebounds:CGRect?;
    
    var mCountWrongPost = 0;//用來提示使用者臉該往哪擺
    
    // VNRequest: Either Retangles or Landmarks
    private var faceDetectionRequest: VNRequest!
    
    //Requests 是指當你要求 Framework 為你偵測一些東西時
    private var requests = [VNRequest]()
}

//AVCapturePhotoCaptureDelegate: 擷取照片Caputre的事件.
//AVCaptureVideoDataOutputSampleBufferDelegate: 每一個進來的Frame的事件.
extension CameraController : AVCapturePhotoCaptureDelegate,AVCaptureVideoDataOutputSampleBufferDelegate
{
    enum CameraControllerError: Swift.Error
    {
        case captureSessionAlreadyRunning
        case captureSessionIsMissing
        case inputsAreInvalid
        case invalidOperation
        case noCamerasAvailable
        case unknown
    }
    
    public enum CameraPosition
    {
        case front
        case rear
    }
    
    func setFullScreenSize(aSize: CGSize)
    {
        mScreenCGSize = aSize;
        print("aSize width: \(aSize.width) height: \(aSize.height)");
    }
    
    func setFaceHandleStatus(aValue:Bool)
    {
        mIsHandleResultToServer = aValue;
        mHasFace = false;
    }
    
    func setCameraLens(aLens:Int)
    {
        self.mCameraLensPostion = aLens;
        
        do {
            try self.switchCameras();
        }
        catch {
            print("setCameraLens() catch error: \(error)")
        }
    }
    
    func setDetectionMode(_ aMode:Int)
    {
        self.mDetectionMode = aMode;
    }
    
    func setFaceResultCallback(_ callback: @escaping (UIImage)->Void)
    {
        faceResult = callback;
    }
    
    func setFaceVisionCallback(_ callback: @escaping (VNFaceObservation)->Void)
    {
        faceVisionFrameResult = callback;
    }
    
    //@escaping讓closure function能夠賦予給別人使用
    func setQRcodeCallback(_ callback: @escaping (String)->Void)
    {
        mQRcodeResult = callback;
    }
    
    func setFaceDetectionHintCallback(_ callback: @escaping (Int)->Void)
    {
        mFaceDetectionHintCallback = callback;
    }
    
    //photoOutput is AVCapturePhotoCaptureDelegate func
    public func photoOutput(_ captureOutput: AVCapturePhotoOutput, didFinishProcessingPhoto photoSampleBuffer: CMSampleBuffer?, previewPhoto previewPhotoSampleBuffer: CMSampleBuffer?,resolvedSettings: AVCaptureResolvedPhotoSettings, bracketSettings: AVCaptureBracketedStillImageSettings?, error: Swift.Error?)
    {
        print("photoOut")
        
        if let errors = error
        {
            self.photoCaptureCompletionBlock?(nil, errors)
        }
        else if let buffer = photoSampleBuffer,let data = AVCapturePhotoOutput.jpegPhotoDataRepresentation(forJPEGSampleBuffer: buffer, previewPhotoSampleBuffer: nil)
        {
            
            let image = UIImage(data: data)
            //底下繼承UIImage，加入一個rotate func
            let newImage = image!.rotate(radians: .pi*2)
            
            self.photoCaptureCompletionBlock?(newImage, nil)
        }
        else {
            self.photoCaptureCompletionBlock?(nil, CameraControllerError.unknown)
        }
    }
    
    func exifOrientationFromDeviceOrientation() -> UInt32
    {
        enum DeviceOrientation: UInt32
        {
            case top0ColLeft = 1
            case top0ColRight = 2
            case bottom0ColRight = 3
            case bottom0ColLeft = 4
            case left0ColTop = 5
            case right0ColTop = 6
            case right0ColBottom = 7
            case left0ColBottom = 8
        }
        
        var exifOrientation: DeviceOrientation
        
        switch UIDevice.current.orientation
        {
        case .portraitUpsideDown:
            exifOrientation = .left0ColBottom
        case .landscapeLeft:
            exifOrientation = devicePosition == .front ? .bottom0ColRight : .top0ColLeft
        case .landscapeRight:
            exifOrientation = devicePosition == .front ? .top0ColLeft : .bottom0ColRight
        default:
            exifOrientation = devicePosition == .front ? .left0ColTop : .right0ColTop
        }
        return exifOrientation.rawValue
    }
    
    func checkIncludFace(request: VNRequest)
    {
        //perform all the UI updates on the main queue
        guard let results = request.results as? [VNFaceObservation] else { return }

        //  self.previewView.removeMask()

        // if let callback = self.faceVisionFrameResult
        // {
        for face in results
        {
            let transform = CGAffineTransform(scaleX: 1, y: -1).translatedBy(x: 0, y: -self.mScreenCGSize!.height)

            let translate = CGAffineTransform.identity.scaledBy(x: self.mScreenCGSize!.width, y: self.mScreenCGSize!.height)

            // The coordinates are normalized to the dimensions of the processed image, with the origin at the image's lower-left corner.
            self.mFacebounds = face.boundingBox.applying(translate).applying(transform)

            let pendingWidth = (Int(self.mScreenCGSize!.width) - SwiftOwlDetectionCameraPlugin.sFaceFrameWidth)/2;
            let pendingHeight = (Int(self.mScreenCGSize!.height) - SwiftOwlDetectionCameraPlugin.sFaceFrameHeight)/2;

            let chectRect = CGRect(origin:CGPoint(x:pendingWidth,y:pendingHeight),size:CGSize(width: SwiftOwlDetectionCameraPlugin.sFaceFrameWidth, height: SwiftOwlDetectionCameraPlugin.sFaceFrameHeight));

            if(self.mCountWrongPost==0)
            {
                self.mFaceDetectionHintCallback!(Define.DETECTION_HINT_FIT_CENTER);
                self.mCountWrongPost = Define.COUNT_WRONG_POST_DELAY_TIME;
            }

            //偵測臉的位置的線匡，丟回去給UI做顯示
            self.faceVisionFrameResult!(face);

            //檢查是否有在人臉框內
            if(chectRect.contains(self.mFacebounds!))
            {
                if(min(chectRect.width,self.mFacebounds!.height) < (chectRect.width)/1.3)
                {
                    //too far
                    if(self.mCountWrongPost < 10)
                    {
                        self.mCountWrongPost = Define.COUNT_WRONG_POST_DELAY_TIME;
                        self.mFaceDetectionHintCallback!(Define.DETECTION_HINT_FORWARD);
                    }
                    else{
                        self.mCountWrongPost-=1;
                    }
                }
                else{
                    self.mHasFace = true;
                    self.mCountWrongPost = 0;
                    break;
                }
            }
            else
            {
                if(self.mCountWrongPost < 10)
                {
                    let distance = sqrt((chectRect.width/2 - self.mFacebounds!.width/2)*2 + (chectRect.height/2 - self.mFacebounds!.height/2)*2);

                    //too close
                    if(distance<45 && (chectRect.width < self.mFacebounds!.width || chectRect.height < self.mFacebounds!.height))
                    {
                        self.mCountWrongPost = Define.COUNT_WRONG_POST_DELAY_TIME;
                        self.mFaceDetectionHintCallback!(Define.DETECTION_HINT_BACKWARD);
                    }
                }

                if(self.mCountWrongPost>=10)
                {
                    self.mCountWrongPost-=1;
                }
            }
        }
    }
    
    //Handlers 是指當你想要 Framework 在 Request 產生後執行一些東西或處理這個 Request 時
    func handleFaces(request: VNRequest, error: Error?)
    {
        if(self.mDetectionMode == Define.QRCODE_MODE)
        {
            return;
        }
        
        if(!mIsHandleResultToServer)
        {
            DispatchQueue.main.async {
                self.checkIncludFace(request:request)
            }
        }
        
    }
    
    //Vision的callBack用來收QRCode影像
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection)
    {
        if(self.mDetectionMode == Define.FACE_MODE)
        {
            return;
        }
                
        // 檢查 metadataObjects 陣列是否為非空值，它至少需包含一個物件
        if metadataObjects == nil || metadataObjects.count == 0
        {
            //qrCodeFrameView?.frame = CGRectZero
            //messageLabel.text = "No QR code is detected"
            print("No QR code is detected");
            return
        }
        
        //取得元資料（metadata）物件
        let metadataObj = metadataObjects[0] as! AVMetadataMachineReadableCodeObject
        if metadataObj.type == AVMetadataObject.ObjectType.qr
        {
            //倘若發現的原資料與 QR code 原資料相同，便更新狀態標籤的文字並設定邊界
            if metadataObj.stringValue != nil
            {
                let qrText = metadataObj.stringValue
                self.mQRcodeResult!(qrText!);
                print("QRcode Msg: \(String(describing: qrText))");
            }
        }
    }
    
    //    func metadataOutput(captureOutput: AVCaptureOutput!, didOutputMetadataObjects metadataObjects: [AnyObject]!, fromConnection connection: AVCaptureConnection!)
    //    {
    //        // 檢查 metadataObjects 陣列是否為非空值，它至少需包含一個物件
    //        if metadataObjects == nil || metadataObjects.count == 0
    //        {
    //            //            qrCodeFrameView?.frame = CGRectZero
    //            //            messageLabel.text = "No QR code is detected"
    //            print("No QR code is detected");
    //            return
    //        }
    //
    //        //        // 取得元資料（metadata）物件
    //        let metadataObj = metadataObjects[0] as! AVMetadataMachineReadableCodeObject
    //        if metadataObj.type == AVMetadataObject.ObjectType.qr
    //        {
    //            //倘若發現的原資料與 QR code 原資料相同，便更新狀態標籤的文字並設定邊界
    //            if metadataObj.stringValue != nil
    //            {
    //                let qrText = metadataObj.stringValue
    //                print("QRcode Msg: \(qrText)");
    //            }
    //        }
    //    }
    
    //鏡頭照片一般的callBack
    //AVCaptureVideoDataOutputSampleBufferDelegate的func 設置給setSampleBufferDelegate
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection)
    {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer),let exifOrientation = CGImagePropertyOrientation(rawValue: exifOrientationFromDeviceOrientation()) else { return }
        
        var requestOptions: [VNImageOption : Any] = [:]
        
        if let cameraIntrinsicData = CMGetAttachment(sampleBuffer, key: kCMSampleBufferAttachmentKey_CameraIntrinsicMatrix, attachmentModeOut: nil)
        {
            requestOptions = [.cameraIntrinsics : cameraIntrinsicData]
        }
        
        if(!mHasFace)
        {
            let imageRequestHandler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: exifOrientation, options: requestOptions)
            
            do {
                //透過VNRequest去處理所想要處理的Vision
                try imageRequestHandler.perform(requests)
            }
            catch {
                print(error)
            }
        }else
        {
//            print("mIsHandleResultToServer: \(mIsHandleResultToServer)");
            
            if(!mIsHandleResultToServer)
            {
                mIsHandleResultToServer = true;
                
                let buffer = sampleBuffer;
                
                let images = Utility.imageFromSampleBuffer(sampleBuffer: buffer);
                
                //flip image for mirror
                let newImage = images.rotate(radians: .pi/2)!.withHorizontallyFlippedOrientation()
                let moreGap:CGFloat = 70
                let widthRatio = (newImage.size.width/self.mScreenCGSize!.width);
                let heightRatio = (newImage.size.height/self.mScreenCGSize!.height);
                print("widthRatio:\(widthRatio) heightRatio: \(heightRatio)");
                
                var realPiexl = rotateRect(CGRect(origin: CGPoint(x:(self.mFacebounds!.origin.x * widthRatio),y:self.mFacebounds!.origin.y*heightRatio), size:CGSize(width:(self.mFacebounds!.width)*widthRatio,height:(self.mFacebounds!.height)*heightRatio)));
                realPiexl.size.width = realPiexl.width + moreGap
                realPiexl.size.height = realPiexl.height + moreGap
                realPiexl.origin.x = realPiexl.origin.x - moreGap;
                realPiexl.origin.y = realPiexl.origin.y - moreGap;
                
                let cropImage = newImage.cropImage1( newImage , realPiexl);
                
                //                if let faceCallback = faceResult
                //                {
                self.faceResult(cropImage);
                
                //                    DispatchQueue.main.async
                //                    {
                //                        faceCallback(newImage);
                //                    }
                //                }
            }
        }
        
        
        
        //------------------before-----------------
        //        let time = Utility.getTime();
        //        if let image = self.getImageFromSampleBuffer(buffer: sampleBuffer)
        //        {
        //            if let faceCallback = faceResult
        //            {
        //                DispatchQueue.main.async
        //                {
        //                    faceCallback(image);
        //                }
        //            }
        //        }
        
        //           DispatchQueue.main.async
        //           {
        //              let photoVC = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "PhotoVC") as! PhotoViewController
        //              photoVC.takenPhoto = image
        //              self.present(photoVC, animated: true, completion: {self.stopCaptureSession()})
        //           }
        //------------------before-----------------
    }
    
    func rotateRect(_ rect: CGRect) -> CGRect {
        let x = rect.midX
        let y = rect.midY
        let transform = CGAffineTransform(translationX: x, y: y)
            .rotated(by: .pi / 2)
            .translatedBy(x: -x, y: -y)
        return rect.applying(transform)
    }
    
    func getImageFromSampleBuffer (buffer:CMSampleBuffer) -> CIImage?
    {
        if let pixelBuffer = CMSampleBufferGetImageBuffer(buffer)
        {
            let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
            let context = CIContext()
            
            let imageRect = CGRect(x: 0, y: 0, width: CVPixelBufferGetWidth(pixelBuffer), height: CVPixelBufferGetHeight(pixelBuffer))
            
            if let image = context.createCGImage(ciImage, from: imageRect)
            {
                return ciImage
            }
        }
        return nil
    }
    
    func captureImage(completion: @escaping (UIImage?, Error?) -> Void)
    {
        let aAVCaptureConnection = self.photoOutputs!.connection(with: AVMediaType.video);
        if aAVCaptureConnection != nil
        {
            //effect
            aAVCaptureConnection!.videoOrientation = returnedOrientation();
        }
        
        guard let captureSession = captureSession, captureSession.isRunning else { completion(nil, CameraControllerError.captureSessionIsMissing); return }
        
        let settings = AVCapturePhotoSettings()
        settings.flashMode = self.flashMode
        
        self.photoOutputs?.capturePhoto(with: settings, delegate: self)
        self.photoCaptureCompletionBlock = completion
    }
    
    func switchCameras() throws
    {
        guard let currentCameraPositions = currentCameraPosition, let captureSession = self.captureSession, captureSession.isRunning
        else
        {
            throw CameraControllerError.captureSessionIsMissing
        }
        
        captureSession.beginConfiguration()
        
        func switchToFrontCamera() throws
        {
            //同時滿足rearCameraInput不是nil 且 裝置包含後鏡頭 且 frontCamera不是nil
            guard let rearCameraInput = self.rearCameraInput, captureSession.inputs.contains(rearCameraInput),let frontCamera = self.frontCamera
            else
            {
                throw CameraControllerError.invalidOperation
            }
            
            //取得前鏡頭輸入
            self.frontCameraInput = try AVCaptureDeviceInput(device: frontCamera)
            
            //從目前的Session中移除後鏡頭的input
            captureSession.removeInput(rearCameraInput)
            
            //判斷是否可以加入前/Users/josh/Documents/Ios專案/AppleFaceDetection-master/VisionDetection/ViewController.swift鏡頭Input
            if captureSession.canAddInput(self.frontCameraInput!)
            {
                //加入前鏡頭input
                captureSession.addInput(self.frontCameraInput!)
                self.currentCameraPosition = CameraPosition.front
            }
            else {
                throw CameraControllerError.invalidOperation
            }
            
            captureSession.commitConfiguration()
        }
        
        func switchToRearCamera() throws
        {
            //同時滿足frontCameraInput不是nil 且 裝置包含前鏡頭 且 rearCamera不是nil
            guard let frontCameraInput = self.frontCameraInput, captureSession.inputs.contains(frontCameraInput),let rearCamera = self.rearCamera
            else
            {
                throw CameraControllerError.invalidOperation
            }
            
            //取得後鏡頭輸入
            self.rearCameraInput = try AVCaptureDeviceInput(device: rearCamera)
            
            //從目前的Session中移除前鏡頭的input
            captureSession.removeInput(frontCameraInput)
            
            //判斷是否可以加入後鏡頭Input
            if captureSession.canAddInput(self.rearCameraInput!)
            {
                //加入後鏡頭input
                captureSession.addInput(self.rearCameraInput!)
                
                self.currentCameraPosition = .rear
            }
            else
            {
                throw CameraControllerError.invalidOperation
            }
            
            captureSession.commitConfiguration()
        }
        
        switch currentCameraPositions
        {
            case CameraPosition.front:
                try switchToRearCamera()
                
            case CameraPosition.rear:
                try switchToFrontCamera()
        }
    }
    
    func returnedOrientation() -> AVCaptureVideoOrientation
    {
        var videoOrientation: AVCaptureVideoOrientation!
        let orientation = UIDevice.current.orientation
        switch orientation {
            case .portrait:
                videoOrientation = .portrait
            //userDefault.setInteger(0, forKey: "CaptureVideoOrientation")
            case .portraitUpsideDown:
                videoOrientation = .portraitUpsideDown
            //userDefault.setInteger(1, forKey: "CaptureVideoOrientation")
            case .landscapeLeft:
                videoOrientation = .landscapeRight
            //userDefault.setInteger(2, forKey: "CaptureVideoOrientation")
            case .landscapeRight:
                videoOrientation = .landscapeLeft
            // userDefault.setInteger(3, forKey: "CaptureVideoOrientation")
            case .faceDown, .faceUp, .unknown:
                // let digit = userDefault.integerForKey("CaptureVideoOrientation")
                videoOrientation = AVCaptureVideoOrientation.init(rawValue:0)
        }
        return videoOrientation
    }
    
    func displayPreview(to view: UIView) throws
    {
        guard let captureSession = self.captureSession, captureSession.isRunning else { throw CameraControllerError.captureSessionIsMissing }
        
        self.previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
       
        print(" self.previewLayer?.bounds: \(self.previewLayer?.bounds)");

        self.previewLayer?.videoGravity = AVLayerVideoGravity.resizeAspectFill
        self.previewLayer?.connection?.videoOrientation = .portrait
        
        view.layer.insertSublayer(self.previewLayer!, at: 0)
        self.previewLayer?.frame = view.frame
    }
    
    //    func doFocus() throws
    //    {
    //        guard let currentCameraPosition = currentCameraPosition, let captureSession = self.captureSession, captureSession.isRunning else { throw CameraControllerError.captureSessionIsMissing }
    //
    //        switch currentCameraPosition
    //        {
    //            case CameraPosition.rear:
    //                if let camera = rearCamera
    //                {
    //                    try camera.lockForConfiguration()
    //                    camera.focusMode = .autoFocus
    //                    camera.unlockForConfiguration()
    //
    //
    //                }
    //            case .front:
    //            print("front")
    //        }
    //    }
    
    //camera focus callback
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey: Any]?, context: UnsafeMutableRawPointer?)
    {
        if keyPath == "adjustingFocus"
        {
            print("============== adjustingFocus: \(rearCamera?.lensPosition)")
        }
    }
    
    func prepareCamera(completionHandler: @escaping (Error?) -> Void)
    {
        func createCaptureSession()
        {
            self.captureSession = AVCaptureSession()
        }
        
        func setupVision()
        {
            // Set up Vision Request
            faceDetectionRequest = VNDetectFaceRectanglesRequest(completionHandler: self.handleFaces) // Default
            self.requests = [faceDetectionRequest]
        }
        
        func configureCaptureDevices() throws
        {
            //deviceType:選擇要使用的相機類型ex:一般廣角、廣角、超廣角.etc.
            //mediaType: 選擇多媒體類型ex:視頻、音頻.etc.
            //position:  選擇鏡頭ex:前鏡頭、後鏡頭.
            let session = AVCaptureDevice.DiscoverySession(deviceTypes: [AVCaptureDevice.DeviceType.builtInWideAngleCamera], mediaType: AVMediaType.video, position: .unspecified)
            let cameras = session.devices.compactMap { $0 }
            
            guard !cameras.isEmpty else { throw CameraControllerError.noCamerasAvailable }
            
            for camera in cameras
            {
//                if(mCameraLensPostion == Define.FRONT_CAMERA)
//                {
                    if camera.position == .front
                    {
                        
                        self.frontCamera = camera
                        try camera.lockForConfiguration()
                        camera.unlockForConfiguration()
                        camera.addObserver(self, forKeyPath: "adjustingFocus", options: .new, context: nil)
                    }
//                }
//                else{
                    if camera.position == .back
                    {
                        self.rearCamera = camera
                        try camera.lockForConfiguration()
                        camera.focusMode = .continuousAutoFocus
                        camera.unlockForConfiguration()
                        camera.addObserver(self, forKeyPath: "adjustingFocus", options: .new, context: nil)
                    }
//                }
            }
        }
        
        func configureDeviceInputs() throws
        {
            guard let captureSession = self.captureSession
            else
            {
                throw CameraControllerError.captureSessionIsMissing
            }
            
            //設定攝影機解析度
            if captureSession.canSetSessionPreset(.photo)
            {
                print("sessionPreset")
                /*設定輸出的視頻音頻品質比特率(output video and audio bit rates)
                 AVCaptureSession.Preset:
                 photo:輸出為4:3
                 必須根據所要輸出的比率設定UI Preview Screen
                 */
                captureSession.sessionPreset = AVCaptureSession.Preset.photo;
            }
            
            if let frontCamera = self.frontCamera
            {
               self.frontCameraInput = try AVCaptureDeviceInput(device: frontCamera)
               
               if captureSession.canAddInput(self.frontCameraInput!)
               {
                   captureSession.addInput(self.frontCameraInput!)
               }
               else
               {
                   throw CameraControllerError.inputsAreInvalid
               }
               
               self.currentCameraPosition = .front
            }
            else if let rearCamera = self.rearCamera
            {
                self.rearCameraInput = try AVCaptureDeviceInput(device: rearCamera)
                
                if captureSession.canAddInput(self.rearCameraInput!)
                {
                    captureSession.addInput(self.rearCameraInput!)
                }
                
                self.currentCameraPosition = .rear
            }
            else
            {
                throw CameraControllerError.noCamerasAvailable
            }
        }
        
        func configurePhotoOutput() throws
        {
            guard let captureSession = self.captureSession else { throw CameraControllerError.captureSessionIsMissing }
            
            self.photoOutputs = AVCapturePhotoOutput()
            self.photoOutputs!.setPreparedPhotoSettingsArray([AVCapturePhotoSettings(format: [AVVideoCodecKey : AVVideoCodecJPEG])], completionHandler: nil)
            //這邊AVCaptureConnection一定是null，因為camera還沒啟動，要修改應該在拍照按鈕按下時。
            //self.photoOutputs!.connection(with: AVMediaType.video)?.videoOrientation = returnedOrientation();
            
            if captureSession.canAddOutput(self.photoOutputs!)
            {
                captureSession.addOutput(self.photoOutputs!)
            }
            
            // 初始化 AVCaptureMetadataOutput 物件並將其設定作為擷取session的輸出裝置
            let captureMetadataOutput = AVCaptureMetadataOutput()
            captureSession.addOutput(captureMetadataOutput)
            
            // 設定代理並使用預設的調度佇列來執行回呼（call back）這邊的self是指metadataOutput()
            captureMetadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            captureMetadataOutput.metadataObjectTypes = [AVMetadataObject.ObjectType.qr] //這是我們告訴App哪一種元資料是我們感興趣
            
            captureSession.startRunning()
        }
        
        func configureVideoDataOutput() throws
        {
            guard let captureSession = self.captureSession else { throw CameraControllerError.captureSessionIsMissing }
            
            videoOutputs = AVCaptureVideoDataOutput()
            
            videoOutputs!.videoSettings = [((kCVPixelBufferPixelFormatTypeKey as NSString) as String):NSNumber(value:kCVPixelFormatType_32BGRA)]
            
            videoOutputs!.alwaysDiscardsLateVideoFrames = true
            //videoOutputs.
            
            if captureSession.canAddOutput(self.videoOutputs!)
            {
                captureSession.addOutput(self.videoOutputs!)
            }
            
            let queue = DispatchQueue(label: "com.foxconn-strc.OwlFaceId.captureQueue")
            videoOutputs!.setSampleBufferDelegate(self, queue: queue)
            
            captureSession.commitConfiguration()
        }
        
        DispatchQueue(label: "prepare").async
        {
            do
                {
                    setupVision()
                    createCaptureSession()
                    try configureCaptureDevices()
                    try configureDeviceInputs()
                    try configurePhotoOutput()
                    try configureVideoDataOutput()
                }
            catch
            {
                DispatchQueue.main.async
                {
                    completionHandler(error)
                }
                
                return
            }
            
            DispatchQueue.main.async
            {
                completionHandler(nil)
            }
        }
    }
}

extension UIImage {
    func rotate(radians: Float) -> UIImage? {
        var newSize = CGRect(origin: CGPoint.zero, size: self.size).applying(CGAffineTransform(rotationAngle: CGFloat(radians))).size
        // Trim off the extremely small float value to prevent core graphics from rounding it up
        newSize.width = floor(newSize.width)
        newSize.height = floor(newSize.height)
        
        UIGraphicsBeginImageContextWithOptions(newSize, false, self.scale)
        let context = UIGraphicsGetCurrentContext()!
        
        // Move origin to middle
        context.translateBy(x: newSize.width/2, y: newSize.height/2)
        // Rotate around middle
        context.rotate(by: CGFloat(radians))
        // Draw the image at its center
        self.draw(in: CGRect(x: -self.size.width/2, y: -self.size.height/2, width: self.size.width, height: self.size.height))
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return newImage
    }
}
