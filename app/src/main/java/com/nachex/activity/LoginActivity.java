package com.nachex.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.nachex.MainActivity;
import com.nachex.util.PrefManager;
import com.nachex.R;

import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class LoginActivity extends AppCompatActivity {
    Button btn_login;
    EditText edittext_mobile_number;
    EditText edittext_password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btn_login = findViewById(R.id.btn_login);
        edittext_mobile_number = findViewById(R.id.edittext_mobile_number);
        edittext_password = findViewById(R.id.edittext_password);

        if(getIntent()!=null){
            if(getIntent().getStringExtra("EXIT").equals("true")){
                //finish()
                startActivity(new Intent(LoginActivity.this, SplashActivity.class));
            }
        }

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getConnectionStatus(LoginActivity.this)) {
                    if (edittext_mobile_number.getText().toString().trim().isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Please enter mobile number", Toast.LENGTH_LONG).show();
                    } else if (edittext_mobile_number.getText().toString().length() <= 9) {
                        Toast.makeText(LoginActivity.this, "Please enter valid mobile number", Toast.LENGTH_LONG).show();
                    } else if (edittext_password.getText().toString().trim().isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Please enter password", Toast.LENGTH_LONG).show();
                    } else {
                        login();//7000951208//12345678
                    }
                } else{
                    Toast.makeText(LoginActivity.this, "Please check internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public Boolean getConnectionStatus(Context context) {
        NetworkInfo mNetworkInfoMobile;
        NetworkInfo mNetworkInfoWifi;
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        mNetworkInfoMobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        mNetworkInfoWifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        try {

            if (mNetworkInfoMobile.isConnected() || mNetworkInfoWifi.isConnected()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (mNetworkInfoWifi.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void login(){
        ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        builder.addFormDataPart("phone_number", edittext_mobile_number.getText().toString().trim());
        builder.addFormDataPart("password", edittext_password.getText().toString().trim());
        RequestBody body = builder.build();
        Request request = new Request.Builder()
                .url(getString(R.string.URL)+"auth/login")
                .method("POST", body)
                .addHeader("Accept", "application/json")
                //.addHeader("Authorization", "Bearer "+ PrefManager.getString(LoginActivity.this, PrefManager.USER_TOKEN))
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
                        String access_token = jsonObject11.optString("access_token");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                PrefManager.setString(LoginActivity.this, PrefManager.IS_USER_LOGGED_IN, "true");
                                PrefManager.setString(LoginActivity.this, PrefManager.USER_TOKEN, access_token);
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LoginActivity.this, message.toString(), Toast.LENGTH_LONG).show();
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