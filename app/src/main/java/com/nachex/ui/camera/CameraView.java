package com.nachex.ui.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;

import com.nachex.R;
import com.nachex.bankcardquality.BankcardQualityProcess;
import com.nachex.ui.util.DimensionUtil;
import com.nachex.ui.util.ImageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Responsible for the management of the camera. At the same time, the crop mask function is provided.
 */
public class CameraView extends FrameLayout {

    private int maskType;

    /**
     * Camera callback
     */
    interface OnTakePictureCallback {
        void onPictureTaken(Bitmap bitmap);
    }

    /**
     * Vertical orientation {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_PORTRAIT = 0;
    /**
     * Horizontal orientation {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_HORIZONTAL = 90;
    /**
     * Horizontal flip orientation {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_INVERT = 270;

    /**
     * Local model authorization, loaded successfully
     */
    public static final int NATIVE_AUTH_INIT_SUCCESS = 0;

    /**
     * Local model authorization, missing SO
     */
    public static final int NATIVE_SOLOAD_FAIL = 10;

    /**
     * Local model authorization, authorization failed, token exception
     */
    public static final int NATIVE_AUTH_FAIL = 11;

    /**
     * Local model authorization, model loading failed
     */
    public static final int NATIVE_INIT_FAIL = 12;


    /**
     * Has passed a local quality control scan
     */
    private final int SCAN_SUCCESS = 0;

    public void setInitNativeStatus(int initNativeStatus) {
        this.initNativeStatus = initNativeStatus;
    }

