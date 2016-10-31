package com.dashu.open.audioplay;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telecom.ConnectionService;
import android.util.Log;

public class StreamPlayer {
	public static final String TAG = "Dashu";

	public static final int StreamPlayerError = -1;
	public static final int StreamPlayerPlaying = 0x104;
	public static final int StreamPlayerPaused = 0x105;
	public static final int StreamPlayerStopped = 0x106;
	public static final int StreamPlayerTitle = 0x107;
	public static final int StreamPlayerPosition = 0x108;
	public static final int StreamPlayerTime = 0x109;

	private Context context;
	private Handler handler;

	private PlayService service;

	public StreamPlayer(Context context, Handler handler) {
		this.context = context;
		this.handler = handler;
	}

	private PlayListener listener = new PlayListener() {

		@Override
		public void onException(int code, String message) {
			Message msg = new Message();
			msg.what = StreamPlayerError;
			msg.arg1 = code;
			msg.obj = message;

			if (null != handler)
				handler.sendMessage(msg);
		}

		@SuppressLint("UseValueOf")
		@Override
		public void onTimeChanged(long time) {
			Message msg = new Message();
			msg.what = StreamPlayerTime;
			msg.obj = new Long(time);
			if (null != handler)
				handler.sendMessage(msg);
		}

		@SuppressLint("UseValueOf")
		@Override
		public void onPositionChanged(float pos) {
			Message msg = new Message();
			msg.what = StreamPlayerPosition;
			msg.obj = new Float(pos);
			if (null != handler)
				handler.sendMessage(msg);
		}

		@Override
		public void onPlaying() {
			if (null != handler)
				handler.sendEmptyMessage(StreamPlayerPlaying);
		}

		@Override
		public void onStopped() {
			if (null != handler)
				handler.sendEmptyMessage(StreamPlayerStopped);
		}

		@Override
		public void onPaused() {
			if (null != handler)
				handler.sendEmptyMessage(StreamPlayerPaused);
		}

		@Override
		public void onTitleChanged(String title) {
			Message msg = new Message();
			msg.what = StreamPlayerTitle;
			msg.obj = title;
			if (null != handler)
				handler.sendMessage(msg);
		}
	};

	public void stop() {
		if (null != service && service.isPlaying()) {
			
			service.stop();
			service = null;
			
			this.context.unbindService(serviceConnection);
		}
	}
	
	private String url;
	ServiceConnection serviceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name,
				IBinder iBinder) {
			Log.i(TAG, "StreamPlayer --> onConnected");

			service = PlayService.getService(iBinder);
			
			service.play(url, listener);
			
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Log.i(TAG, "StreamPlayer --> onDisconnected");
			
		}};

	public void start(final String url) {
		Log.i(TAG, "Start in StreamPlayer");
		this.url = url;
		stop();
		context.bindService(new Intent(context, PlayService.class),
				serviceConnection,
				Context.BIND_AUTO_CREATE);
	}
}
