package com.kitsugi.animelist.data.cloudstream

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper

/**
 * A custom Context wrapper for Cloudstream plugins that:
 *  1. Spoofs package name to "com.lagradost.cloudstream3" to satisfy plugin-side security checks.
 *  2. Wraps a real AppCompatActivity context so that Dialogs and Windows can be created successfully.
 *  3. Intercepts Dialog DecorViews shown via WindowManager to inject custom styling (premium dark/teal),
 *     hide unnecessary default title panels, and add a direct premium close button.
 */
class CsPluginContextWrapper(
    private val baseActivity: AppCompatActivity,
    themeResId: Int
) : ContextThemeWrapper(baseActivity, themeResId) {

    override fun getPackageName(): String {
        return "com.lagradost.cloudstream3"
    }

    override fun getSystemService(name: String): Any? {
        if (name == Context.WINDOW_SERVICE) {
            val baseWM = baseActivity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return KitsugiWindowManagerWrapper(baseWM)
        }
        // Redirect standard services to the activity context to prevent issues with themed inflaters
        try {
            val activityService = baseActivity.getSystemService(name)
            if (activityService != null) return activityService
        } catch (_: Exception) {}
        return super.getSystemService(name)
    }

    inner class KitsugiWindowManagerWrapper(private val base: WindowManager) : WindowManager by base {
        override fun addView(view: View, params: ViewGroup.LayoutParams) {
            if (view.javaClass.simpleName.contains("DecorView", ignoreCase = true)) {
                val decorView = view as? ViewGroup
                if (decorView != null) {
                    // Apply immediate styling modifications
                    applyPremiumDialogStyle(decorView)

                    // Post a runnable to customize components once layout is measured
                    decorView.post {
                        try {
                            setupDialogInterception(decorView)
                        } catch (e: Exception) {
                            android.util.Log.e("CsPluginContext", "Failed to customize plugin dialog: ${e.message}", e)
                        }
                    }
                }
            }
            base.addView(view, params)
        }
    }

    private fun applyPremiumDialogStyle(decorView: ViewGroup) {
        // Set rounded corners and premium dark background to the window content
        try {
            // Find parentPanel which contains the actual dialog card
            val parentPanel = decorView.findViewById<View>(androidx.appcompat.R.id.parentPanel) as? ViewGroup
            if (parentPanel != null) {
                val backgroundDrawable = GradientDrawable().apply {
                    setColor(Color.parseColor("#10131D")) // Kitsugi Surface
                    cornerRadius = 16 * baseActivity.resources.displayMetrics.density // 16dp
                    setStroke((1.5f * baseActivity.resources.displayMetrics.density).toInt(), Color.parseColor("#1A1F2C")) // Subtle Border
                }
                parentPanel.background = backgroundDrawable
                
                // Add margins around parentPanel to keep it clean on all screen sizes
                val lp = parentPanel.layoutParams as? FrameLayout.LayoutParams
                if (lp != null) {
                    val margin = (16 * baseActivity.resources.displayMetrics.density).toInt()
                    lp.setMargins(margin, margin, margin, margin)
                    parentPanel.layoutParams = lp
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CsPluginContext", "Could not apply premium card shape: ${e.message}")
        }
    }

    private fun setupDialogInterception(decorView: ViewGroup) {
        // Find panels in the standard AlertDialog hierarchy
        val contentPanel = decorView.findViewById<View>(androidx.appcompat.R.id.contentPanel)
        val customPanel = decorView.findViewById<View>(androidx.appcompat.R.id.customPanel)
        val parentPanel = decorView.findViewById<View>(androidx.appcompat.R.id.parentPanel) as? ViewGroup
        val topPanel = decorView.findViewById<View>(androidx.appcompat.R.id.topPanel)
        val titleView = decorView.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)

        // Hide title panel if there is no title text
        if (titleView == null || titleView.text.isNullOrBlank()) {
            topPanel?.visibility = View.GONE
        }

        // Hide the default divider if present
        val titleDivider = decorView.findViewById<View>(androidx.appcompat.R.id.titleDividerNoCustom)
        titleDivider?.visibility = View.GONE

        // Hide empty button panels if they contain no active buttons to clean up spacing
        val buttonPanel = decorView.findViewById<View>(androidx.appcompat.R.id.buttonPanel)
        val btnPositive = decorView.findViewById<View>(android.R.id.button1)
        val btnNegative = decorView.findViewById<View>(android.R.id.button2)
        val btnNeutral = decorView.findViewById<View>(android.R.id.button3)
        val hasButtons = (btnPositive?.visibility == View.VISIBLE) || 
                         (btnNegative?.visibility == View.VISIBLE) || 
                         (btnNeutral?.visibility == View.VISIBLE)
        if (!hasButtons) {
            buttonPanel?.visibility = View.GONE
        }

        // Add a premium close button to the top-right of the dialog
        val targetContainer = parentPanel ?: (decorView.findViewById<View>(android.R.id.content) as? ViewGroup) ?: decorView
        val existingCloseBtn = targetContainer.findViewWithTag<View>("Kitsugi_dialog_close_button")
        
        if (existingCloseBtn == null) {
            val closeButton = ImageButton(baseActivity).apply {
                tag = "Kitsugi_dialog_close_button"
                
                // Use default AppCompat close resource
                setImageResource(androidx.appcompat.R.drawable.abc_ic_clear_material)
                setBackgroundColor(Color.TRANSPARENT)
                
                // Premium Kitsugi colors: Teal Accent tint (#C8F4EF)
                setColorFilter(Color.parseColor("#C8F4EF"))
                
                // Clean touch targets
                val padding = (10 * baseActivity.resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
                
                setOnClickListener {
                    // Send a back key event directly to the DecorView to safely dismiss the dialog
                    decorView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
                    decorView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
                }
            }

            // Size and layout layoutParams
            val size = (38 * baseActivity.resources.displayMetrics.density).toInt()
            val layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = (10 * baseActivity.resources.displayMetrics.density).toInt()
                rightMargin = (10 * baseActivity.resources.displayMetrics.density).toInt()
            }

            if (targetContainer is FrameLayout) {
                targetContainer.addView(closeButton, layoutParams)
            } else {
                targetContainer.addView(closeButton)
            }
        }
    }
}
