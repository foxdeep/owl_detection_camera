//
//  Define.swift
//  owl_detection_camera
//
//  Created by Josh on 2022/3/18.
//

import Foundation
class Define
{
    static var FRONT_CAMERA: Int = 0;
    static var REAR_CAMERA: Int = 1;
    
    static let SCREEN_BRIGHT = 0;
    static let SCREEN_DARK = 1;
    
    static let DETECTION_HINT_FIT_CENTER = 0;
    static let DETECTION_HINT_FORWARD = 1;
    static let DETECTION_HINT_BACKWARD = 2;
    
    static let flutter_owl_camera_plugin = "owl_detection_camera";
    
    static let METHOD_CHANNEL_FACE_FRAME_SIZE="face_frame_size";
    
    static let METHOD_STOP_DETECTION_FACE="stop_detection_face";
    static let METHOD_START_DETECTION_FACE="start_detection_face";
    
    static let METHOD_ID_SCREEN_BRIGHT = "screen_bright";
    static let METHOD_ID_FACE_DETECT_HINT = "face_detect_hint";
    
    //invoke meant from native to flutter.
    static let INVOKE_ID_IMAGE_PATH_NAME = "image_path_name_callback";
    static let INVOKE_ID_FACE_RECTANGLE = "face_rectangle_callback";
    static let INVOKE_ID_QRCODE_TEXT = "qrcode_text_callback";
}
