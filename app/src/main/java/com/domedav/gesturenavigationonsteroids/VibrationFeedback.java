package com.domedav.gesturenavigationonsteroids;
import static androidx.core.content.ContextCompat.getSystemService;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibrationFeedback {
	private static final int SHORT_MS = 100;
	private final Vibrator vibrator;
	private boolean canVibrate = false;
	
	public VibrationFeedback(Context context){
		vibrator = getSystemService(context, Vibrator.class);
		if(vibrator == null){
			return;
		}
		canVibrate = vibrator.hasVibrator() && vibrator.hasAmplitudeControl();
	}
	
	public void vibrateShort(){
		if(!canVibrate){
			return;
		}
		vibrator.vibrate(VibrationEffect.createOneShot(SHORT_MS, VibrationEffect.EFFECT_CLICK));
	}
}
