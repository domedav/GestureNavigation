package com.domedav.gesturenavigationonsteroids.checker;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;

public class BatteryCheckUtils {
	public static boolean isBatteryOptmizationsOff(@NonNull Context context){
		PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		var packageName = context.getPackageName();
		return powerManager.isIgnoringBatteryOptimizations(packageName);
	}
}