    /**
     * Local detection initialization, model load identification
     */
    private int initNativeStatus  = NATIVE_AUTH_INIT_SUCCESS;

    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_HORIZONTAL, ORIENTATION_INVERT})
    public @interface Orientation {

    }

    private CameraViewTakePictureCallback cameraViewTakePictureCallback = new CameraViewTakePictureCallback();

    private ICameraControl cameraControl;

    /**
     * Camera preview View
     */
    private View displayView;
    /**
     * Masks for ID cards, bank cards, etc.
     */
    private MaskView maskView;

    /**
     * Used to display backgrounds like "Please align the front of the ID card"
     */
    private ImageView hintView;

    /**
     * Used to display text such as "Please align the front of the ID card"
     */
    private TextView hintViewText;

    /**
     * Prompt copy container
     */
    private LinearLayout hintViewTextWrapper;

    /**
     * Is it a local quality control scan
     */
    private boolean isEnableScan;

    public void setEnableScan(boolean enableScan) {
        isEnableScan = enableScan;
    }

    /**
     * UI thread handler
     */
    Handler uiHandler = new Handler(Looper.getMainLooper());

    public ICameraControl getCameraControl() {
        return cameraControl;
    }

    public void setOrientation(@Orientation int orientation) {
        cameraControl.setDisplayOrientation(orientation);
    }
    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void start() {
        cameraControl.start();
        setKeepScreenOn(true);
    }

    public void stop() {
        cameraControl.stop();
        setKeepScreenOn(false);
    }

    public void takePicture(final File file, final OnTakePictureCallback callback) {
        cameraViewTakePictureCallback.file = file;
        cameraViewTakePictureCallback.callback = callback;
        cameraControl.takePicture(cameraViewTakePictureCallback);
    }

    private OnTakePictureCallback autoPictureCallback;

    public void setAutoPictureCallback(OnTakePictureCallback callback) {
        autoPictureCallback = callback;
    }

    public void setMaskType(@MaskView.MaskType int maskType, final Context ctx) {
        maskView.setMaskType(maskType);

        maskView.setVisibility(VISIBLE);
        hintView.setVisibility(VISIBLE);

        //int hintResourceId = R.drawable.bd_ocr_round_corner;
        int hintResourceId = 0;
        this.maskType = maskType;
        boolean isNeedSetImage = true;
        switch (maskType) {
            case MaskView.MASK_TYPE_ID_CARD_FRONT:
                hintResourceId = R.drawable.bg_round_corner;
                isNeedSetImage = false;
                break;
            case MaskView.MASK_TYPE_ID_CARD_BACK:
                isNeedSetImage = false;
                hintResourceId = R.drawable.bg_round_corner;
                break;
            case MaskView.MASK_TYPE_BANK_CARD:
                //hintResourceId = R.drawable.bd_ocr_hint_align_bank_card;
                break;
            case MaskView.MASK_TYPE_PASSPORT:
                hintView.setVisibility(INVISIBLE);
                break;
            case MaskView.MASK_TYPE_NONE:
            default:
                maskView.setVisibility(INVISIBLE);
                hintView.setVisibility(INVISIBLE);
                break;
        }

        if (isNeedSetImage) {
            hintView.setImageResource(hintResourceId);
            hintViewTextWrapper.setVisibility(INVISIBLE);
        }

        if (maskType == MaskView.MASK_TYPE_ID_CARD_FRONT && isEnableScan) {
            cameraControl.setDetectCallback(new ICameraControl.OnDetectPictureCallback() {
                @Override
                public int onDetect(byte[] data, int rotation) {
                    return detect(data, rotation);
                }
            });
        }

        if (maskType == MaskView.MASK_TYPE_ID_CARD_BACK && isEnableScan) {
            cameraControl.setDetectCallback(new ICameraControl.OnDetectPictureCallback() {
                @Override
                public int onDetect(byte[] data, int rotation) {
                    return detect(data, rotation);
                }
            });
        }
    }

    private int detect(byte[] data, final int rotation) {
        if (initNativeStatus != NATIVE_AUTH_INIT_SUCCESS) {
            showTipMessage(initNativeStatus);
            return 1;
        }
        // Scan successfully blocks redundant operations
        if (cameraControl.getAbortingScan().get()) {
            return 0;
        }

        Rect previewFrame = cameraControl.getPreviewFrame();

        if (maskView.getWidth() == 0 || maskView.getHeight() == 0
                || previewFrame.width() == 0 || previewFrame.height() == 0) {
            return 0;
        }

        // BitmapRegionDecoder doesn't load entire image into memory。
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int width = rotation % 180 == 0 ? decoder.getWidth() : decoder.getHeight();
        int height = rotation % 180 == 0 ? decoder.getHeight() : decoder.getWidth();

        Rect frameRect = maskView.getFrameRectExtend();

        int left =  width * frameRect.left / maskView.getWidth();
        int top = height * frameRect.top / maskView.getHeight();
        int right = width * frameRect.right / maskView.getWidth();
        int bottom = height * frameRect.bottom / maskView.getHeight();

        // taller than picture
        if (previewFrame.top < 0) {
            // Width alignment.
            int adjustedPreviewHeight = previewFrame.height() * getWidth() / previewFrame.width();
            int topInFrame = ((adjustedPreviewHeight - frameRect.height()) / 2)
                    * getWidth() / previewFrame.width();
            int bottomInFrame = ((adjustedPreviewHeight + frameRect.height()) / 2) * getWidth()
                    / previewFrame.width();

            // Proportionally projected into the photo.
            top = topInFrame * height / previewFrame.height();
            bottom = bottomInFrame * height / previewFrame.height();
        } else {
            // wider than picture
            if (previewFrame.left < 0) {
                // height alignment
                int adjustedPreviewWidth = previewFrame.width() * getHeight() / previewFrame.height();
                int leftInFrame = ((adjustedPreviewWidth - maskView.getFrameRect().width()) / 2) * getHeight()
                        / previewFrame.height();
                int rightInFrame = ((adjustedPreviewWidth + maskView.getFrameRect().width()) / 2) * getHeight()
                        / previewFrame.height();

                // Proportional projection into the photo。
                left = leftInFrame * width / previewFrame.width();
                right = rightInFrame * width / previewFrame.width();
            }
        }

        Rect region = new Rect();
        region.left = left;
        region.top = top;
        region.right = right;
        region.bottom = bottom;

        // 90 degree or 270 degree rotation
        if (rotation % 180 == 90) {
            int x = decoder.getWidth() / 2;
            int y = decoder.getHeight() / 2;

            int rotatedWidth = region.height();
            int rotated = region.width();

            // Calculate, the coordinates of the crop frame after rotation
            region.left = x - rotatedWidth / 2;
            region.top = y - rotated / 2;
            region.right = x + rotatedWidth / 2;
            region.bottom = y + rotated / 2;
            region.sort();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();

        // maximum image size。
        int maxPreviewImageSize = 2560;
        int size = Math.min(decoder.getWidth(), decoder.getHeight());
        size = Math.min(size, maxPreviewImageSize);

        options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
        options.inScaled = true;
        options.inDensity = Math.max(options.outWidth, options.outHeight);
        options.inTargetDensity = size;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = decoder.decodeRegion(region, options);
        if (rotation != 0) {
            // It can only be rotated after cropping. Is there any other better solution?
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            if (bitmap != rotatedBitmap) {
                // Sometimes createBitmap reuses objects
                bitmap.recycle();
            }
            bitmap = rotatedBitmap;
        }

        final int status;

        // Invoke local QC request
        switch (maskType) {
            case MaskView.MASK_TYPE_ID_CARD_FRONT:
                status = BankcardQualityProcess.getInstance().idcardQualityDetectionImg(bitmap, true);
                break;
            case MaskView.MASK_TYPE_ID_CARD_BACK:
                status = BankcardQualityProcess.getInstance().idcardQualityDetectionImg(bitmap, false);
                break;
            default:
                status = 1;
        }

        // When a scan processing thread is successfully called, prevent other threads from continuing to call local control code
        if (status == SCAN_SUCCESS) {
            // Scan successfully prevents multiple threads from calling back at the same time
            if (!cameraControl.getAbortingScan().compareAndSet(false, true)) {
                bitmap.recycle();
                return 0;
            }
            autoPictureCallback.onPictureTaken(bitmap);
        }

        showTipMessage(status);

        return status;
    }

    private void showTipMessage(final int status) {
        // Prompt tip text change
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (status == 0) {
                    hintViewText.setVisibility(View.INVISIBLE);
                } else if (!cameraControl.getAbortingScan().get()) {
                    hintViewText.setVisibility(View.VISIBLE);
                    hintViewText.setText(getScanMessage(status));
                }
            }
        });
    }

    private String getScanMessage(int status) {
        String message;
        switch (status) {
            case 0:
                message = "";
                break;
            case 2:
                message = "ID is blurry, please try again";
                break;
            case 3:
                message = "ID card is reflective, please try again";
                break;
            case 4:
                message = "Please reverse the ID card before and after identification";
                break;
            case 5:
                message = "Please hold the lens and ID";
                break;
            case 6:
                message = "Please keep the camera close to the ID card";
                break;
            case 7:
                message = "Please put the ID card completely in the viewfinder";
                break;
            case NATIVE_AUTH_FAIL:
                message = "Local QC authorization failed";
                break;
            case NATIVE_INIT_FAIL:
                message = "Failed to load local model";
                break;
            case NATIVE_SOLOAD_FAIL:
                message = "Local SO library failed to load";
                break;
            case 1:
            default:
                message = "Please put your ID card in the viewfinder";
        }


        return message;
    }

    private void init() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            cameraControl = new Camera2Control(getContext());
