package com.example.anvi

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AnviAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ANVI will process screen/accessibility events here.
    }

    override fun onInterrupt() {
        // Called when Android interrupts the service.
    }

    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
}
