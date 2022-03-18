package com.strc_foxconn.owl_detection_camera.utils;


import static com.strc_foxconn.owl_detection_camera.CameraView.sRealWindowHeight;

import android.content.Context;
import android.graphics.Typeface;

import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.strc_foxconn.owl_detection_camera.R;

public class ToastUtils {

    public static void showToast(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.setText(text);
        toast.show();
    }

    public static void showToastForFaceHint(Context aContext, String aMessage)
    {
        Toast toast = new Toast(aContext);
        TextView viewToast = new TextView(aContext);
        viewToast.setBackground(aContext.getResources().getDrawable(R.drawable.toast_background));
        viewToast.setTextColor(aContext.getResources().getColor(R.color.white));
        viewToast.setTypeface(Typeface.DEFAULT_BOLD);
        viewToast.setTextSize(aContext.getResources().getDimension(R.dimen.face_detection_hint_textview));
        viewToast.setText(aMessage);
        viewToast.setPadding(15, 10, 15, 10);
        toast.setView(viewToast);
        toast.setGravity(Gravity.BOTTOM,0,(sRealWindowHeight/12)*2);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }
}
