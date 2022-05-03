package com.nachex;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.baidu.ocr.sdk.OCR;
import com.nachex.activity.LoginActivity;
import com.nachex.activity.PreviewActivity;
import com.nachex.ui.camera.CameraActivity;
import com.nachex.ui.camera.CameraNativeHelper;
import com.nachex.util.FileUtil;
import com.nachex.util.PrefManager;

public class MainActivity extends AppCompatActivity {
    android.app.AlertDialog alertDialog;
    boolean doubleBackToExitPressedOnce = false;
    private static final int REQUEST_CODE_CAMERA = 102;
    private TextView mContent;
    private TextView textview_app_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textview_app_name = (TextView) findViewById(R.id.textview_app_name);
        mContent = (TextView) findViewById(R.id.content);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;

            textview_app_name.setText(getString(R.string.app_name)+" ("+version+")");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Camera
        findViewById(R.id.credit_card_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH, FileUtil.getSaveFile(getApplication()).getAbsolutePath());
                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, CameraActivity.CONTENT_TYPE_BANK_CARD);
                startActivityForResult(intent, REQUEST_CODE_CAMERA);*/

                final Intent it = new Intent(android.content.Intent.ACTION_VIEW);
                it.setClassName(getApplicationContext(), "com.nachex.activity.SplashActivity");
                startActivity(it);
            }
        });

        findViewById(R.id.btnLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!((Activity) MainActivity.this).isFinishing()) {
                    //show dialog
                    LayoutInflater li = LayoutInflater.from(MainActivity.this);
                    final View promptsView = li.inflate(R.layout.logout_popup, null);
                    final android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
                    alertDialogBuilder.setView(promptsView);
                    alertDialogBuilder.setCancelable(false);

                    TextView textview_cancel = promptsView.findViewById(R.id.textview_cancel);
                    TextView textview_logout = promptsView.findViewById(R.id.textview_logout);
                    textview_logout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog.dismiss();
                            PrefManager.setString(MainActivity.this, PrefManager.IS_USER_LOGGED_IN, "");
                            PrefManager.setString(MainActivity.this, PrefManager.USER_TOKEN, "");
                            startActivity(new Intent(MainActivity.this, LoginActivity.class).putExtra("EXIT", "true"));
                            finish();
                            //onBackPressed();
                        }
                    });

                    textview_cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog.dismiss();
                        }
                    });

                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("onActivityResult="+requestCode);
        if (requestCode == REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String contentType = data.getStringExtra(CameraActivity.KEY_CONTENT_TYPE);
                String filePath = FileUtil.getSaveFile(getApplicationContext()).getAbsolutePath();
                if (!TextUtils.isEmpty(contentType)) {
                    startActivity(new Intent(MainActivity.this, PreviewActivity.class).putExtra("filePath", filePath));
                    System.out.println("onResponse="+filePath);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        try{
            CameraNativeHelper.release();
            // free memory resources
            OCR.getInstance(this).release();
            super.onDestroy();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }
}
