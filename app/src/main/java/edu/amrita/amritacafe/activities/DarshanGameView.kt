package edu.amrita.amritacafe.activities

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.preference.PreferenceManager
import edu.amrita.amritacafe.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DarshanGameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var ammaBitmap: Bitmap? = null
    private var scaledAmmaBitmap: Bitmap? = null
    
    // Amma Center Rect
    private var ammaRadius = 100f
    private var ammaRect = RectF()

    private val devotees = mutableListOf<Devotee>()
    private var score = 0
    private var highScore = 0
    private var isGameOver = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FF5252") // semi-transparent red
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 60f
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private var lastFrameTime = 0L
    private var spawnTimer = 0L
    private var spawnInterval = 3000L
    private var gameSpeed = 100f // pixels per second base speed

    private var isRunning = false
    private var isWaitingToStart = false
    private var selectedDevotee: Devotee? = null
    private var hasStartedDrawing = false

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }

    init {
        val options = BitmapFactory.Options()
        options.inSampleSize = 2
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.amma_face_splash, options)
        ammaBitmap = originalBitmap
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        highScore = prefs.getInt("darshan_flight_high_score", 0)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Amma in center
        val screenMin = Math.min(w, h)
        ammaRadius = screenMin * 0.15f
        
        ammaBitmap?.let { bmp ->
            // Keep aspect ratio
            val aspect = bmp.width.toFloat() / bmp.height.toFloat()
            val finalWidth: Float
            val finalHeight: Float
            if (aspect > 1) {
                finalWidth = ammaRadius * 2
                finalHeight = finalWidth / aspect
            } else {
                finalHeight = ammaRadius * 2
                finalWidth = finalHeight * aspect
            }
            
            scaledAmmaBitmap = Bitmap.createScaledBitmap(bmp, finalWidth.toInt(), finalHeight.toInt(), true)
            ammaRect.set(
                w / 2f - finalWidth / 2f,
                h / 2f - finalHeight / 2f,
                w / 2f + finalWidth / 2f,
                h / 2f + finalHeight / 2f
            )
        }
    }

    fun startGame() {
        score = 0
        devotees.clear()
        isGameOver = false
        spawnInterval = 3000L
        gameSpeed = 100f
        isRunning = false
        isWaitingToStart = true
        selectedDevotee = null
        hasStartedDrawing = false
        lastFrameTime = System.currentTimeMillis()
        postInvalidateOnAnimation()
    }

    fun stopGame() {
        isRunning = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#FFF3E0")) // Light orange background

        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastFrameTime) / 1000f
        lastFrameTime = currentTime

        if (isRunning) {
            updateGame(dt, currentTime)
        }

        // Draw drawn paths
        for (d in devotees) {
            if (d.pathPoints.isNotEmpty() && !d.isArrived) {
                val path = Path()
                path.moveTo(d.x, d.y)
                for (p in d.pathPoints) {
                    path.lineTo(p.x, p.y)
                }
                canvas.drawPath(path, pathPaint)
            }
        }

        // Draw Amma
        scaledAmmaBitmap?.let {
            canvas.drawBitmap(it, ammaRect.left, ammaRect.top, null)
        }

        // Draw Devotees
        paint.style = Paint.Style.FILL
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        for (d in devotees) {
            val alphaValue = if (d.isArrived) {
                val elapsed = currentTime - d.arrivalTime
                val a = 255 - (elapsed * 255 / 1500).toInt()
                a.coerceIn(0, 255)
            } else 255

            paint.color = if (d.isArrived) {
                Color.parseColor("#4CAF50") // Green smile
            } else if (d === selectedDevotee) {
                Color.parseColor("#FFD54F") // Yellow selected
            } else {
                Color.parseColor("#FF5252") // Red frown
            }
            paint.alpha = alphaValue
            canvas.drawCircle(d.x, d.y, d.radius, paint)

            // Eyes
            eyePaint.alpha = alphaValue
            canvas.drawCircle(d.x - 20f, d.y - 15f, 8f, eyePaint)
            canvas.drawCircle(d.x + 20f, d.y - 15f, 8f, eyePaint)

            // Mouth
            facePaint.alpha = alphaValue
            if (d.isArrived) {
                // Smile
                val mouthRect = RectF(d.x - 25f, d.y + 5f, d.x + 25f, d.y + 40f)
                canvas.drawArc(mouthRect, 0f, 180f, false, facePaint)
            } else {
                // Frown
                val frownRect = RectF(d.x - 25f, d.y + 15f, d.x + 25f, d.y + 40f)
                canvas.drawArc(frownRect, 180f, 180f, false, facePaint)
            }

            // Direction dot
            if (!d.isArrived) {
                paint.color = Color.WHITE
                paint.alpha = alphaValue
                canvas.drawCircle(d.x + cos(d.heading) * d.radius * 0.8f, d.y + sin(d.heading) * d.radius * 0.8f, 8f, paint)
            }
        }

        // Draw Score and High Score (Top Left)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 50f
        canvas.drawText("High: $highScore", 50f, 100f, textPaint)
        canvas.drawText("Score: $score", 50f, 160f, textPaint)

        // Draw Instructions
        if (isWaitingToStart) {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 55f
            canvas.drawText("Tap anywhere to start!", width / 2f, height / 2f - 200f, textPaint)
        } 
        
        if (!hasStartedDrawing && !isGameOver) {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 45f
            canvas.drawText("Drag the red devotees to Amma!", width / 2f, height - 150f, textPaint)
            canvas.drawText("Don't let them crash into each other!", width / 2f, height - 80f, textPaint)
        }

        if (isGameOver) {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 60f
            textPaint.color = Color.BLACK
            canvas.drawText("Oh no, EGO clash!!!", width / 2f, height / 2f - 150f, textPaint)
            textPaint.textSize = 50f
            canvas.drawText("Score: $score", width / 2f, height / 2f - 70f, textPaint)
            canvas.drawText("Tap to Restart", width / 2f, height / 2f + 150f, textPaint)
        }

        if (isRunning || isWaitingToStart) {
            postInvalidateOnAnimation()
        }
    }

    private fun updateGame(dt: Float, currentTime: Long) {
        // Spawn logic
        if (currentTime - spawnTimer > spawnInterval) {
            spawnTimer = currentTime
            spawnDevotee()
            // Increase difficulty slightly
            if (spawnInterval > 800L) spawnInterval -= 50L
            gameSpeed += 2f
        }

        // Move devotees
        val iterator = devotees.iterator()
        while (iterator.hasNext()) {
            val d = iterator.next()

            if (d.isArrived) {
                if (currentTime - d.arrivalTime > 1500) {
                    iterator.remove()
                }
                // float up slightly
                d.y -= 20f * dt
                continue
            }

            if (d.pathPoints.isNotEmpty()) {
                val target = d.pathPoints.first()
                val dist = distance(d.x, d.y, target.x, target.y)
                val moveDist = gameSpeed * dt

                if (dist <= moveDist) {
                    // Reached target point
                    d.x = target.x
                    d.y = target.y
                    d.pathPoints.removeAt(0)
                    if (d.pathPoints.isNotEmpty()) {
                        val nextTarget = d.pathPoints.first()
                        d.heading = atan2(nextTarget.y - d.y, nextTarget.x - d.x)
                    }
                } else {
                    d.heading = atan2(target.y - d.y, target.x - d.x)
                    d.x += cos(d.heading) * moveDist
                    d.y += sin(d.heading) * moveDist
                }
            } else {
                // No path drawn, keep moving straight in current heading
                d.x += cos(d.heading) * (gameSpeed * 0.7f) * dt
                d.y += sin(d.heading) * (gameSpeed * 0.7f) * dt
            }

            // Check Collision with Amma
            if (distance(d.x, d.y, width / 2f, height / 2f) < ammaRadius) {
                d.isArrived = true
                d.arrivalTime = currentTime
                score++
                if (score > highScore) {
                    highScore = score
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("darshan_flight_high_score", highScore).apply()
                }
                if (selectedDevotee === d) selectedDevotee = null
                continue
            }
            
            // Check Out of bounds (give some leeway)
            if (d.x < -100 || d.x > width + 100 || d.y < -100 || d.y > height + 100) {
                 iterator.remove()
                 if (selectedDevotee === d) selectedDevotee = null
                 continue
            }
        }

        // Check crash between devotees
        for (i in 0 until devotees.size) {
            for (j in i + 1 until devotees.size) {
                val d1 = devotees[i]
                val d2 = devotees[j]
                if (d1.isArrived || d2.isArrived) continue
                
                // Only allow crashes if BOTH devotees are completely visible on-screen
                val d1Visible = d1.x > d1.radius && d1.x < width - d1.radius && d1.y > d1.radius && d1.y < height - d1.radius
                val d2Visible = d2.x > d2.radius && d2.x < width - d2.radius && d2.y > d2.radius && d2.y < height - d2.radius
                if (!d1Visible || !d2Visible) continue

                if (distance(d1.x, d1.y, d2.x, d2.y) < d1.radius + d2.radius - 5f) { // 5f forgiveness
                    isGameOver = true
                    isRunning = false
                }
            }
        }
    }

    private fun spawnDevotee() {
        val radius = 70f // Doubled the size
        val edge = (0..3).random()
        var startX = 0f
        var startY = 0f
        when (edge) {
            0 -> { // Top
                startX = (0..width).random().toFloat()
                startY = -radius
            }
            1 -> { // Right
                startX = width + radius
                startY = (0..height).random().toFloat()
            }
            2 -> { // Bottom
                startX = (0..width).random().toFloat()
                startY = height + radius
            }
            3 -> { // Left
                startX = -radius
                startY = (0..height).random().toFloat()
            }
        }
        
        // Find a safe randomized heading
        val centerAngle = atan2(height / 2f - startY, width / 2f - startX)
        var bestHeading = centerAngle
        
        for (i in 0..4) {
            val testAngle = centerAngle + (Math.random() - 0.5) * Math.PI * 0.7
            var safe = true
            for (t in 1..3) { // Predict 1 to 3 seconds into the future
                val fx = startX + cos(testAngle) * gameSpeed * t
                val fy = startY + sin(testAngle) * gameSpeed * t
                for (other in devotees) {
                    if (other.isArrived) continue
                    // Very rough prediction for 'other', assuming they move straight
                    val ox = other.x + cos(other.heading) * gameSpeed * t
                    val oy = other.y + sin(other.heading) * gameSpeed * t
                    if (distance(fx.toFloat(), fy.toFloat(), ox.toFloat(), oy.toFloat()) < radius * 3f) {
                        safe = false
                        break
                    }
                }
                if (!safe) break
            }
            if (safe) {
                bestHeading = testAngle.toFloat()
                break
            }
        }

        devotees.add(Devotee(startX, startY, radius, bestHeading))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isWaitingToStart && event.action == MotionEvent.ACTION_DOWN) {
            isWaitingToStart = false
            isRunning = true
            lastFrameTime = System.currentTimeMillis()
            postInvalidateOnAnimation()
            return true
        }

        if (isGameOver && event.action == MotionEvent.ACTION_DOWN) {
            startGame()
            return true
        }

        if (isRunning) {
            val ex = event.x
            val ey = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Find if a devotee was tapped
                    selectedDevotee = devotees.find { distance(ex, ey, it.x, it.y) < it.radius * 2f } // slightly larger touch area
                    selectedDevotee?.pathPoints?.clear()
                }
                MotionEvent.ACTION_MOVE -> {
                    selectedDevotee?.let { d ->
                        hasStartedDrawing = true
                        val lastPoint = if (d.pathPoints.isNotEmpty()) d.pathPoints.last() else PointF(d.x, d.y)
                        if (distance(ex, ey, lastPoint.x, lastPoint.y) > 20f) {
                            d.pathPoints.add(PointF(ex, ey))
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    selectedDevotee = null
                }
            }
        }
        return true
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    class Devotee(var x: Float, var y: Float, val radius: Float, var heading: Float) {
        val pathPoints = mutableListOf<PointF>()
        var isArrived = false
        var arrivalTime = 0L
    }
}
