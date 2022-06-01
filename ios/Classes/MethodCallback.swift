//
//  MethodCallback.swift
//  owl_detection_camera
//
//  Created by Josh on 2022/3/18.
//

import Foundation
public class MethodCallback
{
    var onStopFaceDetection: ()->()
    var onStartFaceDetection: ()->()
    var onScreenBright: (Int)->()
    var onSetDetectionMode: (Int)->()
    var onDisableHint: (Bool)->()
    var onSetFaceDetectionHintText:(String,String,String)->()
    
    //初始化Callback時，要求傳入兩個func，分別是1. (_ Response: Response)->()，2.(_ error: String)->()
    init(_ aOnStopFaceDetection: @escaping ()->(),_ aOnStartFaceDetection: @escaping ()->(),_ aOnScreenBright: @escaping (Int)->(),_ aOnSetDetectionMode: @escaping (Int)->(),_ aOnDisableHint: @escaping (Bool)->(),_ aOnSetFaceDetectionHintText: @escaping (String,String,String)->())
    {
        onStopFaceDetection = aOnStopFaceDetection;
        onStartFaceDetection = aOnStartFaceDetection;
        onScreenBright = aOnScreenBright;
        onSetDetectionMode = aOnSetDetectionMode;
        onDisableHint = aOnDisableHint;
        onSetFaceDetectionHintText = aOnSetFaceDetectionHintText;
    }
}