//        } else {
//
//        }
        cameraControl = new Camera1Control(getContext());

        displayView = cameraControl.getDisplayView();
        addView(displayView);

        maskView = new MaskView(getContext());
        addView(maskView);

        hintView = new ImageView(getContext());
        addView(hintView);

        hintViewTextWrapper = new LinearLayout(getContext());
        hintViewTextWrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                DimensionUtil.dpToPx(25));

        lp.gravity = Gravity.CENTER;
        hintViewText = new TextView(getContext());
        hintViewText.setBackgroundResource(R.drawable.bg_round_corner);
        hintViewText.setAlpha(0.5f);
        hintViewText.setPadding(DimensionUtil.dpToPx(10), 0, DimensionUtil.dpToPx(10), 0);
        hintViewTextWrapper.addView(hintViewText, lp);


        hintViewText.setGravity(Gravity.CENTER);
        hintViewText.setTextColor(Color.WHITE);
        hintViewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hintViewText.setText(getScanMessage(-1));


        addView(hintViewTextWrapper, lp);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        displayView.layout(left, 0, right, bottom - top);
        maskView.layout(left, 0, right, bottom - top);

        int hintViewWidth = DimensionUtil.dpToPx(250);
        int hintViewHeight = DimensionUtil.dpToPx(25);

        int hintViewLeft = (getWidth() - hintViewWidth) / 2;
        int hintViewTop = maskView.getFrameRect().bottom + DimensionUtil.dpToPx(16);

        hintViewTextWrapper.layout(hintViewLeft, hintViewTop,
                hintViewLeft + hintViewWidth, hintViewTop + hintViewHeight);

        hintView.layout(hintViewLeft, hintViewTop,
                hintViewLeft + hintViewWidth, hintViewTop + hintViewHeight);
    }

    /**
     * After the photo is taken. Cropping is required. Some mobile phones (such as Samsung) do not rotate the photo data, but write the rotation angle into the EXIF ​​information,
     * So it needs to be rotated.
     *
     * @param outputFile The file to write the photo to.
     * @param data Raw photo data.
     * @param rotation The rotation angle in the photo exif.
     *
     * @return cropped bitmap.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Bitmap crop(File outputFile, byte[] data, int rotation) {
        try {
            Rect previewFrame = cameraControl.getPreviewFrame();

            if (maskView.getWidth() == 0 || maskView.getHeight() == 0
                    || previewFrame.width() == 0 || previewFrame.height() == 0) {
                return null;
            }

            // BitmapRegionDecoder doesn't load entire image into memory。
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);



            int width = rotation % 180 == 0 ? decoder.getWidth() : decoder.getHeight();
            int height = rotation % 180 == 0 ? decoder.getHeight() : decoder.getWidth();

            Rect frameRect = maskView.getFrameRect();

            int left = width * frameRect.left / maskView.getWidth();
            int top = height * frameRect.top / maskView.getHeight();
            int right = width * frameRect.right / maskView.getWidth();
            int bottom = height * frameRect.bottom / maskView.getHeight();

            // taller than picture
            if (previewFrame.top < 0) {
                // Width alignment.
                int adjustedPreviewHeight = previewFrame.height() * getWidth() / previewFrame.width();
                int topInFrame = ((adjustedPreviewHeight - frameRect.height()) / 2)
                        * getWidth() / previewFrame.width();
                int bottomInFrame = ((adjustedPreviewHeight + frameRect.height()) / 2) * getWidth()
                        / previewFrame.width();

                // Proportional projection into the photo。
                top = topInFrame * height / previewFrame.height();
                bottom = bottomInFrame * height / previewFrame.height();
            } else {
                // wider than picture
                if (previewFrame.left < 0) {
                    // height alignment
                    int adjustedPreviewWidth = previewFrame.width() * getHeight() / previewFrame.height();
                    int leftInFrame = ((adjustedPreviewWidth - maskView.getFrameRect().width()) / 2) * getHeight()
                            / previewFrame.height();
                    int rightInFrame = ((adjustedPreviewWidth + maskView.getFrameRect().width()) / 2) * getHeight()
                            / previewFrame.height();

                    // Proportional projection into the photo。
                    left = leftInFrame * width / previewFrame.width();
                    right = rightInFrame * width / previewFrame.width();
                }
            }

            Rect region = new Rect();
            region.left = left;
            region.top = top;
            region.right = right;
            region.bottom = bottom;

            // 90 degree or 270 degree rotation
            if (rotation % 180 == 90) {
                int x = decoder.getWidth() / 2;
                int y = decoder.getHeight() / 2;

                int rotatedWidth = region.height();
                int rotated = region.width();

                // Calculate, the coordinates of the crop frame after rotation
                region.left = x - rotatedWidth / 2;
                region.top = y - rotated / 2;
                region.right = x + rotatedWidth / 2;
                region.bottom = y + rotated / 2;
                region.sort();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();

            // maximum image size。
            int maxPreviewImageSize = 2560;
            int size = Math.min(decoder.getWidth(), decoder.getHeight());
            size = Math.min(size, maxPreviewImageSize);

            options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
            options.inScaled = true;
            options.inDensity = Math.max(options.outWidth, options.outHeight);
            options.inTargetDensity = size;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = decoder.decodeRegion(region, options);

            if (rotation != 0) {
                // It can only be rotated after cropping. Is there any other better solution?
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                if (bitmap != rotatedBitmap) {
                    // Sometimes createBitmap reuses objects
                    bitmap.recycle();
                }
                bitmap = rotatedBitmap;
            }

            try {
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void release() {
        BankcardQualityProcess.getInstance().releaseModel();
    }

    private class CameraViewTakePictureCallback implements ICameraControl.OnTakePictureCallback {

        private File file;
        private OnTakePictureCallback callback;

        @Override
        public void onPictureTaken(final byte[] data) {
            CameraThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    final int rotation = ImageUtil.getOrientation(data);
                    Bitmap bitmap = crop(file, data, rotation);
                    callback.onPictureTaken(bitmap);
                }
            });
        }
    }
}
