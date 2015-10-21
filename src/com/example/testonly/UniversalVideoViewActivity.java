package com.example.testonly;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.testonly.UniversalVideoView.OnStateChangeListener;

public class UniversalVideoViewActivity extends Activity {
	private static final String TAG = "UniversalVideoViewActivity";
	private UniversalVideoView mUniversalVideoView;
	private View mLoadingView;
	private ImageView mLoadingImage;
	private AnimationDrawable mLoadingAnimation;
	private String path;
	private String title;
	private String whichStream;// 视频流类型 1、使用Vitamio，直播2、使用Vitamio3、不使用Vitamio
	private String mBatteryLevel;

	private MediaController mMediaController;

	private BatteryReceiver mBatteryReceiver;

	private static final IntentFilter BATTERY_FILTER = new IntentFilter(
			Intent.ACTION_BATTERY_CHANGED);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_universalvideoview);
		findView();
		initView();
	}

	private void findView() {
		mUniversalVideoView = (UniversalVideoView) findViewById(R.id.uvv);
		mLoadingView = findViewById(R.id.loading);
		mLoadingImage = (ImageView) findViewById(R.id.iv_loading);
		mLoadingImage.setBackgroundResource(R.drawable.runningman);
		mLoadingAnimation = (AnimationDrawable) mLoadingImage.getBackground();
	}

	private void initView() {
		showLoadingView();
		mMediaController = new MediaController(this);
		mUniversalVideoView.setMediaController(mMediaController);
		setBatteryLevel();

		Intent intent = getIntent();
		if (intent != null) {

			title = intent.getStringExtra("title");
			path = intent.getStringExtra("url").replaceAll("&amp;", "&");
			whichStream = intent.getStringExtra("which");
			setFileName();
			if (whichStream.equals("1")) {
				mUniversalVideoView.setVideoPath(path, true, true);
			} else if (whichStream.equals("2")) {
				mUniversalVideoView.setVideoPath(path, true);
			} else if (whichStream.equals("3")) {
				mUniversalVideoView.setVideoPath(path);
			}

		}

		mUniversalVideoView
				.setOnStateChangeListener(new OnStateChangeListener() {

					@Override
					public void stateChange(State state) {
						if (state == State.PREPARING) {
							showLoadingView();
						} else if (state == State.PLAYING) {
							Log.e("UniversalVideoView",
									"live playing and hide the loading view");
							hideLoadingView();
						} else if (state == State.ERROR) {
							hideLoadingView();
						} else if (state == State.VITAMIO_INITIALIZING) {
							showLoadingView();
						} else if (state == State.PREPARED) {
							Log.e("UniversalVideoView", "live prepared....");
						} else if (state == State.BUFFERING_START) {
							showLoadingView();
						} else if (state == State.BUFFERING_END) {
							hideLoadingView();
						}
					}
				});

		mUniversalVideoView.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				finish();
			}
		});

		mUniversalVideoView
				.setOnCompletionListener(new io.vov.vitamio.MediaPlayer.OnCompletionListener() {

					@Override
					public void onCompletion(io.vov.vitamio.MediaPlayer mp) {
						finish();
					}
				});
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		unRegistReceiver();
		if (mUniversalVideoView != null) {
			mUniversalVideoView.suspend();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		registReceiver();
		if (mUniversalVideoView != null) {
			mUniversalVideoView.resume();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		if (mUniversalVideoView != null)
			mUniversalVideoView.stopPlayback();
	}

	private void registReceiver() {
		if (mBatteryReceiver == null) {
			mBatteryReceiver = new BatteryReceiver();
		}
		registerReceiver(mBatteryReceiver, BATTERY_FILTER);
	}

	private void unRegistReceiver() {
		if (mBatteryLevel != null) {
			unregisterReceiver(mBatteryReceiver);
			mBatteryReceiver = null;
		}
	}

	private void showLoadingView() {
		mLoadingAnimation.start();
		mLoadingView.setVisibility(View.VISIBLE);
	}

	private void hideLoadingView() {
		if (mLoadingAnimation != null && mLoadingAnimation.isRunning()) {
			mLoadingAnimation.stop();
		}
		mLoadingView.setVisibility(View.GONE);
	}

	private void setBatteryLevel() {
		if (mMediaController != null)
			mMediaController.setBatteryLevel(mBatteryLevel);
	}

	private void setFileName() {
		if (mMediaController != null)
			mMediaController.setFileName(title);
	}

	private class BatteryReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
			int percent = scale > 0 ? level * 100 / scale : 0;
			if (percent > 100)
				percent = 100;
			mBatteryLevel = String.valueOf(percent) + "%";
			setBatteryLevel();
		}
	}
}
