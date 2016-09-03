package com.kit.qrcode.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.kit.utils.MathExtend;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * 作者: 陈涛(1076559197@qq.com)
 * <p>
 * 时间: 2014年5月9日 下午12:22:12
 * <p>
 * 版本: V_1.0.0
 * <p>
 * 描述: 相机参数配置
 */
final class CameraConfigurationManager {

    private static final String TAG = CameraConfigurationManager.class
            .getSimpleName();

    private static final int TEN_DESIRED_ZOOM = 27;
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final int MIN_PREVIEW_PIXELS = 854 * 480;

    private static final int MIN_PREVIEW_WIDTH = 854;
    private static final int MIN_PREVIEW_HEIGHT = 480;


    private static final int MAX_PREVIEW_PIXELS = 1920 * 1080;

    private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
    private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;
    private static final double MAX_ASPECT_DISTORTION = 1.5;

    private final Context context;
    private Point screenResolution;
    private Point cameraResolution;
    private int previewFormat;
    private String previewFormatString;

    private static Display display;

    static ArrayList<Point> BestPreviewSizeValue = new ArrayList<Point>();
    public static boolean useCustomOptimization = true;

    CameraConfigurationManager(Context context) {
        this.context = context;
    }

    @SuppressWarnings("deprecation")
    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        previewFormat = parameters.getPreviewFormat();
        previewFormatString = parameters.get("preview-format");
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = manager.getDefaultDisplay();
        screenResolution = new Point(display.getWidth(), display.getHeight());

        Point screenResolutionForCamera = new Point();
        screenResolutionForCamera.x = screenResolution.x;
        screenResolutionForCamera.y = screenResolution.y;

        if (screenResolution.x < screenResolution.y) {
            screenResolutionForCamera.x = screenResolution.y;
            screenResolutionForCamera.y = screenResolution.x;
        }
        cameraResolution = getCameraResolution(parameters,
                screenResolutionForCamera);

