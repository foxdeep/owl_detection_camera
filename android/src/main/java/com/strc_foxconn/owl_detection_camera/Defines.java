package com.strc_foxconn.owl_detection_camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

public class Defines
{
    public static final int DETECTION_HINT_FIT_CENTER = 0;
    public static final int DETECTION_HINT_FORWARD = 1;
    public static final int DETECTION_HINT_BACKWARD = 2;

    public static final int METRICS_XDPI_SRANDARD = 404;

    public static final int SYSTEM_WRITE_SETTING_REQUEST_CODE = 1002;

    /**
     * 以此Version來區分人臉辨識機器版本，或一般裝置版本。
     *
     * 由於配合人臉辨識裝置特殊相機配置的原因，故需要此參數作為判別。
     * */
    public static int sVersion = VERSION.NORMAL;

    public static float sFACE_SCALE=1;
    public static float sFACE_SCALE_Y=1;

    public static final String FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/OwlFaceIdCamera/";

    public static class CAMERA_ACTION
    {
        public static final int NONE = 0;
        public static final int IS_CAPTURING_FROM_FACE = 1;
        public static final int IS_CAPTURING_FROM_QRCODE = 2;
    }

    public static class VERSION
    {
        public static final int NORMAL = 0;
        public static final int JWS_DEVICE = 1;
    }

    /**
     * @param filePath 文件保存路徑
     * @param imgName  文件名
     * @param compress 壓縮百分比
     * @return 返回保存圖片文件
     * */
    public static File savePicture(Context aContext, Bitmap aBitmap, String filePath, String imgName, int compress)
    {
//        if (!imgName.contains(".png"))
//        {
//            imgName += ".png";
//        }
        Bitmap newBM = aBitmap;

        try
        {
            File f;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                String path = aContext.getExternalFilesDir(null).getPath();
                f = new File(path, imgName);
            }
            else{
                File dir = new File(filePath);
                if (!dir.exists())
                {
                    dir.mkdirs();
                }
                f = new File(filePath, imgName);
                if (!f.exists())
                {
                    f.createNewFile();
                } else
                {
                    f.delete();
                }
            }

            FileOutputStream out = new FileOutputStream(f);

            if (compress >= 1 && compress <= 100)
                newBM.compress(Bitmap.CompressFormat.JPEG, compress, out);
            else
            {
                newBM.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }

            out.close();

            newBM.recycle();
            newBM = null;
            return f;
        } catch (Exception e)
        {
            return null;
        }

    }

}
