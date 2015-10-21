package com.example.testonly;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MediaController extends FrameLayout {
	private static final String TAG = "MediaController";

	private static final int FADE_OUT = 1;
	private static final int SET_PROGRESS = 2;
	private static final int HIDE_GESTURE_ROOT = 3;
	private static final int MSG_TIME_TICK = 4;
	private static final int MSG_HIDE_OPERATION_INFO = 5;

	private static final int TIME_TICK_INTERNAL = 1000;
	private static final int NORMAL_INTERNAL = 500;
	private static final int DEFAULT_TIME_OUT = 3000;

	private MediaPlayerControl mPlayer;
	private Context mContext;
	private PopupWindow mPopupWindow;
	/**
	 * View that acts as the anchor for the control view. this will be set on
	 * the setAnchorView method
	 */
	private View mAnchor;
	private View mMediaController;
	private SeekBar mProgress;
	private TextView mEndTime;
	private TextView mCurrentTime;
	private TextView mFileName;
	private String mTitle;

	private long mDuration;
	private boolean mShowing;
	private boolean mDragging;
	private boolean mInstantSeeking = true;
	private boolean mFromXml = false;

	private ImageButton mPauseButton;
	private ImageButton mScreenSizeButton;
	private LinearLayout mControllerLayout;
	private RelativeLayout mControllerRoot;
	private ImageButton mLockButton;
	// private ImageButton mMenuButton;
	private TextView mDownloadRate;
	private TextView mBatteryLevel;
	private TextView mSystemTime;
	private TextView mOperationInfo;

	private AudioManager mAudioManager;
	private OnShownListener mShownListener;
	private OnHiddenListener mHiddenListener;
	private GestureDetector mGestureDetector;
	private VideoGestureListener mGestureListener;

	private int mMaxVolume;
	private int mVolume = -1;
	private float mBrightness = -1f;
	private View mOperationRoot;
	private ImageView mOperationBg;
	private ImageView mOperationPercent;

	private boolean mLocked;

	private int mLayout;

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			long pos;
			switch (msg.what) {
			case FADE_OUT:
				hide();
				break;
			case SET_PROGRESS:
				pos = setProgress();
				if (!mDragging && mShowing) {
					msg = obtainMessage(SET_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
					updatePausePlay();
				}
				break;
			case HIDE_GESTURE_ROOT:
				if (mOperationRoot != null) {
					mOperationRoot.setVisibility(View.GONE);
				}
				break;

			case MSG_HIDE_OPERATION_INFO:
				if (mOperationInfo != null) {
					mOperationInfo.setVisibility(View.GONE);
				}
				break;

			case MSG_TIME_TICK:
				mSystemTime.setText(StringUtils.currentTimeString());
				sendEmptyMessageDelayed(MSG_TIME_TICK, TIME_TICK_INTERNAL);
				break;

			}
		}
	};

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		public void onClick(View v) {
			doPauseResume();
			show(DEFAULT_TIME_OUT);
		}
	};

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			mDragging = true;
			show(3600000);
			mHandler.removeMessages(SET_PROGRESS);
			if (mInstantSeeking) {
				mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
				if (!mPlayer.isPlaying()) {
					mPlayer.start();
				}
			}

		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (!fromuser)
				return;

			long newposition = (mDuration * progress) / 1000;
			String time = StringUtils.generateTime(newposition);
			if (mInstantSeeking) {
				mPlayer.seekTo((int) newposition);
			}

			if (mCurrentTime != null) {
				mCurrentTime.setText(time);
			}

			setOperationInfo(time, 500);
		}

		public void onStopTrackingTouch(SeekBar bar) {
			if (!mInstantSeeking) {
				mPlayer.seekTo((int) ((mDuration * bar.getProgress()) / 1000));
			}
			show(DEFAULT_TIME_OUT);
			mHandler.removeMessages(SET_PROGRESS);
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			mDragging = false;
			mHandler.sendEmptyMessageDelayed(SET_PROGRESS, TIME_TICK_INTERNAL);
			mHandler.sendEmptyMessageDelayed(MSG_HIDE_OPERATION_INFO,
					NORMAL_INTERNAL);
		}
	};

	public MediaController(Context context, AttributeSet attrs) {
		super(context, attrs);
		mMediaController = this;
		mFromXml = true;
		initController(context);
	}

	/**
	 * @param context
	 *            Must be an Activity
	 */
	public MediaController(Context context) {
		super(context);
		if (!mFromXml && initController(context)) {
			initFloatingWindow();
		}
	}

	private boolean initController(Context context) {
		mContext = context;
		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mGestureListener = new VideoGestureListener();
		mGestureDetector = new GestureDetector(mContext, mGestureListener);
		return true;
	}

	@Override
	public void onFinishInflate() {
		if (mMediaController != null) {
			initControllerView(mMediaController);
		}
	}

	private void initFloatingWindow() {
		mPopupWindow = new PopupWindow(mContext);
		mPopupWindow.setFocusable(false);
		mPopupWindow.setBackgroundDrawable(null);
		mPopupWindow.setOutsideTouchable(true);
	}

	/**
	 * Set the view that acts as the anchor for the control view. This can for
	 * example be a VideoView, or your Activity's main view. If call this method
	 * the controller is already showing, but the view is invisible
	 * 
	 * @param view
	 *            The view to which to anchor the controller when it is visible.
	 */
	public void setAnchorView(View view) {
		mAnchor = view;
		if (!mFromXml) {
			removeAllViews();
			mMediaController = makeControllerView();
			mPopupWindow.setContentView(mMediaController);
			mPopupWindow.setWidth(LayoutParams.MATCH_PARENT);
			mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
		}
		initControllerView(mMediaController);

		int[] location = new int[2];
		mAnchor.getLocationOnScreen(location);
		Rect anchorRect = new Rect(location[0], location[1], location[0]
				+ mAnchor.getWidth(), location[1] + mAnchor.getHeight());
		mPopupWindow.showAtLocation(mAnchor, Gravity.NO_GRAVITY,
				anchorRect.left, anchorRect.bottom);
	}

	/**
	 * Create the view that holds the widgets that control playback. Derived
	 * classes can override this to create their own.
	 * 
	 * @return The controller view.
	 */
	protected View makeControllerView() {
		return ((LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.mediacontroller, this);
	}

	/**
	 * Find the view of the media controller and set the listener of the
	 * component
	 * 
	 * @param v
	 *            Controller View
	 */
	protected void initControllerView(View v) {
		mControllerRoot = (RelativeLayout) v
				.findViewById(R.id.mediacontroller_controller_root);
		mControllerLayout = (LinearLayout) v
				.findViewById(R.id.mediacontroller_controller);

		mPauseButton = (ImageButton) v
				.findViewById(R.id.mediacontroller_play_pause);
		if (mPauseButton != null) {
			mPauseButton.requestFocus();
			mPauseButton.setOnClickListener(mPauseListener);
		}

		// mSnapshotButton = (ImageButton)
		// v.findViewById(R.id.mediacontroller_snapshot);
		mScreenSizeButton = (ImageButton) v
				.findViewById(R.id.mediacontroller_screen_size);
		// mSnapshotButton.setOnClickListener(mSnapshotListener);
		mScreenSizeButton.setOnClickListener(mScreenToggleListener);
		mLockButton = (ImageButton) v.findViewById(R.id.mediacontroller_lock);
		// mMenuButton = (ImageButton)
		// v.findViewById(R.id.mediacontroller_menu);
		mLockButton.setOnClickListener(mLockClickListener);
		// mMenuButton.setOnClickListener(mMenuClickListener);

		mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_seekbar);
		mProgress.setOnSeekBarChangeListener(mSeekListener);

		mDownloadRate = (TextView) v
				.findViewById(R.id.mediacontroller_download_rate);
		mBatteryLevel = (TextView) v
				.findViewById(R.id.mediacontroller_battery_level);
		mSystemTime = (TextView) v.findViewById(R.id.mediacontroller_time);
		mEndTime = (TextView) v.findViewById(R.id.mediacontroller_time_total);
		mCurrentTime = (TextView) v
				.findViewById(R.id.mediacontroller_time_current);
		mFileName = (TextView) v.findViewById(R.id.mediacontroller_file_name);
		if (mFileName != null) {
			mFileName.setText(mTitle);
		}

		mOperationRoot = v.findViewById(R.id.operation_volume_brightness);
		mOperationInfo = (TextView) v.findViewById(R.id.operation_info);
		mOperationBg = (ImageView) v.findViewById(R.id.operation_bg);
		mOperationPercent = (ImageView) v.findViewById(R.id.operation_percent);
	}

	public void setMediaPlayer(MediaPlayerControl player) {
		mPlayer = player;
		updatePausePlay();
	}

	/**
	 * Control the action when the seekbar dragged by user
	 * 
	 * @param seekWhenDragging
	 *            True the media will seek periodically
	 */
	public void setInstantSeeking(boolean seekWhenDragging) {
		mInstantSeeking = seekWhenDragging;
	}

	public void show() {
		show(DEFAULT_TIME_OUT);
	}

	/**
	 * Set the content of the file_name TextView
	 * 
	 * @param name
	 */
	public void setFileName(String name) {
		mTitle = name;
		if (mFileName != null) {
			mFileName.setText(mTitle);
		}
	}

	private void setCurrentTime(String time) {
		if (mCurrentTime != null) {
			mCurrentTime.setText(time);
		}
	}

	private void setTotalTime(String time) {
		if (mEndTime != null) {
			mEndTime.setText(time);
		}
	}

	private void setOperationInfo(String info, long time) {
		mOperationInfo.setText(info);
		mOperationInfo.setVisibility(View.VISIBLE);
		mHandler.removeMessages(MSG_HIDE_OPERATION_INFO);
		mHandler.sendEmptyMessageDelayed(MSG_HIDE_OPERATION_INFO, time);
	}

	public void setDownloadRate(String rate) {
		mDownloadRate.setVisibility(View.VISIBLE);
		mDownloadRate.setText(rate);
	}

	public void setBatteryLevel(String level) {
		if (mBatteryLevel == null) {
			return;
		}
		mBatteryLevel.setVisibility(View.VISIBLE);
		mBatteryLevel.setText(level);
	}

	/**
	 * Show the controller on screen. It will go away automatically after
	 * 'timeout' milliseconds of inactivity.
	 * 
	 * @param timeout
	 *            The timeout in milliseconds. Use 0 to show the controller
	 *            until hide() is called.
	 */
	public void show(final int timeout) {
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendEmptyMessageDelayed(FADE_OUT, timeout);
		} else {
			mHandler.sendEmptyMessageDelayed(FADE_OUT, DEFAULT_TIME_OUT);
		}

		if (!mShowing && mAnchor != null && mAnchor.getWindowToken() != null) {
			if (mPauseButton != null) {
				mPauseButton.requestFocus();
			}
			if (mFromXml) {
				setVisibility(View.VISIBLE);
			} else {
				mControllerRoot.setVisibility(View.VISIBLE);
			}

			updatePausePlay();
			mHandler.sendEmptyMessage(MSG_TIME_TICK);
			mHandler.sendEmptyMessage(SET_PROGRESS);
			mShowing = true;
			if (mShownListener != null) {
				mShownListener.onShown();
			}
		}
	}

	public boolean isShowing() {
		return mShowing;
	}

	public void hide() {
		if (mAnchor == null) {
			return;
		}

		if (mShowing) {
			try {
				mHandler.removeMessages(SET_PROGRESS);
				mHandler.removeMessages(MSG_TIME_TICK);
				if (mFromXml) {
					setVisibility(View.GONE);
				} else {
					mControllerRoot.setVisibility(View.INVISIBLE);
				}
			} catch (IllegalArgumentException ex) {
				ex.printStackTrace();
			}
			mShowing = false;
			if (mHiddenListener != null) {
				mHiddenListener.onHidden();
			}
		}
	}

	public void setOnShownListener(OnShownListener l) {
		mShownListener = l;
	}

	public void setOnHiddenListener(OnHiddenListener l) {
		mHiddenListener = l;
	}

	private long setProgress() {
		if (mPlayer == null || mDragging) {
			return 0;
		}

		long position = mPlayer.getCurrentPosition();
		long duration = mPlayer.getDuration();
		if (mProgress != null) {
			if (duration > 0) {
				long pos = 1000L * position / duration;
				mProgress.setProgress((int) pos);
			}
			int percent = mPlayer.getBufferPercentage();
			mProgress.setSecondaryProgress(percent * 10);
		}

		mDuration = duration;

		setCurrentTime(StringUtils.generateTime(position));
		setTotalTime(StringUtils.generateTime(mDuration));

		return position;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event)) {
			return true;
		}
		// Hide the controller
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			endGesture();
			break;
		}

		return super.onTouchEvent(event);
	}

	/**
	 * Hide the controller
	 */
	private void endGesture() {
		mVolume = -1;
		mBrightness = -1f;
		mHandler.sendEmptyMessageDelayed(HIDE_GESTURE_ROOT, NORMAL_INTERNAL);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		show(DEFAULT_TIME_OUT);
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		Log.e(TAG, "dispatch key event and key code is : " + keyCode);
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_MUTE:
			return super.dispatchKeyEvent(event);
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			Log.d(TAG, "dispatch key event. up and down");
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			int step = keyCode == KeyEvent.KEYCODE_VOLUME_UP ? 1 : -1;
			setVolume(mVolume + step);
			mHandler.removeMessages(HIDE_GESTURE_ROOT);
			mHandler.sendEmptyMessageDelayed(HIDE_GESTURE_ROOT, 500);
			return true;
		}

		if (mLocked) {
			show();
			return true;
		}

		if (event.getRepeatCount() == 0
				&& (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
						|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE)) {
			doPauseResume();
			show(DEFAULT_TIME_OUT);
			if (mPauseButton != null)
				mPauseButton.requestFocus();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				updatePausePlay();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_MENU) {
			hide();
			return true;
		} else {
			show(DEFAULT_TIME_OUT);
		}
		return super.dispatchKeyEvent(event);
	}

	private void updatePausePlay() {
		if (mMediaController == null || mPauseButton == null) {
			return;
		}

		if (mPlayer.isPlaying()) {
			mPauseButton
					.setImageResource(R.drawable.mediacontroller_pause_button);
		} else {
			mPauseButton
					.setImageResource(R.drawable.mediacontroller_play_button);
		}
	}

	private void doPauseResume() {
		if (mPlayer.isPlaying()) {
			mPlayer.pause();
		} else {
			mPlayer.start();
		}
		updatePausePlay();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (mPauseButton != null)
			mPauseButton.setEnabled(enabled);
		if (mProgress != null)
			mProgress.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	private void lock(boolean toLock) {
		if (toLock) {
			mLockButton.setImageResource(R.drawable.mediacontroller_lock);
			// mMenuButton.setVisibility(View.GONE);
			mControllerLayout.setVisibility(View.GONE);
			mProgress.setEnabled(false);
			if (mLocked != toLock) {
				setOperationInfo(
						mContext.getString(R.string.video_screen_locked), 1000);
			}
		} else {
			mLockButton.setImageResource(R.drawable.mediacontroller_unlock);
			// mMenuButton.setVisibility(View.GONE);
			mControllerLayout.setVisibility(View.VISIBLE);
			mProgress.setEnabled(true);
			if (mLocked != toLock) {
				setOperationInfo(
						mContext.getString(R.string.video_screen_unlocked),
						1000);
			}
		}
		mLocked = toLock;
	}

	private View.OnClickListener mLockClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			hide();
			lock(!mLocked);
			show();
		}
	};

	private View.OnClickListener mScreenToggleListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			show(DEFAULT_TIME_OUT);
			toogleScreen();
		}
	};

	// private View.OnClickListener mMenuClickListener = new
	// View.OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// show(DEFAULT_TIME_OUT);
	// // TODO ..
	// }
	// };

	public static int getScreenWidth(Context context) {
		DisplayMetrics metric = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(metric);
		return metric.widthPixels;
	}

	public static int getScreenHeight(Context context) {
		DisplayMetrics metric = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(metric);
		return metric.heightPixels;
	}

	private class VideoGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (!mLocked) {
				toogleScreen();
			}
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if (mControllerRoot.getVisibility() != View.VISIBLE) {
				show();
			} else {
				hide();
			}

			return super.onSingleTapUp(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			if (mLocked) {
				return true;
			}
			float mOldX = e1.getX(), mOldY = e1.getY();
			int y = (int) e2.getRawY();

			int windowWidth = getScreenWidth(mContext);
			int windowHeight = getScreenHeight(mContext);

			if (mOldX > windowWidth * 4.0 / 5)// ??�边�????
				onVolumeSlide((mOldY - y) / windowHeight);
			else if (mOldX < windowWidth / 5.0)// �?边�?????
				onBrightnessSlide((mOldY - y) / windowHeight);

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	private void toogleScreen() {
		// VIDEO_LAYOUT_ORIGIN = 0
		// VIDEO_LAYOUT_SCALE = 1
		// VIDEO_LAYOUT_STRETCH = 2
		// VIDEO_LAYOUT_ZOOM = 3
		if (mLayout == 3) {
			mLayout = 0;
		} else {
			mLayout++;
		}
		if (mPlayer != null) {
			mPlayer.setVideoLayout(mLayout, 0);
		}
	}

	private void onVolumeSlide(float percent) {
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0)
				mVolume = 0;
			mOperationBg.setImageResource(R.drawable.video_volumn_bg);
		}

		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;
		mOperationRoot.setVisibility(View.VISIBLE);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = findViewById(R.id.operation_full).getLayoutParams().width
				* index / mMaxVolume;
		mOperationPercent.setLayoutParams(lp);
	}

	private void setVolume(int v) {
		if (v > mMaxVolume)
			v = mMaxVolume;
		else if (v < 0)
			v = 0;
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
		onVolumeSlide((float) v / mMaxVolume);
	}

	private void onBrightnessSlide(float percent) {
		if (mBrightness < 0) {
			mBrightness = ((Activity) mContext).getWindow().getAttributes().screenBrightness;
			if (mBrightness <= 0.00f)
				mBrightness = 0.50f;
			if (mBrightness < 0.01f)
				mBrightness = 0.01f;

			mOperationBg.setImageResource(R.drawable.video_brightness_bg);
		}
		WindowManager.LayoutParams lpa = ((Activity) mContext).getWindow()
				.getAttributes();
		lpa.screenBrightness = mBrightness + percent;
		if (lpa.screenBrightness > 1.0f)
			lpa.screenBrightness = 1.0f;
		else if (lpa.screenBrightness < 0.01f)
			lpa.screenBrightness = 0.01f;
		((Activity) mContext).getWindow().setAttributes(lpa);

		mOperationRoot.setVisibility(View.VISIBLE);

		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = (int) (findViewById(R.id.operation_full).getLayoutParams().width * lpa.screenBrightness);
		mOperationPercent.setLayoutParams(lp);
	}

	public interface OnShownListener {
		public void onShown();
	}

	public interface OnHiddenListener {
		public void onHidden();
	}

	public interface MediaPlayerControl {
		void start();

		void pause();

		int getDuration();

		int getCurrentPosition();

		void seekTo(int pos);

		boolean isPlaying();

		int getBufferPercentage();

		boolean canPause();

		boolean canSeekBackward();

		boolean canSeekForward();

		void setVideoLayout(int layout, float aspectRatio);

	}
}
