package com.domedav.gesturenavigationonsteroids.checker;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.NonNull;
import com.domedav.gesturenavigationonsteroids.service.GestureAccessibilityService;
import java.util.List;

public class AccessibilityCheckUtils {
	public static boolean isAccessibilityEnabled(@NonNull Context context, String id) {
		AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
		List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
		for (AccessibilityServiceInfo service : runningServices) {
			if(id.equals(service.getId())) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isThreeButtonNavigationEnabled(@NonNull Context context) {
		String navigationMode = Settings.Secure.getString(context.getContentResolver(), "navigation_mode");
		return "0".equals(navigationMode) || "100".equals(navigationMode);
	}
	
	public static boolean callForceRepaint() {
		return GestureAccessibilityService.forceRepaint();
	}
}
