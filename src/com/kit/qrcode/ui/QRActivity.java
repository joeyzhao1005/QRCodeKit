package com.kit.qrcode.ui;

import java.io.IOException;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.kit.extend.qrcode.R;
import com.kit.qrcode.camera.CameraManager;
import com.kit.qrcode.decoding.CaptureActivityHandler;
import com.kit.qrcode.decoding.InactivityTimer;
import com.kit.ui.BaseActivity;
import com.kit.utils.IntentUtils;
import com.kit.utils.ZogUtils;
import com.kit.utils.StringUtils;
//import com.ericssonlabs.R;
//import com.hiaas.hibit.nemo.QRcode.view.ViewfinderView;

public class QRActivity extends BaseActivity implements Callback,
		OnClickListener, IQRStrategy {
	// private TopBar4Layout topBar;

	private CaptureActivityHandler handler;
	// private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	private TextView tvTips;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private RelativeLayout mContainer = null;
	private RelativeLayout mCropLayout = null;

	private LinearLayout llLeft;

	private Context mContext;

	private LoadDriverTask loadDriverTask;

	private boolean isClipboard = true;

	private Handler handler1 = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				initCamera(surfaceHolder);
				break;

			}
			super.handleMessage(msg);
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initWidget();

	}

	public boolean initWidget() {
		mContext = this;
		setContentView(R.layout.qr_activity);

		// topBar = (TopBar4Layout) findViewById(R.id.topBar);
		// setContentView(R.layout.camera);
		// ViewUtil.addTopView(getApplicationContext(), this,
		// R.string.scan_card);
		// CameraManager.init(getApplication());
		// viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		// tvTips = (TextView) this.findViewById(R.id.tvTips);
		// hasSurface = false;
		// inactivityTimer = new InactivityTimer(this);

		CameraManager.init(getApplication());

		llLeft = (LinearLayout) findViewById(R.id.llLeft);

		mContainer = (RelativeLayout) findViewById(R.id.capture_containter);
		mCropLayout = (RelativeLayout) findViewById(R.id.capture_crop_layout);

		ImageView mQrLineView = (ImageView) findViewById(R.id.capture_scan_line);
		TranslateAnimation mAnimation = new TranslateAnimation(
				TranslateAnimation.ABSOLUTE, 0f, TranslateAnimation.ABSOLUTE,
				0f, TranslateAnimation.RELATIVE_TO_PARENT, 0f,
				TranslateAnimation.RELATIVE_TO_PARENT, 0.9f);
		mAnimation.setDuration(1500);
		mAnimation.setRepeatCount(-1);
		mAnimation.setRepeatMode(Animation.RESTART);
		mAnimation.setInterpolator(new LinearInterpolator());
		mQrLineView.setAnimation(mAnimation);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);

		llLeft.setOnClickListener(this);

		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		surfaceView = (SurfaceView) findViewById(R.id.capture_preview);
		surfaceHolder = surfaceView.getHolder();

		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;

		// quit the scan view
		// tvsetOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// CaptureActivity.this.finish();
		// }
		// });
	}

	@Override
	public void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
		java.lang.System.gc();
		// IntentUtils.gotoNextActivity(this, MineActivity.class);
	}

	@Override
	protected void onStop() {
		inactivityTimer.shutdown();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// inactivityTimer.shutdown();
		super.onDestroy();
	}

	/**
	 * Handler scan result
	 * 
	 * @param result
	 * @param barcode
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void handleDecode(Result result, Bitmap barcode) {
		inactivityTimer.onActivity();
		playBeepSoundAndVibrate();
		String resultString = StringUtils.getChineseString(result.getText());
		// FIXME
		if (!TextUtils.isEmpty(resultString)) {

			qrWhereToGo(resultString);

		} else {
			Toast.makeText(QRActivity.this, "empty!", Toast.LENGTH_SHORT)
					.show();
		}
		QRActivity.this.finish();
	}

	private class LoadDriverTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();

			// topBar.pbLoading(loadDriverTask);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				CameraManager.get().openDriver(surfaceHolder);
			} catch (IOException ioe) {
				return false;
			} catch (RuntimeException e) {
				return false;
			}
			return true;

		}

		@Override
		protected void onPostExecute(Boolean result) {

			if (handler == null) {
				handler = new CaptureActivityHandler((QRActivity) mContext,
						decodeFormats, characterSet);
			}
		}

	}

	private void initCamera(SurfaceHolder surfaceHolder) {

		doLoadDriverTask();

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			// initCamera(holder);
			handler1.sendEmptyMessage(0);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	// public ViewfinderView getViewfinderView() {
	// return viewfinderView;
	// }

	public Handler getHandler() {
		return handler;
	}

	// public void drawViewfinder() {
	// viewfinderView.drawViewfinder();
	//
	// }

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(
					R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	// //处理扫描结果（在界面上显示）
	// if (resultCode == RESULT_OK) {
	// Bundle bundle = data.getExtras();
	// String scanResult = bundle.getString("result");
	// //resultTextView.setText(scanResult);
	// }
	// }
	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

	public void doLoadDriverTask() {

		if (loadDriverTask != null) {
			loadDriverTask.cancel(true);
			loadDriverTask = new LoadDriverTask();
			loadDriverTask.execute();
		} else {

			loadDriverTask = new LoadDriverTask();
			loadDriverTask.execute();
		}
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.llLeft) {
			this.finish();
		}

	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void qrWhereToGo(String content) {
		if (isClipboard) {
			ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clip.setText(content); // 复制
		}

		ZogUtils.printLog(mContext.getClass(), "QR", "content:" + content);

		Intent resultIntent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putString("content", content);
		resultIntent.putExtras(bundle);
		IntentUtils.gotoNextActivity(mContext,
				QRCodeShowResultWebActivity.class, bundle, true);

	}

	@Override
	public void qrDoSomething() {
		// TODO Auto-generated method stub

	}

}