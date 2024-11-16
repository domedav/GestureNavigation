package com.domedav.gesturenavigationonsteroids.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.ColorUtils;
import com.domedav.gesturenavigationonsteroids.R;
import com.google.android.material.color.MaterialColors;

public class GestureAccessibilityService extends AccessibilityService implements UserStateCallback {
	
	@SuppressLint("StaticFieldLeak")
	private static GestureAccessibilityService instance = null;
	
	private static final int HEIGHT_IN_DP = 12;
	private static final int DELAY_FOR_SECONDARY_ACTIONS_MS = 380;
	private static final int TIME_TO_EXECUTE_SECONDARY_ACTIONS_MS = 300;
	
	private FrameLayout layout;
	private ImageView imageView;
	private UserReceiver userReceiver;
	
	private int[] beginPointerX;    private int[] beginPointerY;    private long beginTime;
	private int[] endPointerX;      private int[] endPointerY;      private long endTime;
	
	private int pointerExecutionCount;
	
	float[] deltaX; float[] deltaY;
	float[] velocityX; float[] velocityY;
	
	private final Handler handler = new Handler();
	private Runnable delayedRunnable;
	private Runnable colorChangeRunnable;
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {}
	
	@Override
	public void onInterrupt() {}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		forceRepaint();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		instance = this;
		
		userReceiver = new UserReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(Intent.ACTION_BOOT_COMPLETED);
		registerReceiver(userReceiver, filter);
		userReceiver.registerCallback(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		instance = null;
		unregisterReceiver(userReceiver);
	}
	
	@Override
	public void onServiceConnected() {
		super.onServiceConnected();
		createOverlay();
	}
	
	@Override
	public void onScreenOff() {
		removeOverlay();
	}
	
	@Override
	public void onScreenOn() {}
	
	public void onUserLogin(){
		createOverlay();
	}
	
	public void onBootComplete(){
		createOverlay();
	}
	
	public static boolean forceRepaint(){
		if(instance == null){
			return false;
		}
		instance.removeOverlay();
		instance.createOverlay();
		return true;
	}
	
	private boolean hasOverlay = false;
	
	@SuppressLint("ClickableViewAccessibility")
	private void createOverlay(){
		if(hasOverlay || layout != null){
			return;
		}
		hasOverlay = true;
		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		layout = new FrameLayout(getApplicationContext());
		layout.setBackgroundColor(Color.argb(0, 0, 0, 0));
		
		WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT
		);
		Display display = windowManager.getDefaultDisplay();
		Point screenSize = new Point();
		display.getSize(screenSize);
		layoutParams.width = screenSize.x;
		layoutParams.x = 0;
		
		float scale = getResources().getDisplayMetrics().density;
		layoutParams.height = (int) (HEIGHT_IN_DP * scale);
		
		String manufacturer = Build.MANUFACTURER.toLowerCase();
		
		boolean hasCustomNavigationBar = manufacturer.contains("huawei") || getNavigationBarOffset() != (48 * scale);
		
		if(hasCustomNavigationBar){
			layoutParams.y = -(int) (HEIGHT_IN_DP * scale);
		}
		else{
			layoutParams.y = getNavigationBarOffset() - (int)(HEIGHT_IN_DP * scale);
		}
		
		layoutParams.verticalMargin = 0f;
		
		layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
		layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		layoutParams.format = PixelFormat.TRANSPARENT;
		layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		
		try {
			windowManager.addView(layout, layoutParams);
		} catch (Exception ignored) {
			removeOverlay();
			return;
		}
		
		layout.setOnTouchListener((view, event) -> {
			onLayoutEvent(event);
			return false;
		});
		
		imageView = new ImageView(getApplicationContext());
		imageView.setImageDrawable(getBarDrawableByState(false));
		
		LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		imageParams.gravity = Gravity.CENTER_HORIZONTAL;
		imageParams.leftMargin = (int) (layoutParams.width * 0.35);
		imageParams.rightMargin = (int) (layoutParams.width * 0.35);
		imageParams.height = (int)(2.5 * scale);
		imageView.setLayoutParams(imageParams);
		imageView.setScaleType(ImageView.ScaleType.CENTER);
		
