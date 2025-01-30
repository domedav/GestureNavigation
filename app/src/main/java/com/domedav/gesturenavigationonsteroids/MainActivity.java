package com.domedav.gesturenavigationonsteroids;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.domedav.gesturenavigationonsteroids.checker.AccessibilityCheckUtils;
import com.domedav.gesturenavigationonsteroids.checker.BatteryCheckUtils;
import com.domedav.gesturenavigationonsteroids.service.GestureAccessibilityService;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_main);
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});
		DynamicColors.applyToActivitiesIfAvailable(getApplication());
		
		findViewById(R.id.accessibility_grant_popup_button).setOnClickListener(v -> {
			Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplicationContext().startActivity(intent);
		});
		
		findViewById(R.id.navigation_wrong_popup_button).setOnClickListener(v -> {
			showMaterialDialog(
					getResources().getString(R.string.main_toast_manual_settings_navigation_header),
					getResources().getString(R.string.main_toast_manual_settings_navigation_description),
					getResources().getString(R.string.main_toast_positive_button),
					getResources().getString(R.string.main_toast_negative_button),
					() -> {
						Intent intent = new Intent(Settings.ACTION_SETTINGS);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_toast_manual_settings_navigation_description), Toast.LENGTH_LONG).show();
					},
					() -> {}
			);
		});
		
		findViewById(R.id.battery_unoptimal_button).setOnClickListener(v -> {
			Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplicationContext().startActivity(intent);
		});
		
		findViewById(R.id.main_allgood_displaybar_button).setOnClickListener(v -> {
			if(AccessibilityCheckUtils.callForceRepaint()){
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_allgood_summary_text_button_result_toast), Toast.LENGTH_SHORT).show();
				return;
			}
			showMaterialDialog(
					getResources().getString(R.string.main_relaunch_service_header),
					getResources().getString(R.string.main_relaunch_service_description),
					getResources().getString(R.string.main_relaunch_service_positive_button),
					getResources().getString(R.string.main_relaunch_service_negative_button),
					() -> {
						Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_relaunch_service_description_later), Toast.LENGTH_LONG).show();
					},
					() -> {}
			);
		});
		
		findViewById(R.id.testing).setOnClickListener(v -> {
			showMaterialDialog(
					"getResources().getString(R.string.main_relaunch_service_header)",
					"getResources().getString(R.string.main_relaunch_service_description)",
					getResources().getString(R.string.main_relaunch_service_positive_button),
					getResources().getString(R.string.main_relaunch_service_negative_button),
					() -> {},
					() -> {}
			);
		});
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
			// android 12 has the "Rubber Band" overscroll
			// this is purely cosmetic
			findViewById(R.id.main_scrollview).setOverScrollMode(View.OVER_SCROLL_ALWAYS);
		}
		
		setupUI();
	}
	
	protected void onNewIntent(@NonNull Intent intent){
		super.onNewIntent(intent);
		setupUI();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus){
		super.onWindowFocusChanged(hasFocus);
		setupUI();
	}
	
	private boolean hasEnabledAccessibilityService() {
		String serviceId = getApplicationContext().getPackageName() + "/.service.GestureAccessibilityService";
		return AccessibilityCheckUtils.isAccessibilityEnabled(getApplicationContext(), serviceId);
	}
	
	private boolean hasEnabledThreeButtonNavigation(){
		return AccessibilityCheckUtils.isThreeButtonNavigationEnabled(getApplicationContext());
	}
	
	private boolean hasTurnedOffBatteryOptimizations(){
		return BatteryCheckUtils.isBatteryOptmizationsOff(getApplicationContext());
	}
	
	private void checkAbleToProceed(){
		if(hasEnabledAccessibilityService() && hasEnabledThreeButtonNavigation() && hasTurnedOffBatteryOptimizations()){
			Intent intent = new Intent(getApplicationContext(), GestureAccessibilityService.class);
			startService(intent);
		}
	}
	
	private void setVisibilityOfAccessibilityMissingPopup(boolean visible) {
		findViewById(R.id.accessibility_grant_popup).setVisibility(!visible ? View.VISIBLE : View.GONE);
	}
	
	private void setVisibilityOfThreeButtonNavigationPopup(boolean visible) {
		findViewById(R.id.navigation_wrong_popup).setVisibility(!visible ? View.VISIBLE : View.GONE);
	}
	
	private void setVisibilityOfBatteryOptimizationPopup(boolean visible) {
		findViewById(R.id.navigation_batteryopotimize_popup).setVisibility(!visible ? View.VISIBLE : View.GONE);
	}
	
	private void setVisibilityOfMainContent(boolean visible){
		findViewById(R.id.main_issues_popups_notifier).setVisibility(!visible ? View.VISIBLE : View.GONE);
		
		findViewById(R.id.main_noissues_popups_notifier).setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	private void setupUI(){
		setVisibilityOfAccessibilityMissingPopup(hasEnabledAccessibilityService());
		setVisibilityOfThreeButtonNavigationPopup(hasEnabledThreeButtonNavigation());
		setVisibilityOfBatteryOptimizationPopup(hasTurnedOffBatteryOptimizations());
		setVisibilityOfMainContent(hasEnabledAccessibilityService() && hasEnabledThreeButtonNavigation());
		checkAbleToProceed();
	}
	
	private void showMaterialDialog(String header, String description, String positiveButtonText, String negativeButtonText, VoidCallback onPositiveButtonClick, VoidCallback onNegativeButtonClick){
		new MaterialAlertDialogBuilder(this, new ContextThemeWrapper(getApplicationContext(), R.style.Theme_GestureNavigation_MaterialYou_Alert).getThemeResId())
				.setTitle(header)
				.setMessage(description)
				.setCancelable(true)
				.setPositiveButton(positiveButtonText, (dialog, which) -> {
					onPositiveButtonClick.invoke();
				})
				.setNegativeButton(negativeButtonText, (dialog, which) -> {
					onNegativeButtonClick.invoke();
					dialog.dismiss();
				})
				.show();
	}
}