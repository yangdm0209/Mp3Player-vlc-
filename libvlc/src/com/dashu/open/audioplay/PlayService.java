package com.dashu.open.audioplay;

import java.util.ArrayList;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.Media.Meta;
import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.libvlc.util.VLCUtil;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class PlayService extends Service {
	public static final String TAG = "Dashu";

	private final IBinder mBinder = new LocalBinder();

	private PlayListener listener;

	private boolean mHasAudioFocus = false;

	private static LibVLC libVLC = null;
	private MediaPlayer mediaPlayer;
	
	private boolean mSeekable = false;
    private boolean mPausable = false;

	/**
	 * Service framework start
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "PlayService onCreate");

		if (!VLCUtil.hasCompatibleCPU(this)) {
			if (null != listener)
				listener.onException(-1, "CPU not suport");
			stopSelf();
			return;
		}

		if (null == libVLC) {
			/*
			ArrayList<String> options = new ArrayList<String>(50);
			options.add("--no-audio-time-stretch");
			options.add("--avcodec-skiploopfilter");
			options.add("3");
			options.add("--avcodec-skip-frame");
			options.add("0");
			options.add("--avcodec-skip-idct");
			options.add("0");
			options.add("--subsdec-encoding");
			options.add("");
			options.add("--stats");
			options.add("--androidwindow-chroma");
			options.add("RV32");
			options.add("--audio-resampler");
			options.add("ugly");
			options.add("-vvv");
			libVLC = new LibVLC(options);
			*/
			libVLC = new LibVLC();
		}
		mediaPlayer = new MediaPlayer(libVLC);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "PlayService onDestroy");
		mediaPlayer.release();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "PlayService onBind");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "PlayService onUnbind");
		if (!isPlaying())
			stopSelf();
		return super.onUnbind(intent);
	}

	/**
	 * control play stop .....
	 */
	public void play(String url, PlayListener listener) {
		Log.i(TAG, "play url is " + url);
		this.listener = listener;
		
		mPausable = mSeekable = true;
        final Media media = new Media(libVLC, Uri.parse(url));
        media.setEventListener(mMediaListener);
        mediaPlayer.setMedia(media);
        media.release();
        
        changeAudioFocus(true);
        mediaPlayer.setEventListener(mMediaPlayerListener);
        
        mediaPlayer.play();
	}
	

	public void stop() {
		Log.i(TAG, "Stop");
		if (mediaPlayer == null)
            return;
    	changeAudioFocus(false);
    	
        final Media media = mediaPlayer.getMedia();
        if (media != null) {
            media.setEventListener(null);
            mediaPlayer.setEventListener(null);
            mediaPlayer.stop();
            mediaPlayer.setMedia(null);
            media.release();
            
            if (null != listener)
            	listener.onStopped();
            listener = null;
        }
        stopSelf();
	}

	public void pause() {
		Log.i(TAG, "pause");
    	changeAudioFocus(false);
		if (mPausable) {
            changeAudioFocus(false);
            // 通知界面
        }
	}
	
	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	
	/**
	 * 按照AudioFocus的机制，在使用AudioStream之前，需要申请AudioFocus，
	 * 在获得AudioFocus之后才可以使用相应的AudioStream；如果有别的程序竞争你正在使用的AudioStream，
	 * 你的程序需要在收到通知之后做停止播放或者降低声音的处理。值得指出的是，这种机制是需要合作完成的，
	 * 需要所有使用Audio资源的程序都按照这种机制来做，而如果有程序在它失去AudioFocus的时候仍然在使用Audio，AudioFocus拿它也没办法。
	 */
	private final OnAudioFocusChangeListener mAudioFocusListener = AndroidUtil
			.isFroyoOrLater() ? createOnAudioFocusChangeListener() : null;

	@TargetApi(Build.VERSION_CODES.FROYO)
	private OnAudioFocusChangeListener createOnAudioFocusChangeListener() {
		return new OnAudioFocusChangeListener() {
			private boolean mLossTransient = false;
			private boolean mLossTransientCanDuck = false;

			@Override
			public void onAudioFocusChange(int focusChange) {
				/*
				 * Pause playback during alerts and notifications
				 */
				switch (focusChange) {
				case AudioManager.AUDIOFOCUS_LOSS:
					Log.i(TAG, "AUDIOFOCUS_LOSS");
					// Stop playback
					changeAudioFocus(false);
					stop();
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
					// Pause playback
					if (mediaPlayer.isPlaying()) {
						mLossTransient = true;
						mediaPlayer.pause();
					}
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
					// Lower the volume
					if (mediaPlayer.isPlaying()) {
						mediaPlayer.setVolume(36);
						mLossTransientCanDuck = true;
					}
					break;
				case AudioManager.AUDIOFOCUS_GAIN:
					Log.i(TAG, "AUDIOFOCUS_GAIN: " + mLossTransientCanDuck
							+ ", " + mLossTransient);
					// Resume playback
					if (mLossTransientCanDuck) {
						mediaPlayer.setVolume(100);
						mLossTransientCanDuck = false;
					}
					if (mLossTransient) {
						mediaPlayer.play();
						mLossTransient = false;
					}
					break;
				}
			}
		};
	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	private void changeAudioFocusFroyoOrLater(boolean acquire) {
		final AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (am == null)
			return;

		if (acquire) {
			if (!mHasAudioFocus) {
				final int result = am
						.requestAudioFocus(mAudioFocusListener,
								AudioManager.STREAM_MUSIC,
								AudioManager.AUDIOFOCUS_GAIN);
				if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					am.setParameters("bgm_state=true");
					mHasAudioFocus = true;
				}
			}
		} else {
			if (mHasAudioFocus) {
				final int result = am.abandonAudioFocus(mAudioFocusListener);
				am.setParameters("bgm_state=false");
				mHasAudioFocus = false;
			}
		}
	}

	private void changeAudioFocus(boolean acquire) {
		if (AndroidUtil.isFroyoOrLater())
			changeAudioFocusFroyoOrLater(acquire);
	}
	/**
	 * AudioFocus 处理结束
	 */
	private static String getMetaId(Media media, int id, boolean trim) {
        String meta = media.getMeta(id);
        return meta != null ? trim ? meta.trim() : meta : null;
    }
	
	
	
	private String mArtist="";
	private String mNowPlaying="";
	
	private void updateMeta(){
		final Media media = mediaPlayer.getMedia();
		
		String artist = getMetaId(media, Meta.Artist, true);
		String nowPlaying = getMetaId(media, Meta.NowPlaying, false);
        
        media.release();
        
        if (null != artist && !mArtist.equals(artist)) {
        	mArtist = artist;
		}
        
        if (null != nowPlaying && !mNowPlaying.equals(nowPlaying)) {
			mNowPlaying = nowPlaying;
		}
        
        if (null != listener) {
        	if (!TextUtils.isEmpty(mArtist)) {
        		listener.onTitleChanged(mArtist);
			} 
        	else if (!TextUtils.isEmpty(mNowPlaying)) {
        		listener.onTitleChanged(mNowPlaying);
			} 							
		}
        Log.d(TAG, "updateMeta->Artist " + mArtist);
        Log.d(TAG, "updateMeta->mNowPlaying " + mNowPlaying);
	}
	
	private final Media.EventListener mMediaListener = new Media.EventListener() {
        @Override
        public void onEvent(Media.Event event) {
            switch (event.type) {
                case Media.Event.MetaChanged:
                    Log.i(TAG, "Media.Event.MetaChanged: " + event.getMetaId());
                    updateMeta();
                    break;

                case Media.Event.ParsedChanged:
                    Log.i(TAG, "Media.Event.ParsedChanged");
                    break;

            }
        }
    };

    private final MediaPlayer.EventListener mMediaPlayerListener = new MediaPlayer.EventListener() {

        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    Log.i(TAG, "MediaPlayer.Event.Playing " + mediaPlayer.getLength());
                    executeUpdate();
                    changeAudioFocus(true);
                    if (null != listener)
                    	listener.onPlaying();
                    break;
                    
                case MediaPlayer.Event.Paused:
                    Log.i(TAG, "MediaPlayer.Event.Paused");
                    executeUpdate();
                    if (null != listener)
                    	listener.onPaused();
                    break;
                    
                case MediaPlayer.Event.Stopped:
                    Log.i(TAG, "MediaPlayer.Event.Stopped");
                    executeUpdate();
                    changeAudioFocus(false);
                    if (null != listener)
                    	listener.onStopped();
                    break;
                    
                case MediaPlayer.Event.EndReached:
                    Log.i(TAG, "MediaPlayer.Event.EndReached");
                    executeUpdate();
                    changeAudioFocus(false);
                    break;
                    
                case MediaPlayer.Event.EncounteredError:
                	Log.i(TAG, "MediaPlayer.Event.EncounteredError");
                    executeUpdate();
                    break;
                    
                case MediaPlayer.Event.TimeChanged:
                	Log.i(TAG, "MediaPlayer.Event.TimeChanged -> " + event.getTimeChanged());
                	if (null != listener)
                    	listener.onTimeChanged(event.getTimeChanged());
                    break;
                    
                case MediaPlayer.Event.PositionChanged:
                	Log.i(TAG, "MediaPlayer.Event.PositionChanged -> " + event.getPositionChanged());
                	if (null != listener)
                    	listener.onPositionChanged(event.getPositionChanged());
                    break;
                    
                case MediaPlayer.Event.Vout:
                	Log.i(TAG, "MediaPlayer.Event.Vout");
                    break;
                    
                case MediaPlayer.Event.ESAdded:
                	Log.i(TAG, "MediaPlayer.Event.ESAdded");
                    break;
                    
                case MediaPlayer.Event.ESDeleted:
                	Log.i(TAG, "MediaPlayer.Event.ESDeleted");
                    break;
                    
                case MediaPlayer.Event.PausableChanged:
                	Log.i(TAG, "MediaPlayer.Event.PausableChanged -> " + event.getPausable());
                    mPausable = event.getPausable();
                    break;
                    
                case MediaPlayer.Event.SeekableChanged:
                	Log.i(TAG, "MediaPlayer.Event.SeekableChanged -> " + event.getSeekable());
                    mSeekable = event.getSeekable();
                    break;
            }
        }
    };
	
    private void executeUpdate(){
    	
    }

	private class LocalBinder extends Binder {
		PlayService getService() {
			return PlayService.this;
		}
	}

	public static PlayService getService(IBinder iBinder) {
		LocalBinder binder = (LocalBinder) iBinder;
		return binder.getService();
	}

	/**
	 * client
	 */
	public static class Client {

		public interface Callback {
			void onConnected(PlayService service);

			void onDisconnected();
		}

		private boolean mBound = false;
		private final Callback mCallback;
		private final Context mContext;

		private final ServiceConnection mServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder iBinder) {
				Log.d(TAG, "Service Connected");
				if (!mBound)
					return;

				final PlayService service = PlayService.getService(iBinder);
				if (service != null)
					mCallback.onConnected(service);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.d(TAG, "Service Disconnected");
				mCallback.onDisconnected();
			}
		};

		private static Intent getServiceIntent(Context context) {
			return new Intent(context, PlayService.class);
		}

		private static void startService(Context context) {
			context.startService(getServiceIntent(context));
		}

		private static void stopService(Context context) {
			context.stopService(getServiceIntent(context));
		}

		public Client(Context context, Callback callback) {
			if (context == null || callback == null)
				throw new IllegalArgumentException(
						"Context and callback can't be null");
			mContext = context;
			mCallback = callback;
		}

		public void connect() {
			if (mBound)
				throw new IllegalStateException("already connected");
			startService(mContext);
			mBound = mContext.bindService(getServiceIntent(mContext),
					mServiceConnection, BIND_AUTO_CREATE);
		}

		public void disconnect() {
			if (mBound) {
				mBound = false;
				mContext.unbindService(mServiceConnection);
			}
		}

		public static void restartService(Context context) {
			stopService(context);
			startService(context);
		}
	}

}