		layout.addView(imageView);
	}
	
	private void onLayoutEvent(@NonNull MotionEvent event){
		int action = event.getAction();
		float scale = getResources().getDisplayMetrics().density;
		float dpi = getResources().getDisplayMetrics().densityDpi;
		switch (action){
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				if(handler.hasCallbacks(delayedRunnable) || handler.hasCallbacks(colorChangeRunnable)){
					pointerExecutionCount++;
					if(imageView != null) imageView.setImageDrawable(getBarDrawableByState(false));
					if(delayedRunnable != null) handler.removeCallbacks(delayedRunnable);
					if(colorChangeRunnable != null) handler.removeCallbacks(colorChangeRunnable);
					delayedRunnable = null;
					colorChangeRunnable = null;
				}
				else{
					pointerExecutionCount = 1;
					colorChangeRunnable = () -> {
						if(imageView != null) imageView.setImageDrawable(getBarDrawableByState(true));
					};
					handler.postDelayed(colorChangeRunnable, DELAY_FOR_SECONDARY_ACTIONS_MS);
				}
				
				if(pointerExecutionCount == 1){
					beginPointerX = new int[1];
					beginPointerY = new int[1];
					endPointerX = new int[1];
					endPointerY = new int[1];
					
					deltaX = new float[1];
					deltaY = new float[1];
					velocityX = new float[1];
					velocityY = new float[1];
				}
				else {
					int[] tempBeginPointerX = new int[pointerExecutionCount];
					int[] tempBeginPointerY = new int[pointerExecutionCount];
					int[] tempendPointerX = new int[pointerExecutionCount];
					int[] tempendPointerY = new int[pointerExecutionCount];
					
					float[] tempdeltaX = new float[pointerExecutionCount];
					float[] tempdeltaY = new float[pointerExecutionCount];
					float[] tempvelocityX = new float[pointerExecutionCount];
					float[] tempvelocityY = new float[pointerExecutionCount];
					
					for (int i = 0; i < beginPointerX.length; i++) {
						tempBeginPointerX[i] = beginPointerX[i];
						tempBeginPointerY[i] = beginPointerY[i];
						tempendPointerX[i] = endPointerX[i];
						tempendPointerY[i] = endPointerY[i];
						
						tempdeltaX[i] = deltaX[i];
						tempdeltaY[i] = deltaY[i];
						tempvelocityX[i] = velocityX[i];
						tempvelocityY[i] = velocityY[i];
					}
					
					beginPointerX = tempBeginPointerX;
					beginPointerY = tempBeginPointerY;
					endPointerX = tempendPointerX;
					endPointerY = tempendPointerY;
					
					deltaX = tempdeltaX;
					deltaY = tempdeltaY;
					velocityX = tempvelocityX;
					velocityY = tempvelocityY;
				}
				
				beginPointerX[pointerExecutionCount - 1] = (int)event.getRawX(0);
				beginPointerY[pointerExecutionCount - 1] = (int)event.getRawY(0);
				
				beginTime = event.getEventTime();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				endPointerX[pointerExecutionCount - 1] = (int)event.getRawX(0);
				endPointerY[pointerExecutionCount - 1] = (int)event.getRawY(0);
				
				endTime = event.getEventTime();
				
				deltaX[pointerExecutionCount - 1] = (beginPointerX[pointerExecutionCount - 1] - endPointerX[pointerExecutionCount - 1]) * scale;
				deltaY[pointerExecutionCount - 1] = (beginPointerY[pointerExecutionCount - 1] - endPointerY[pointerExecutionCount - 1]) * scale;
				
				float deltaTimeSec = (float)(endTime - beginTime) / 1000f;
				
				velocityX[pointerExecutionCount - 1] = Math.abs(deltaX[pointerExecutionCount - 1] / deltaTimeSec) * (2.54f / dpi);
				velocityY[pointerExecutionCount - 1] = Math.abs(deltaY[pointerExecutionCount - 1] / deltaTimeSec) * (2.54f / dpi);
				
				delayedRunnable = () -> handleAction(deltaX, deltaY, velocityX, velocityY);
				handler.postDelayed(delayedRunnable, deltaTimeSec >= (DELAY_FOR_SECONDARY_ACTIONS_MS / 1000f) ? TIME_TO_EXECUTE_SECONDARY_ACTIONS_MS : 0);
				break;
			default:
				break;
		}
	}
	
	private void handleAction(float[] deltaX, float[] deltaY, float[] velocityX, float[] velocityY){
		if(handler.hasCallbacks(delayedRunnable) || handler.hasCallbacks(colorChangeRunnable)){
			if(delayedRunnable != null) handler.removeCallbacks(delayedRunnable);
			if(colorChangeRunnable != null) handler.removeCallbacks(colorChangeRunnable);
			delayedRunnable = null;
			colorChangeRunnable = null;
		}
		if(imageView != null) imageView.setImageDrawable(getBarDrawableByState(false));
		if(pointerExecutionCount == 1){
			//single pointer events
			if(Math.abs(deltaX[0]) < Math.abs(deltaY[0])){
				if(deltaY[0] > 25 && velocityY[0] < 10){
					performGlobalAction(GLOBAL_ACTION_RECENTS);
				}
				else if(deltaY[0] > 25 && velocityY[0] >= 10){
					performGlobalAction(GLOBAL_ACTION_HOME);
				}
				else if(deltaY[0] < -10){
					performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
				}
			}
			else{
				if((deltaX[0] > 100 || deltaX[0] < -100) && velocityX[0] < 15){
					performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
				}
				else if((deltaX[0] > 25 || deltaX[0] < -25) && velocityX[0] >= 15){
					performGlobalAction(GLOBAL_ACTION_BACK);
				}
			}
		} else if (pointerExecutionCount == 2) {
			// two pointer events
			if(deltaY[1] > 25 && velocityY[1] >= 10){
				Intent intent = new Intent();
				intent.setAction("android.media.action.STILL_IMAGE_CAMERA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				try {
					startActivity(intent);
				} catch (Exception ignored) {
				}
			}
			else if(deltaY[1] < -10 && deltaY[0] > 25){
				performGlobalAction(GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS);
			}
		}
	}
	
	private void removeOverlay() {
		if(!hasOverlay){
			return;
		}
		hasOverlay = false;
		if(handler.hasCallbacks(delayedRunnable) || handler.hasCallbacks(colorChangeRunnable)){
			if(delayedRunnable != null) handler.removeCallbacks(delayedRunnable);
			if(colorChangeRunnable != null) handler.removeCallbacks(colorChangeRunnable);
			delayedRunnable = null;
			colorChangeRunnable = null;
		}
		if(imageView != null) imageView.setImageDrawable(getBarDrawableByState(false));
		if (layout != null) {
			WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
			try {
				windowManager.removeView(layout);
				layout = null;
			} catch (Exception ignored) {
			}
		}
	}
	
	private int getNavigationBarOffset() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			WindowInsets insets = ((WindowManager) getSystemService(WINDOW_SERVICE)).getCurrentWindowMetrics().getWindowInsets();
			Insets navBarInsets = insets.getInsets(WindowInsets.Type.navigationBars());
			return navBarInsets.bottom;
		} else {
			@SuppressLint({"DiscouragedApi", "InternalInsetResource"})
			int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
			return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
		}
	}
	
	@NonNull
	private Drawable getBarDrawableByState(boolean isActive){
		int color;
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R && isActive){
			ContextThemeWrapper wrapper = new ContextThemeWrapper(getApplicationContext(), R.style.Theme_GestureNavigation_MaterialYou);
			color = MaterialColors.getColor(wrapper, com.google.android.material.R.attr.colorPrimaryVariant, getResources().getColor(R.color.bar_color_active, getTheme()));
		}
		else{
			TypedValue outValue = new TypedValue();
			ContextThemeWrapper wrapper = new ContextThemeWrapper(getApplicationContext(), R.style.Theme_GestureNavigation);
			wrapper.getTheme().resolveAttribute(isActive ? R.attr.barColorActive : R.attr.barColor, outValue, true);
			color = outValue.data;
		}
		color = ColorUtils.setAlphaComponent(color, isActive ? 0x77 : 0x5F);
		return new ColorDrawable(color);
	}
}
