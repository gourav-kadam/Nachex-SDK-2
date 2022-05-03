package com.nachex.ui.camera;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.IntDef;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Android 5.0 Camera API has changed a lot. Some classes block API changes. The operation and function of the camera are abstracted away.
 */
public interface ICameraControl {

    /**
     * Flash off {@link #setFlashMode(int)}
     */
    int FLASH_MODE_OFF = 0;
    /**
     * Flash on {@link #setFlashMode(int)}
     */
    int FLASH_MODE_TORCH = 1;
    /**
     * Flash auto {@link #setFlashMode(int)}
     */
    int FLASH_MODE_AUTO = 2;

    @IntDef({FLASH_MODE_TORCH, FLASH_MODE_OFF, FLASH_MODE_AUTO})
    @interface FlashMode {

    }

    /**
     * Camera callback.
     */
    interface OnTakePictureCallback {
        void onPictureTaken(byte[] data);
    }

    /**
     * Set the local quality control callback, if not set, it is considered that the local quality control code is not scanned and called.
     */
    void setDetectCallback(OnDetectPictureCallback callback);

    /**
     * preview callback
     */
    interface OnDetectPictureCallback {
        int onDetect(byte[] data, int rotation);
    }

    /**
     * turn on a camera.
     */
    void start();

    /**
     * Turn off the camera
     */
    void stop();

    void pause();

    void resume();

    /**
     * The preview view corresponding to the camera.
     * @return preview view
     */
    View getDisplayView();

    /**
     * The preview seen may not be the full picture of the photo. Returns the full picture of the preview view.
     * @return preview view frame;
     */
    Rect getPreviewFrame();

    /**
     * Photograph. The result is fetched in the callback.
     * @param callback Photo result callback
     */
    void takePicture(OnTakePictureCallback callback);

    /**
     * Set the permission callback, when the phone does not have permission to take pictures, it can be obtained in the callback.
     * @param callback permission callback
     */
    void setPermissionCallback(PermissionCallback callback);

    /**
     * set horizontal orientation
     * @param displayOrientation parameter value see {@link CameraView.Orientation}
     */
    void setDisplayOrientation(@CameraView.Orientation int displayOrientation);

    /**
     * When the camera permission is obtained, call some functions to continue.
     */
    void refreshPermission();

    /**
     * Get has been scanned successfully, processing
     */
    AtomicBoolean getAbortingScan();

    /**
     * Set flash status.
     * @param flashMode {@link #FLASH_MODE_TORCH,#FLASH_MODE_OFF,#FLASH_MODE_AUTO}
     */
    void setFlashMode(@FlashMode int flashMode);

    /**
     * Get the current flash status
     * @return current flash state see {@link #setFlashMode(int)}
     */
    @FlashMode
    int getFlashMode();
}
