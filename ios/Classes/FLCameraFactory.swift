//
//  FLCameraFactory.swift
//  owl_detection_camera
//
//  Created by Josh on 2022/3/18.
//

import Foundation
import Flutter
import UIKit
import AVFoundation
import SwiftUI

class FLCameraFactory: NSObject, FlutterPlatformViewFactory
{
    private var messenger: FlutterBinaryMessenger
    private var mSwiftFlutterOwlCameraPlugin: SwiftOwlDetectionCameraPlugin
    
    init(messenger: FlutterBinaryMessenger,aSwiftFlutterOwlCameraPlugin: SwiftOwlDetectionCameraPlugin)
    {
        self.messenger = messenger
        self.mSwiftFlutterOwlCameraPlugin = aSwiftFlutterOwlCameraPlugin;
        super.init()
    }
    
    func create(withFrame frame: CGRect,viewIdentifier viewId: Int64,arguments args: Any?) -> FlutterPlatformView
    {
        let flCameraView = FLCameraView(frame: frame,viewIdentifier: viewId,arguments: args,binaryMessenger: messenger)
        flCameraView.setSwiftFlutterOwlCameraPlugin(aSwiftFlutterOwlCameraPlugin: mSwiftFlutterOwlCameraPlugin)
        return flCameraView
    }
}

class FLCameraView: NSObject, FlutterPlatformView
{
    private var _view: UIView
    private var mSwiftFlutterOwlCameraPlugin: SwiftOwlDetectionCameraPlugin?;
    init(frame: CGRect,viewIdentifier viewId: Int64,arguments args: Any?,binaryMessenger messenger: FlutterBinaryMessenger?)
    {
        _view = UIView()
        //   var parent = UIViewController()
        
        //   let childView = UIHostingController(rootView: CameraSwiftUIView())
        //   _view = childView.view;
        
        super.init()
    }
    
    func setSwiftFlutterOwlCameraPlugin(aSwiftFlutterOwlCameraPlugin: SwiftOwlDetectionCameraPlugin)
    {
        mSwiftFlutterOwlCameraPlugin = aSwiftFlutterOwlCameraPlugin;
        // iOS views can be created here
        createNativeView(view: _view)
    }
    
    func view() -> UIView
    {
        //系統透過這邊所回傳的view來顯示原生自定義的View這邊的_view就是FlutterPlatformView所要的畫面
        return _view
    }
    
    //create view
    func createNativeView(view _view: UIView)
    {
        //   let storyboard = UIStoryboard(name: "Native", bundle: nil)
        //   var controller: UIViewController = storyboard.instantiateViewController(withIdentifier: "CameraVC") as UIViewController
        
        //使用所產生的Native.storyboard,初始化VC為CameraCV
        let storyboard = UIStoryboard.init(name: "Native", bundle: Bundle.init(for: CameraCV.self))
        
        //對應到Native.storyboard內Storyboard的名稱為CameraStoryboard
        let controller = storyboard.instantiateViewController(withIdentifier: "CameraStoryboard") as! CameraCV
        
        controller.setSwiftFlutterOwlCameraPlugin(aSwiftFlutterOwlCameraPlugin: mSwiftFlutterOwlCameraPlugin!)
        
        _view.addSubview(controller.view)
        controller.view.frame = _view.bounds
        
        //add as a childviewcontroller
        //   addChildViewController(controller)
        
        //   _view.backgroundColor = UIColor.blue
        //   let nativeLabel = UILabel()
        //   nativeLabel.text = "Native text from iOS"
        //   nativeLabel.textColor = UIColor.white
        //   nativeLabel.textAlignment = .center
        //   nativeLabel.frame = CGRect(x: 0, y: 0, width: 180, height: 48.0)
        //   _view.addSubview(nativeLabel)
    }
}
