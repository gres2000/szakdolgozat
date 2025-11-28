package com.taskraze.myapplication.view.overlay_widget

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view.main.MainActivity

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var rootView: LinearLayout
    private lateinit var bubbleIcon: ImageView
    private lateinit var optionsContainer: LinearLayout

    private var isLeftSide = true

    override fun onCreate() {
        super.onCreate()

        startForeground(1, createNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        rootView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_layout, null) as LinearLayout

        bubbleIcon = rootView.findViewById(R.id.bubbleIcon)
        optionsContainer = rootView.findViewById(R.id.optionsContainer)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        windowManager.addView(rootView, params)

        setupDrag(params)
        setupBubbleClick()
        setupOptionClicks()
    }

    private fun setupBubbleClick() {
        bubbleIcon.setOnClickListener {
            // Animate bubble
            bubbleIcon.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(100)
                .withEndAction {
                    bubbleIcon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()

            if (optionsContainer.visibility == View.VISIBLE)
                optionsContainer.visibility = View.GONE
            else
                optionsContainer.visibility = View.VISIBLE
        }
    }
    private fun setupDrag(params: WindowManager.LayoutParams) {
        bubbleIcon.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            var isClick = true

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {

                    MotionEvent.ACTION_DOWN -> {
                        isClick = true
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()

                        // if moved enough, treat as drag
                        if (dx * dx + dy * dy > 10) isClick = false  // small threshold

                        params.x = startX + dx
                        params.y = startY + dy
                        windowManager.updateViewLayout(rootView, params)

                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            v?.performClick()
                        } else {
                            snapToEdge(params)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }


    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val width = resources.displayMetrics.widthPixels
        val midpoint = width / 2

        if (params.x < midpoint) {
            params.x = 0
            isLeftSide = true
        } else {
            params.x = width
            isLeftSide = false
        }

        windowManager.updateViewLayout(rootView, params)
    }

    private fun setupOptionClicks() {
        optionsContainer.findViewById<View>(R.id.option1).setOnClickListener { launchOption(1) }
        optionsContainer.findViewById<View>(R.id.option2).setOnClickListener { launchOption(2) }
        optionsContainer.findViewById<View>(R.id.option3).setOnClickListener { launchOption(3) }
    }

    private fun launchOption(option: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when(option) {
                1 -> putExtra("navigateTo", "tasks")
                2 -> putExtra("navigateTo", "home")
                3 -> putExtra("navigateTo", "calendar")
            }
        }
        startActivity(intent)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val channelId = "overlay_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Overlay", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText("Floating bubble running")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::rootView.isInitialized) windowManager.removeView(rootView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
