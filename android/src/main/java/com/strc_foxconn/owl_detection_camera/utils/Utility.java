package com.strc_foxconn.owl_detection_camera.utils;

import android.app.Activity;
import android.content.Intent;

import android.graphics.Bitmap;

import android.net.Uri;

import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;
//import com.google.mlkit.vision.common.InputImage;
//import com.google.mlkit.vision.face.Face;
//import com.google.mlkit.vision.face.FaceDetector;

import com.strc_foxconn.owl_detection_camera.Defines;

import java.io.File;
import java.util.List;

public class Utility
{
//    public static void detectFaceOrNot(Handler aHandler, FaceDetector mDetector, InputImage image, final Bitmap aBitmap)
//    {
//        Task<List<Face>> result = mDetector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>()
//        {
//            @Override
//            public void onSuccess(List<Face> faces)
//            {
//                aBitmap.recycle();
//                Message msg = new Message();
//                msg.arg1 = faces.size();
//                msg.what = R.id.start_detect_face;
//                aHandler.sendMessage(msg);
//            }
//        }).addOnFailureListener(new OnFailureListener()
//        {
//            @Override
//            public void onFailure(@NonNull Exception e)
//            {
//                Message msg = new Message();
//                msg.arg1 = 0;
//                msg.obj = aBitmap;
//                msg.what = R.id.start_detect_face;
//                aHandler.sendMessage(msg);
//            }
//        });
//    }

    public static boolean checkSystemWriteSettings(Activity aActivity)
    {
        boolean hasPermission = false;
        if(!Settings.System.canWrite(aActivity))
        {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + aActivity.getPackageName()));
            aActivity.startActivityForResult(intent,1002);
        }
        else{
            hasPermission = true;
        }
        return hasPermission;
    }

    public static void deleteFolder()
    {
        File dir = new File(Defines.FILE_PATH);
        deleteAll(dir);
    }

    public static boolean deleteAll(File file)
    {
        if (file == null || !file.exists()) return false;

        boolean success = true;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        success &= deleteAll(f);
                    }
                    if (!f.delete()) {
                        Log.w("deleteAll", "Failed to delete " + f);
                        success = false;
                    }
                }
            } else {
                if (!file.delete()) {
                    Log.w("deleteAll", "Failed to delete " + file);
                    success = false;
                }
            }
        } else {
            if (!file.delete()) {
                Log.w("deleteAll", "Failed to delete " + file);
                success = false;
            }
        }
        return success;
    }
}
