package com.domedav.gesturenavigationonsteroids.service;

public interface UserStateCallback{
	void onBootComplete();
	void onScreenOff();
	void onScreenOn();
	void onUserLogin();
}
