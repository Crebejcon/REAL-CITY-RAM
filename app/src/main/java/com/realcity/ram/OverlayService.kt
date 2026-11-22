package com.realcity.ram

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var crosshair: CrosshairView? = null
    private var crosshairOn = true
    private var crosshairSize = 22
    private var crosshairColor = Color.RED
    private val channelId = "real_city_ram_overlay"

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "REAL CITY RAM Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pending = PendingIntent.getActivity(this, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        startForeground(41, builder.setContentTitle("REAL CITY RAM ativo")
            .setContentText("Painel flutuante disponível sobre o jogo")
            .setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pending).setOngoing(true).build())
        showCrosshair()
        showBubble()
    }

    private fun type() = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
    private fun lp(w:Int,h:Int,flags:Int,gravity:Int=Gravity.TOP or Gravity.START) =
        WindowManager.LayoutParams(w,h,type(),flags,PixelFormat.TRANSLUCENT).apply { this.gravity=gravity }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private fun showBubble() {
        if (bubble != null) return
        val b=TextView(this).apply {
            text="RAM"; textSize=12f; setTextColor(Color.WHITE); gravity=Gravity.CENTER
            background=GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(Color.rgb(190,25,35)); setStroke(dp(2),Color.WHITE) }
            elevation=dp(8).toFloat()
        }
        val params=lp(dp(56),dp(56),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN).apply { x=dp(12); y=dp(180) }
        var downX=0f; var downY=0f; var oldX=0; var oldY=0; var moved=false
        b.setOnTouchListener { _,e ->
            when(e.action) {
                MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; oldX=params.x; oldY=params.y; moved=false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx=(e.rawX-downX).toInt(); val dy=(e.rawY-downY).toInt()
                    if(abs(dx)+abs(dy)>dp(5)) moved=true
                    params.x=oldX+dx; params.y=oldY+dy; wm.updateViewLayout(b,params); true
                }
                MotionEvent.ACTION_UP -> { if(!moved) togglePanel(); true }
                else -> true
            }
        }
        bubble=b; wm.addView(b,params)
    }

    private fun togglePanel() {
        if(panel!=null) { removePanel(); return }
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(12))
            background=GradientDrawable().apply { setColor(Color.rgb(15,18,26)); cornerRadius=dp(14).toFloat(); setStroke(dp(1),Color.rgb(210,40,48)) }
            elevation=dp(12).toFloat()
        }
        val head=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply { text="REAL CITY RAM  ⋮"; textSize=16f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER_VERTICAL }
        head.addView(title, LinearLayout.LayoutParams(0,-2,1f))
        head.addView(TextView(this).apply {
            text="×"; textSize=24f; setTextColor(Color.WHITE); gravity=Gravity.CENTER
            setOnClickListener { removePanel() }
        },LinearLayout.LayoutParams(dp(36),dp(36)))
        box.addView(head)
        box.addView(TextView(this).apply { text="PAINEL FLUTUANTE • REAL CITY BETA"; textSize=9f; setTextColor(Color.LTGRAY) })
        box.addView(separator())
        box.addView(TextView(this).apply {
            text="ASSISTÊNCIA DE TREINO"; textSize=10f; setTextColor(Color.LTGRAY); typeface=Typeface.DEFAULT_BOLD
        })
        box.addView(Switch(this).apply {
            text="Aim Assist (somente treino)"; setTextColor(Color.WHITE)
            val prefs=getSharedPreferences("ram_settings_v2",Context.MODE_PRIVATE)
            isChecked=prefs.getBoolean("aim_enabled",false)
            setOnCheckedChangeListener { _,v -> prefs.edit().putBoolean("aim_enabled",v).apply() }
        })
        box.addView(TextView(this).apply {
            text="AMIGOS / CONFIGURAÇÕES"; textSize=12f; gravity=Gravity.CENTER; setTextColor(Color.WHITE)
            setPadding(dp(8),dp(12),dp(8),dp(12))
            background=GradientDrawable().apply { setColor(Color.rgb(42,48,62)); cornerRadius=dp(8).toFloat() }
            setOnClickListener {
                val i=Intent(this@OverlayService,MainActivity::class.java).putExtra("open_tab","AMIGOS")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(i)
            }
        })
        box.addView(TextView(this).apply {
            text="ABRIR TREINAMENTO"; textSize=12f; gravity=Gravity.CENTER; setTextColor(Color.WHITE)
            setPadding(dp(8),dp(12),dp(8),dp(12))
            background=GradientDrawable().apply { setColor(Color.rgb(42,48,62)); cornerRadius=dp(8).toFloat() }
            setOnClickListener {
                val i=Intent(this@OverlayService,MainActivity::class.java).putExtra("open_tab","AIM")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(i)
            }
        })
        box.addView(separator())
        box.addView(Switch(this).apply {
            text="Crosshair na tela"; setTextColor(Color.WHITE); isChecked=crosshairOn
            setOnCheckedChangeListener { _,v -> crosshairOn=v; crosshair?.invalidate() }
        })
        box.addView(TextView(this).apply { text="Tamanho do crosshair"; textSize=11f; setTextColor(Color.WHITE) })
        box.addView(SeekBar(this).apply {
            max=50; progress=crosshairSize-8
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean) { crosshairSize=v+8; crosshair?.invalidate() }
                override fun onStartTrackingTouch(s:SeekBar?) {}
                override fun onStopTrackingTouch(s:SeekBar?) {}
            })
        })
        box.addView(TextView(this).apply { text="Cor do crosshair"; textSize=11f; setTextColor(Color.WHITE) })
        val colors=LinearLayout(this).apply { gravity=Gravity.CENTER }
        listOf(Color.RED,Color.GREEN,Color.CYAN,Color.WHITE,Color.YELLOW).forEach { color ->
            colors.addView(TextView(this).apply {
                text="●"; textSize=26f; gravity=Gravity.CENTER; setTextColor(color)
                setOnClickListener { crosshairColor=color; crosshair?.invalidate() }
            },LinearLayout.LayoutParams(0,dp(42),1f))
        }
        box.addView(colors); box.addView(separator())
        box.addView(TextView(this).apply {
            text="Arraste pelo título para mover o painel. O botão RAM abre e minimiza este menu."
            textSize=10f; setTextColor(Color.LTGRAY)
        })
        val closeOverlay = TextView(this).apply {
            text="DESLIGAR SOBREPOSIÇÃO"; textSize=12f; typeface=Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); gravity=Gravity.CENTER; setPadding(dp(8),dp(12),dp(8),dp(12))
            background=GradientDrawable().apply { setColor(Color.rgb(175,25,35)); cornerRadius=dp(9).toFloat() }
            setOnClickListener { stopSelf() }
        }
        box.addView(closeOverlay, LinearLayout.LayoutParams(-1,dp(44)).apply { topMargin=dp(8) })

        val panelParams=lp(dp(280),WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { y=dp(110) }
        var startX=0f; var startY=0f; var baseX=0; var baseY=0
        title.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> { startX=event.rawX; startY=event.rawY; baseX=panelParams.x; baseY=panelParams.y; true }
                MotionEvent.ACTION_MOVE -> {
                    panelParams.x=baseX+(event.rawX-startX).toInt()
                    panelParams.y=(baseY+(event.rawY-startY).toInt()).coerceAtLeast(0)
                    try { wm.updateViewLayout(box,panelParams) } catch (_:Exception) {}
                    true
                }
                else -> true
            }
        }
        panel=box
        wm.addView(box,panelParams)
    }

    private fun separator()=View(this).apply {
        setBackgroundColor(Color.rgb(55,60,72))
        layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply { topMargin=dp(8); bottomMargin=dp(8) }
    }

    private fun showCrosshair() {
        if(crosshair!=null) return
        val view=CrosshairView(this) { Triple(crosshairOn,crosshairSize,crosshairColor) }
        crosshair=view
        wm.addView(view,lp(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS))
    }

    private fun removePanel() {
        panel?.let { try { wm.removeView(it) } catch (_:Exception) {} }
        panel=null
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int)=START_STICKY
    override fun onBind(intent:Intent?):IBinder?=null

    override fun onDestroy() {
        removePanel()
        listOf(bubble,crosshair).forEach { view -> view?.let { try { wm.removeView(it) } catch (_:Exception) {} } }
        bubble=null; crosshair=null
        super.onDestroy()
    }

    private class CrosshairView(ctx:Context,private val state:()->Triple<Boolean,Int,Int>):View(ctx) {
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
        override fun onDraw(canvas:Canvas) {
            super.onDraw(canvas)
            val s=state(); if(!s.first) return
            val r=dp(s.second).toFloat(); val x=width/2f; val y=height/2f
            paint.style=Paint.Style.STROKE; paint.strokeWidth=dp(4).toFloat(); paint.color=Color.BLACK
            canvas.drawLine(x-r,y,x-r/3,y,paint); canvas.drawLine(x+r/3,y,x+r,y,paint)
            canvas.drawLine(x,y-r,x,y-r/3,paint); canvas.drawLine(x,y+r/3,x,y+r,paint)
            paint.strokeWidth=dp(2).toFloat(); paint.color=s.third
            canvas.drawLine(x-r,y,x-r/3,y,paint); canvas.drawLine(x+r/3,y,x+r,y,paint)
            canvas.drawLine(x,y-r,x,y-r/3,paint); canvas.drawLine(x,y+r/3,x,y+r,paint)
            paint.style=Paint.Style.FILL; canvas.drawCircle(x,y,dp(2).toFloat(),paint)
        }
    }
}