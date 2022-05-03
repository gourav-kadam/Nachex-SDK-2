package com.nachex.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.nachex.MainActivity;
import com.nachex.util.PrefManager;
import com.nachex.R;

import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PreviewActivity extends AppCompatActivity {
    android.app.AlertDialog alertDialog = null;
    Bitmap bitmap = null;
    String filePath = "";
    String status = "";
    ImageView imageview_photo, imageview_back;
    Button btn_submit, btn_retry;
    TextView statusFromRenderScript;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        filePath = getIntent().getStringExtra("filePath");
        statusFromRenderScript = findViewById(R.id.statusFromRenderScript);
        btn_submit = findViewById(R.id.btn_submit);
        imageview_photo = findViewById(R.id.imageview_photo);
        btn_retry = findViewById(R.id.btn_retry);
        imageview_back = findViewById(R.id.imageview_back);

        try {
            bitmap = BitmapFactory.decodeFile(filePath);
            imageview_photo.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //api for delect blur
                nachtextractapi(filePath);
            }
        });

        imageview_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btn_retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void imageUpload(){
        ProgressDialog progressDialog = new ProgressDialog(PreviewActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        File file = new File(filePath);
        if(file.exists()){
            final MediaType MEDIA_TYPE = MediaType.parse(filePath);
            builder.addFormDataPart("image",file.getName(), RequestBody.create(MEDIA_TYPE,file));
        }
        RequestBody body = builder.build();
        Request request = new Request.Builder()
                .url(getString(R.string.URL)+"imageUpload")
                .method("POST", body)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer "+ PrefManager.getString(PreviewActivity.this, PrefManager.USER_TOKEN))
                .build();
        OkHttpClient client = new OkHttpClient.Builder().build();
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println("responseresponseresponse=" + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });
                    final String data=response.body().string().trim();
                    System.out.println("responseresponseresponse=" + data);
                    JSONObject jsonObject11 = new JSONObject(data);
                    String status = jsonObject11.optString("status");
                    String message = jsonObject11.optString("message");
                    if(status.equalsIgnoreCase("true")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PreviewActivity.this, message+"", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!(PreviewActivity.this).isFinishing()) {
                                    //show dialog
                                    LayoutInflater li = LayoutInflater.from(PreviewActivity.this);
                                    final View promptsView = li.inflate(R.layout.logout_popup, null);
                                    final android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(PreviewActivity.this);
                                    alertDialogBuilder.setView(promptsView);
                                    alertDialogBuilder.setCancelable(false);

                                    TextView textview = (TextView) promptsView.findViewById(R.id.textview);
                                    TextView textview_cancel = (TextView) promptsView.findViewById(R.id.textview_cancel);
                                    TextView textview_logout = (TextView) promptsView.findViewById(R.id.textview_logout);

                                    textview.setText(message);
                                    textview_logout.setText("Exit");
                                    textview_cancel.setText("Retry");
                                    textview_logout.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            alertDialog.dismiss();
                                            startActivity(new Intent(PreviewActivity.this, MainActivity.class));
                                            finishAffinity();
                                        }
                                    });
                                    textview_cancel.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
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
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }

    public void nachtextractapi(String image_path){
        ProgressDialog progressDialog = new ProgressDialog(PreviewActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        File file = new File(image_path);
        if(file.exists()){
            final MediaType MEDIA_TYPE = MediaType.parse(image_path);
            builder.addFormDataPart("image",file.getName(), RequestBody.create(MEDIA_TYPE,file));
        }
        RequestBody body = builder.build();
        Request request = new Request.Builder()
                .url(getString(R.string.nachtextractapi))
                .method("POST", body)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", getString(R.string.token))
                .build();
        OkHttpClient client = new OkHttpClient.Builder().build();
        Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println("responseresponseresponse=" + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if (!(PreviewActivity.this).isFinishing()) {
                            //show dialog
                            LayoutInflater li = LayoutInflater.from(PreviewActivity.this);
                            final View promptsView = li.inflate(R.layout.logout_popup, null);
                            final android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(PreviewActivity.this);
                            alertDialogBuilder.setView(promptsView);
                            alertDialogBuilder.setCancelable(false);

                            TextView textview = (TextView) promptsView.findViewById(R.id.textview);
                            TextView textview_cancel = (TextView) promptsView.findViewById(R.id.textview_cancel);
                            TextView textview_logout = (TextView) promptsView.findViewById(R.id.textview_logout);

                            textview.setText("The server is under maintenance, please try again later");
                            textview_logout.setText("Exit");
                            textview_cancel.setText("Retry");
                            textview_logout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    alertDialog.dismiss();
                                    onBackPressed();
                                }
                            });
                            textview_cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    alertDialog.dismiss();
                                    onBackPressed();
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
            public void onResponse(Call call, okhttp3.Response response) {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });
                    final String data=response.body().string().trim();
                    System.out.println("responseresponseresponse=" + data);
                    JSONObject jsonObject11 = new JSONObject(data);
                    String status = jsonObject11.optString("status");
                    if(status.equalsIgnoreCase("P")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageUpload();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusFromRenderScript.setVisibility(View.VISIBLE);
                                if (status.equalsIgnoreCase("E")){
                                    statusFromRenderScript.setText("If any problem comes while processing in image, error flag will be raised");
                                } else if (status.equalsIgnoreCase("N")){
                                    statusFromRenderScript.setText("Uploaded document is not a kyc document");
                                } else{
                                    statusFromRenderScript.setText("The document is blur/blank, please try again");
                                }
                                if (!(PreviewActivity.this).isFinishing()) {
                                    //show dialog
                                    LayoutInflater li = LayoutInflater.from(PreviewActivity.this);
                                    final View promptsView = li.inflate(R.layout.logout_popup, null);
                                    final android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(PreviewActivity.this);
                                    alertDialogBuilder.setView(promptsView);
                                    alertDialogBuilder.setCancelable(false);

                                    TextView textview = (TextView) promptsView.findViewById(R.id.textview);
                                    TextView textview_cancel = (TextView) promptsView.findViewById(R.id.textview_cancel);
                                    TextView textview_logout = (TextView) promptsView.findViewById(R.id.textview_logout);


                                    if (status.equalsIgnoreCase("E")){
                                        textview.setText("If any problem comes while processing in image, error flag will be raised");
                                    } else if (status.equalsIgnoreCase("N")){
                                        textview.setText("Uploaded document is not a kyc document");
                                    } else{
                                        textview.setText("The document is blur/blank, please try again");
                                    }
                                    textview_logout.setText("Exit");
                                    textview_cancel.setText("Retry");
                                    textview_logout.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            alertDialog.dismiss();
                                            onBackPressed();
                                        }
                                    });
                                    textview_cancel.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            alertDialog.dismiss();
                                            onBackPressed();
                                        }
                                    });

                                    alertDialog = alertDialogBuilder.create();
                                    alertDialog.show();
                                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }
}