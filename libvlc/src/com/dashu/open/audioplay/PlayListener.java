package com.dashu.open.audioplay;

public interface PlayListener {
	public void onException(int code, String message);
	public void onTimeChanged(long time);
	public void onPositionChanged(float pos);
	public void onPlaying();
	public void onStopped();
	public void onPaused();
	public void onTitleChanged(String title);
}