        System.out.println("cameraResolution:" + cameraResolution.x + "  "
                + cameraResolution.y);
    }

    void setDesiredCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        setFlash(parameters);
        setZoom(parameters);

        camera.setDisplayOrientation(90);
        camera.setParameters(parameters);
    }

    Point getCameraResolution() {
        return cameraResolution;
    }

    Point getScreenResolution() {
        return screenResolution;
    }

    int getPreviewFormat() {
        return previewFormat;
    }

    String getPreviewFormatString() {
        return previewFormatString;
    }

    private static Point getCameraResolution(Camera.Parameters parameters,
                                             Point screenResolution) {

        String previewSizeValueString = parameters.get("preview-size-values");

        if (previewSizeValueString == null) {
            previewSizeValueString = parameters.get("preview-size-value");
        }

        if (previewSizeValueString == null && parameters.getPreviewSize() != null) {
            previewSizeValueString = parameters.getPreviewSize().width
                    + "x"
                    + parameters.getPreviewSize().height;
        }

        Point cameraResolution = null;
        if (useCustomOptimization) {
            if (previewSizeValueString != null) {
                cameraResolution = findBestPreviewSizeValue(
                        previewSizeValueString, screenResolution);
            }

        } else {

            if (previewSizeValueString != null) {
                cameraResolution = findBestPreviewSizeValue2(
                        previewSizeValueString, screenResolution);
            }

        }
        if (cameraResolution == null) {
            cameraResolution = new Point((screenResolution.x >> 3) << 3,
                    (screenResolution.y >> 3) << 3);
        }
        return cameraResolution;
    }

    private static Point findBestPreviewSizeValue2(
            CharSequence previewSizeValueString, Point screenResolution) {
        int bestX = 0;
        int bestY = 0;
        int diff = Integer.MAX_VALUE;
        for (String previewSize : COMMA_PATTERN.split(previewSizeValueString)) {

            previewSize = previewSize.trim();
            int dimPosition = previewSize.indexOf('x');
            if (dimPosition < 0) {
                continue;
            }

            int newX;
            int newY;
            try {
                newX = Integer.parseInt(previewSize.substring(0, dimPosition));
                newY = Integer.parseInt(previewSize.substring(dimPosition + 1));
            } catch (NumberFormatException nfe) {
                continue;
            }

            int newDiff = Math.abs(newX - screenResolution.x)
                    + Math.abs(newY - screenResolution.y);
            if (newDiff == 0) {
                bestX = newX;
                bestY = newY;
                break;
            } else if (newDiff < diff) {
                bestX = newX;
                bestY = newY;
                diff = newDiff;
            }

        }

        if (bestX > 0 && bestY > 0) {
            return new Point(bestX, bestY);
        }
        return null;
    }

    private static Point findBestPreviewSizeValue(
            CharSequence previewSizeValueString, Point screenResolution) {
        int realWidth;
        int realHeight;
        double screenAspectRatio = (double) screenResolution.x
                / (double) screenResolution.y;
        for (String previewSize : COMMA_PATTERN.split(previewSizeValueString)) {
            previewSize = previewSize.trim();
            int dimPosition = previewSize.indexOf('x');
            if (dimPosition < 0) {
                Log.w(TAG, "Bad preview-size: " + previewSize);
                continue;
            }
            realWidth = Integer.parseInt(previewSize.substring(0, dimPosition));
            realHeight = Integer.parseInt(previewSize
                    .substring(dimPosition + 1));
            Point point = new Point(realWidth, realHeight);
            if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                continue;
            }
            if (realWidth * realHeight > MAX_PREVIEW_PIXELS) {
                continue;
            }
            if (realWidth == screenResolution.x
                    && realHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                return exactPoint;
            }
            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight
                    : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth
                    : realHeight;
            double aspectRatio = (double) maybeFlippedWidth
                    / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue;
            }

            if (realWidth <= screenResolution.x) {
                System.out.println("###############BestPreviewSizeValue point:"
                        + point.x + "," + point.y);
                BestPreviewSizeValue.add(point);
            }
        }

        if (!BestPreviewSizeValue.isEmpty()) {

            double bili = MathExtend.divide(display.getWidth(),
                    display.getHeight(), 2);
            Point largestPreview = null;
            // 为了成比例，屏幕不压缩
            for (Point pointVlaue : BestPreviewSizeValue) {

                double bili2 = MathExtend.divide(pointVlaue.y, pointVlaue.x, 2);
                System.out.println("bili2:" + bili2 + "  bili:" + bili);
                if (bili2 == bili) {
                    largestPreview = pointVlaue;
                    break;
                }

            }
            if (largestPreview == null) {
                largestPreview = BestPreviewSizeValue.get(0);

            }

            Point largestSize = new Point(largestPreview.x, largestPreview.y);

            System.out.println("###############largestSize point:"
                    + largestSize.x + "," + largestSize.y);
            return largestSize;
        }

        return null;
    }

    private static int findBestMotZoomValue(CharSequence stringValues,
                                            int tenDesiredZoom) {
        int tenBestValue = 0;
        for (String stringValue : COMMA_PATTERN.split(stringValues)) {
            stringValue = stringValue.trim();
            double value;
            try {
                value = Double.parseDouble(stringValue);
            } catch (NumberFormatException nfe) {
                return tenDesiredZoom;
            }
            int tenValue = (int) (10.0 * value);
            if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom
                    - tenBestValue)) {
                tenBestValue = tenValue;
            }
        }
        return tenBestValue;
    }

    private void setFlash(Camera.Parameters parameters) {
        if (Build.MODEL.contains("Behold II") && CameraManager.SDK_INT == 3) { // 3
            parameters.set("flash-value", 1);
        } else {
            parameters.set("flash-value", 2);
        }
        parameters.set("flash-mode", "off");
    }

    private void setZoom(Camera.Parameters parameters) {

        String zoomSupportedString = parameters.get("zoom-supported");
        if (zoomSupportedString != null
                && !Boolean.parseBoolean(zoomSupportedString)) {
            return;
        }

        int tenDesiredZoom = TEN_DESIRED_ZOOM;

        String maxZoomString = parameters.get("max-zoom");
        if (maxZoomString != null) {
            try {
                int tenMaxZoom = (int) (10.0 * Double
                        .parseDouble(maxZoomString));
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "Bad max-zoom: " + maxZoomString);
            }
        }

        String takingPictureZoomMaxString = parameters
                .get("taking-picture-zoom-max");
        if (takingPictureZoomMaxString != null) {
            try {
                int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "Bad taking-picture-zoom-max: "
                        + takingPictureZoomMaxString);
            }
        }

        String motZoomValuesString = parameters.get("mot-zoom-values");
        if (motZoomValuesString != null) {
            tenDesiredZoom = findBestMotZoomValue(motZoomValuesString,
                    tenDesiredZoom);
        }

        String motZoomStepString = parameters.get("mot-zoom-step");
        if (motZoomStepString != null) {
            try {
                double motZoomStep = Double.parseDouble(motZoomStepString
                        .trim());
                int tenZoomStep = (int) (10.0 * motZoomStep);
                if (tenZoomStep > 1) {
                    tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
                }
            } catch (NumberFormatException nfe) {
                // continue
            }
        }

        // Set zoom. This helps encourage the user to pull back.
        // Some devices like the Behold have a zoom parameter
        if (maxZoomString != null || motZoomValuesString != null) {
            parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
        }

        // Most devices, like the Hero, appear to expose this zoom parameter.
        // It takes on values like "27" which appears to mean 2.7x zoom
        if (takingPictureZoomMaxString != null) {
            parameters.set("taking-picture-zoom", tenDesiredZoom);
        }
    }

}
