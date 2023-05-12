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

    public static String FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/OwlFaceIdCamera/";

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
     * @param filePath 檔案路徑
     * @param imgName  檔案名稱
     * @param compress 壓縮百分比
     * @return 返回儲存檔案
     * */
    public static File savePicture(Context aContext, Bitmap aBitmap, String filePath, String imgName, int compress)
    {
        Bitmap newBM = aBitmap;

        try
        {
            File dir = new File(filePath);

            if (!dir.exists())
                dir.mkdirs();

            File f = new File(filePath, imgName);

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            {
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


    public static byte[] bmp2Yuv(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;

        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        byte[] data = rgb2YCbCr420(pixels, width, height);
        return data;
    }

    public static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        byte[] yuv = new byte[len * 3 / 2]; //YUV格式，Y(亮度)=len，U、V = len/4
        int y, u, v;
        for (int i = 0; i < height; i++)
        {
            for (int j = 0; j < width; j++)
            {

                int rgb = pixels[i * width + j] & 0x00FFFFFF; //ignore ARGB的透明度
                //bgr -> rgb
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;

                //formula
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

                //Adjustment
                y = y < 16 ? 16 : (y > 255 ? 255 : y);
                u = u < 0 ? 0 : (u > 255 ? 255 : u);
                v = v < 0 ? 0 : (v > 255 ? 255 : v);

                //Copy
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1) + 0] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

}
