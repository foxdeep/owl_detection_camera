//
//  Utility.swift
//  owl_detection_camera
//
//  Created by Josh on 2022/3/18.
//

import Foundation
class Utility
{
    static var sLastTimePath:URL?;
    
    static func showToast(controller: UIViewController, message : String, seconds: Double)
    {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.view.backgroundColor = UIColor.black
        alert.view.alpha = 0.5
        alert.view.layer.cornerRadius = 15
        
        controller.present(alert, animated: true)
        
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + seconds)
        {
            alert.dismiss(animated: true)
        }
    }
    
    static func save(image: UIImage) -> [String]
    {
        let uuid = UUID().uuidString
        let fileName = uuid+".jpg";
        
        let fileURLss = documentsUrl.appendingPathComponent(fileName)
        let scaleImage = image.scaleImage(scaleSize: 0.3);
        _ = NSHomeDirectory() + "/tmp"
        _ = NSTemporaryDirectory()
        
        //        let filePath = NSTemporaryDirectory() + fileName
        
        if let imageData = scaleImage.jpegData(compressionQuality:0.8)
        {
            try? imageData.write(to: fileURLss /*, options: .atomic*/)
            return ["success",fileName,fileURLss.path]
        }
        
        print("Error saving image")
        return ["fail",fileName,fileURLss.path]
    }
    
    static var documentsUrl: URL
    {
        let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return url
    }
    
    static func imageFromSampleBuffer(sampleBuffer : CMSampleBuffer) -> UIImage
    {
        // Get a CMSampleBuffer's Core Video image buffer for the media data
        let  imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
        // Lock the base address of the pixel buffer
        CVPixelBufferLockBaseAddress(imageBuffer!, CVPixelBufferLockFlags.readOnly);
        
        // Get the number of bytes per row for the pixel buffer
        let baseAddress = CVPixelBufferGetBaseAddress(imageBuffer!);
        
        // Get the number of bytes per row for the pixel buffer
        let bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer!);
        // Get the pixel buffer width and height
        let width = CVPixelBufferGetWidth(imageBuffer!);
        let height = CVPixelBufferGetHeight(imageBuffer!);
        
        // Create a device-dependent RGB color space
        let colorSpace = CGColorSpaceCreateDeviceRGB();
        
        // Create a bitmap graphics context with the sample buffer data
        var bitmapInfo: UInt32 = CGBitmapInfo.byteOrder32Little.rawValue
        bitmapInfo |= CGImageAlphaInfo.premultipliedFirst.rawValue & CGBitmapInfo.alphaInfoMask.rawValue
        //let bitmapInfo: UInt32 = CGBitmapInfo.alphaInfoMask.rawValue
        let context = CGContext.init(data: baseAddress, width: width, height: height, bitsPerComponent: 8, bytesPerRow: bytesPerRow, space: colorSpace, bitmapInfo: bitmapInfo)
        // Create a Quartz image from the pixel data in the bitmap graphics context
        let quartzImage = context?.makeImage();
        // Unlock the pixel buffer
        CVPixelBufferUnlockBaseAddress(imageBuffer!, CVPixelBufferLockFlags.readOnly);
        
        // Create an image object from the Quartz image.
        let image = UIImage.init(cgImage: quartzImage!);
        
        return (image);
    }
}

extension UIImage
{
    /**
     *  重設圖片大小
     */
    func reSizeImage(reSize:CGSize)->UIImage
    {
        //UIGraphicsBeginImageContext(reSize);
        UIGraphicsBeginImageContextWithOptions(reSize,false,UIScreen.main.scale);
        self.draw(in: CGRect(x: 0, y: 0, width: reSize.width, height: reSize.height));
        let reSizeImage:UIImage = UIGraphicsGetImageFromCurrentImageContext()!;
        UIGraphicsEndImageContext();
        return reSizeImage;
    }
    
    /**
     *  等比例縮放
     */
    func scaleImage(scaleSize:CGFloat)->UIImage
    {
        let reSize = CGSize(width: self.size.width * scaleSize, height: self.size.height * scaleSize)
        return reSizeImage(reSize: reSize)
    }
    
    func cropImage1(_ image: UIImage,_ rect: CGRect) -> UIImage {
        let cgImage = image.cgImage! // better to write "guard" in realm app
        let croppedCGImage = cgImage.cropping(to: rect)
        return UIImage(cgImage: croppedCGImage!)
    }
}
