package com.nachex.ui.camera;

import android.content.Context;

import com.nachex.bankcardquality.BankcardQualityProcess;

/**
 * Created by ruanshimin on 2018/1/23.
 */

public class CameraNativeHelper {

    public interface CameraNativeInitCallback {
        /**
         * Load native library exception callback
         *
         * @param errorCode error code
         * @param e If loading so is abnormal, an exception object will be passed in
         */
        void onError(int errorCode, Throwable e);
    }

    public static void init(final Context ctx, final String token, final CameraNativeInitCallback cb) {
        CameraThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                int status;
                // Failed to load local so, the exception returns getloadSoException
                if (BankcardQualityProcess.getLoadSoException() != null) {
                    status = CameraView.NATIVE_SOLOAD_FAIL;
                    cb.onError(status, BankcardQualityProcess.getLoadSoException());
                    return;
                }
                // Authorization status
                int authStatus = BankcardQualityProcess.init(token);
                if (authStatus != 0) {
                    cb.onError(CameraView.NATIVE_AUTH_FAIL, null);
                    return;
                }

                // Load model state
                int initModelStatus = BankcardQualityProcess.getInstance()
                        .idcardQualityInit(ctx.getAssets(),
                                "models");

                if (initModelStatus != 0) {
                    cb.onError(CameraView.NATIVE_INIT_FAIL, null);
                }
            }
        });
    }

    public static void release() {
        BankcardQualityProcess.getInstance().releaseModel();
    }
}
