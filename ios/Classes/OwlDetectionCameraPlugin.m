#import "OwlDetectionCameraPlugin.h"
#if __has_include(<owl_detection_camera/owl_detection_camera-Swift.h>)
#import <owl_detection_camera/owl_detection_camera-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "owl_detection_camera-Swift.h"
#endif

@implementation OwlDetectionCameraPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftOwlDetectionCameraPlugin registerWithRegistrar:registrar];
}
@end
