package com.domedav.gesturenavigationonsteroids.service;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import java.util.Objects;

public class UserReceiver extends BroadcastReceiver {
	private UserStateCallback callback;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
			callback.onScreenOff();
		} else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
			callback.onScreenOn();
		} else if(Objects.equals(intent.getAction(), Intent.ACTION_USER_PRESENT)){
			callback.onUserLogin();
		} else if(Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)){
			callback.onBootComplete();
		}
	}
	
	public void registerCallback(UserStateCallback callback){
		this.callback = callback;
	}
}