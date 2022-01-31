package fr.inria.tyrex.senslogs.model.sensors;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.log.Log;

import static android.media.CamcorderProfile.QUALITY_1080P;
import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_720P;
import static android.media.CamcorderProfile.QUALITY_CIF;

/**
 * Video Recorder
 *
 * @author Mark - Initial class here: https://stackoverflow.com/questions/37767511/camera2-video-recording-without-preview-on-android-mp4-output-file-not-fully-pl
 * @author Thibaud - Keep the same process but fit for customization
 */
@androidx.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraRecorder extends Sensor {

    transient private final static String CAMERA_THREAD = "camera";


    /*
     * Threading for Camera
     */
    transient private HandlerThread mBackgroundThread;
    transient private Handler mBackgroundHandler;


    /*
     * Core attributes: recorder, manager, device and session
     */
    transient private MediaRecorder mMediaRecorder;
    transient private CameraManager mCameraManager;
    transient private CameraDevice mCameraDevice;
    transient private CameraCaptureSession mPreviewSession;


    /*
     * Settings
     */
    transient private Settings mSettings;
    transient private String mVideoPath;
    transient private double mTimestampStart;
    transient private Log.RecordTimes mRecordTimes;


    transient private static CameraRecorder instance;

    public static CameraRecorder getInstance() {
        if (instance == null) {
            instance = new CameraRecorder();
        }
        return instance;
    }

    /**
     * Constructor
     */
    public CameraRecorder() {
        super(TYPE_CAMERA, Category.OTHER);
    }

    @Override
    public String getName() {
        return "Camera";
    }

    @Override
    public String getStringType() {
        return null;
    }

    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_camera);
    }

    @Override
    public String getWebPage(Resources res) {
        return res.getString(R.string.webpage_camera);
    }

    @Override
    public String getFileExtension() {
        return "mp4";
    }

    @Override
    public boolean exists(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED);
    }

    public boolean checkPermission(Context context, Settings settings) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        if (settings.useAudio && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void start(Context context, Sensor.Settings settings, Log.RecordTimes recordTimes) {

        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mRecordTimes = recordTimes;

        if (!(settings instanceof Settings)) {
            settings = getDefaultSettings();
        }

        if (!checkPermission(context, (Settings) settings)) {
            return;
        }

        mSettings = (Settings) settings;

        if (mVideoPath == null) {
            throw new RuntimeException("Video path is not set");
        }
        startInternal(mVideoPath);
    }

    public void setVideoPath(String videoPath) {
        mVideoPath = videoPath;
    }

    /**
     * Start the camera recording process
     *
     * @param videoPath The path where the video will be stored
     */
    public void startInternal(String videoPath) {

        mBackgroundThread = new HandlerThread(CAMERA_THREAD);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());


        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(
                    String.valueOf(mSettings.cameraLens.lens));
            Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            mMediaRecorder = new MediaRecorder();

            // https://stackoverflow.com/a/17815035/2239938
            CamcorderProfile profile =
                    CamcorderProfile.get(mSettings.cameraLens.lens, mSettings.outputQuality.profile);

            // Media source(s)
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            if (mSettings.useAudio) {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            }

            // Output format
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(videoPath);

            // Video Settings
            mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
            mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mMediaRecorder.setOrientationHint(sensorOrientation == null ? 0 : sensorOrientation);

            if (mSettings.useAudio) {
                // Audio settings
                mMediaRecorder.setAudioChannels(profile.audioChannels);
                mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
            }

            mMediaRecorder.prepare();

            mCameraManager.openCamera(String.valueOf(mSettings.cameraLens.lens),
                    cameraDeviceStateCallback, mBackgroundHandler);

        } catch (IOException | CameraAccessException | SecurityException e) {
            e.printStackTrace();
        }


    }


    /**
     * Stop the camera recording process
     */
    @Override
    public void stop(Context context) {

        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
        }

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private final CameraDevice.StateCallback
            cameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try {
                camera.createCaptureSession(Collections.singletonList(mMediaRecorder.getSurface()),
                        cameraCaptureSessionStateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
        }

    };


    private final CameraCaptureSession.StateCallback
            cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mPreviewSession = session;
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            super.onReady(session);
            try {
                CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(
                        CameraDevice.TEMPLATE_RECORD);
                builder.addTarget(mMediaRecorder.getSurface());
                builder.set(CaptureRequest.CONTROL_AF_MODE, mSettings.autoFocus.param);
                session.setRepeatingRequest(builder.build(), null, mBackgroundHandler);
                mTimestampStart = System.currentTimeMillis() / 1e3;
                mMediaRecorder.start();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    };

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Settings getDefaultSettings() {
        return Settings.DEFAULT;
    }

    @Override
    public boolean mustRunOnUiThread() {
        return false;
    }

    @Override
    public List<Log.IniRecord> getExtraIniRecords(Context context) {
        ArrayList<Log.IniRecord> iniRecords = new ArrayList<>();
        iniRecords.add(new Log.IniRecord("Camera", "VideoOffset", mTimestampStart - mRecordTimes.startTime));
        return iniRecords;
    }

    public enum CameraLens {
        L_FACING_FRONT(CameraMetadata.LENS_FACING_FRONT, "FACING_FRONT"),
        L_FACING_BACK(CameraMetadata.LENS_FACING_BACK, "FACING_BACK"),
        L_FACING_EXTERNAL(CameraMetadata.LENS_FACING_EXTERNAL, "FACING_EXTERNAL");

        private int lens;
        private String name;

        CameraLens(int lens, String name) {
            this.lens = lens;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum OutputQuality {
        Q_CIF(QUALITY_CIF, "CIF"),
        Q_480P(QUALITY_480P, "480P"),
        Q_720P(QUALITY_720P, "720P"),
        Q_1080P(QUALITY_1080P, "1080P");

        private int profile;
        private String name;

        OutputQuality(int profile, String name) {
            this.profile = profile;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AutoFocus {
        AF_OFF(CaptureRequest.CONTROL_AF_MODE_OFF, "Off"),
        AF_CONTINUOUS(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO, "Continuous");

        private int param;
        private String name;

        AutoFocus(int param, String name) {
            this.param = param;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    public static class Settings extends Sensor.Settings {

        public CameraLens cameraLens;
        public OutputQuality outputQuality;
        public AutoFocus autoFocus;
        public boolean useAudio;

        public static Settings DEFAULT = new Settings(CameraLens.L_FACING_FRONT,
                OutputQuality.Q_480P, AutoFocus.AF_CONTINUOUS, false);

        public Settings(CameraLens cameraLens, OutputQuality outputQuality, AutoFocus autoFocus,
                        boolean useAudio) {
            this.cameraLens = cameraLens;
            this.outputQuality = outputQuality;
            this.autoFocus = autoFocus;
            this.useAudio = useAudio;
        }

        @Override
        public String toString() {
            return "Settings{" +
                    "cameraLens=" + cameraLens +
                    ", outputQuality=" + outputQuality +
                    ", autoFocus=" + autoFocus +
                    ", useAudio=" + useAudio +
                    '}';
        }
    }
}