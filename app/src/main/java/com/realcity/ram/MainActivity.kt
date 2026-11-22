package com.realcity.ram

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.*
import android.os.Debug
import kotlin.math.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("ram_settings_v2", Context.MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var activeTab = "VISUAL"
    private val red = Color.rgb(229,57,53)
    private val bg = Color.rgb(9,11,16)
    private val panel = Color.rgb(20,24,33)
    private val muted = Color.rgb(155,164,178)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=bg; window.navigationBarColor=bg
        buildShell(); showTab()
        applyScreenSetting()
    }
    override fun onResume() { super.onResume(); applyScreenSetting() }
    private fun applyScreenSetting() {
        if(prefs.getBoolean("s_awake",false)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun buildShell() {
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(12),dp(10),dp(12),dp(8)) }
        val header=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        header.addView(TextView(this).apply { text="REAL CITY RAM"; textSize=23f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD) })
        header.addView(TextView(this).apply { text="V2 • ANDROID TRAINING & DIAGNOSTICS"; textSize=10f; setTextColor(red); setPadding(0,dp(3),0,dp(10)) })
        root.addView(header)
        val tabs=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        listOf("VISUAL","AIM","PLAYER","SETTINGS","DEBUG").forEach { t ->
            tabs.addView(TextView(this).apply {
                text=t; textSize=9f; gravity=Gravity.CENTER; setTextColor(if(t==activeTab) Color.WHITE else muted)
                setPadding(dp(4),dp(12),dp(4),dp(12)); setBackgroundColor(if(t==activeTab) red else panel)
                setOnClickListener { activeTab=t; showTab() }
            },LinearLayout.LayoutParams(0,dp(40),1f))
        }
        root.addView(tabs)
        val scroll=ScrollView(this)
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,dp(10),0,dp(18)) }
        scroll.addView(content); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply { text="SIMULADOR LOCAL • SEM CONEXÃO COM REAL CITY"; textSize=9f; gravity=Gravity.CENTER; setTextColor(muted); setPadding(0,dp(7),0,0) })
        setContentView(root)
    }
    private fun showTab() {
        content.removeAllViews()
        when(activeTab) {
            "VISUAL" -> visualTab()
            "AIM" -> aimTab()
            "PLAYER" -> playerTab()
            "SETTINGS" -> settingsTab()
            else -> debugTab()
        }
        val tabs=root.getChildAt(1) as LinearLayout
        for(i in 0 until tabs.childCount) {
            val v=tabs.getChildAt(i) as TextView
            v.setBackgroundColor(if(v.text.toString()==activeTab) red else panel)
            v.setTextColor(if(v.text.toString()==activeTab) Color.WHITE else muted)
        }
    }
    private fun section(t:String,d:String="") {
        content.addView(TextView(this).apply { text=t; textSize=16f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); setPadding(0,dp(7),0,dp(3)) })
        if(d.isNotBlank()) content.addView(TextView(this).apply { text=d; textSize=10f; setTextColor(muted); setPadding(0,0,0,dp(8)) })
    }
    private fun card():LinearLayout {
        val c=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(11),dp(9),dp(11),dp(9)); setBackgroundColor(panel) }
        content.addView(c,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) }); return c
    }
    private fun toggle(p:LinearLayout,label:String,key:String,default:Boolean=false,desc:String="") {
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val texts=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply { text=label; textSize=12f; setTextColor(Color.WHITE) })
        if(desc.isNotBlank()) texts.addView(TextView(this).apply { text=desc; textSize=9f; setTextColor(muted) })
        row.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
        row.addView(Switch(this).apply { isChecked=prefs.getBoolean(key,default); setOnCheckedChangeListener { _,v -> prefs.edit().putBoolean(key,v).apply(); if(key=="s_awake") applyScreenSetting() } })
        p.addView(row)
        p.addView(View(this).apply { setBackgroundColor(Color.rgb(42,47,58)); layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply { topMargin=dp(6); bottomMargin=dp(6) } })
    }
    private fun slider(p:LinearLayout,label:String,key:String,min:Int,max:Int,default:Int,suffix:String="") {
        val title=TextView(this).apply { text="$label: ${prefs.getInt(key,default)}$suffix"; textSize=11f; setTextColor(Color.WHITE) }
        p.addView(title)
        p.addView(SeekBar(this).apply {
            this.max=max-min; progress=prefs.getInt(key,default).coerceIn(min,max)-min
            progressTintList=android.content.res.ColorStateList.valueOf(red); thumbTintList=android.content.res.ColorStateList.valueOf(red)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean) { val n=v+min; prefs.edit().putInt(key,n).apply(); title.text="$label: $n$suffix" }
                override fun onStartTrackingTouch(s:SeekBar?) {}
                override fun onStopTrackingTouch(s:SeekBar?) {}
            })
        })
    }
    private fun visualTab() {
        section("VISUAL LAB","Todos os desenhos atuam apenas nos três alvos fictícios desta tela.")
        val c=card()
        toggle(c,"2D Box","v_box",true); toggle(c,"Corner Box","v_corner",true)
        toggle(c,"Skeleton ESP","v_skeleton",true); toggle(c,"Name & Distance","v_name",true)
        toggle(c,"Health Bar","v_health",true); toggle(c,"Tracers","v_tracer",false)
        toggle(c,"Highlight","v_highlight",true); toggle(c,"Team Filter","v_team",false,"Oculta o alvo marcado como aliado.")
        slider(c,"Distância máxima","v_distance",10,300,150," m")
        slider(c,"Opacidade do destaque","v_alpha",10,100,65,"%")
        content.addView(TrainingView(this,prefs,false),LinearLayout.LayoutParams(-1,dp(310)))
        content.addView(note("Alvos e distâncias são simulados. O filtro de equipe usa uma classificação fictícia."))
    }
    private fun aimTab() {
        section("AIM TRAINING","Pratique seleção e acompanhamento de alvos dentro do simulador.")
        val c=card()
        toggle(c,"Aim Assist do treinamento","aim_enabled",false)
        toggle(c,"FOV Circle","aim_fov",true)
        toggle(c,"Line of Sight (simulado)","aim_los",true,"Alvos atrás de um obstáculo fictício são ignorados.")
        slider(c,"FOV","aim_fov_size",30,300,120," px")
        slider(c,"Smoothness","aim_smooth",1,100,35,"%")
        slider(c,"Distância máxima","aim_range",10,300,120," m")
        val group=RadioGroup(this).apply { orientation=RadioGroup.HORIZONTAL }
        listOf("Cabeça","Peito","Pernas").forEachIndexed { i,s ->
            val r=RadioButton(this).apply { text=s; textSize=10f; setTextColor(Color.WHITE); id=100+i }
            group.addView(r); if(prefs.getInt("aim_part",1)==i) r.isChecked=true
        }
        group.setOnCheckedChangeListener { _,id -> prefs.edit().putInt("aim_part",id-100).apply() }
        c.addView(TextView(this).apply { text="PONTO DE TREINO"; setTextColor(muted); textSize=10f })
        c.addView(group)
        val training=TrainingView(this,prefs,true)
        content.addView(training,LinearLayout.LayoutParams(-1,dp(370)))
        content.addView(Button(this).apply { text="REINICIAR TREINO"; setOnClickListener { training.resetTargets() } })
        content.addView(note("Aim Assist move somente o retículo dentro desta área. Não envia toques nem controla outros aplicativos."))
    }
    private fun playerTab() {
        section("PLAYER MONITOR","Dados gerados localmente para NPCs fictícios.")
        val c=card()
        toggle(c,"Health Monitor","p_health",true)
        toggle(c,"NPC Identification","p_npc",true)
        toggle(c,"Position Debug","p_position",true)
        toggle(c,"Distance Monitor","p_distance",true)
        val status=TextView(this).apply { textSize=12f; setTextColor(Color.WHITE); setPadding(0,dp(8),0,0) }
        c.addView(status)
        status.text="Alvo A • NPC de treino • 100 HP • (25, 28)\\nAlvo B • NPC de treino • 80 HP • (72, 38)\\nAlvo C • Aliado simulado • 65 HP • (52, 62)"
    }
    private fun settingsTab() {
        section("SETTINGS","Preferências gravadas no armazenamento privado do app.")
        val c=card()
        toggle(c,"Vibração ao acertar alvo","s_haptic",false)
        toggle(c,"Baixo consumo visual","s_low",true,"Reduz animação contínua quando Aim Assist está desligado.")
        toggle(c,"Manter tela ativa durante treino","s_awake",false)
        content.addView(Button(this).apply { text="RESTAURAR CONFIGURAÇÕES"; setOnClickListener { prefs.edit().clear().apply(); applyScreenSetting(); showTab() } })
    }
    private fun debugTab() {
        section("DEBUG TOOLS","Métricas do processo deste aplicativo; não são métricas do Real City.")
        val c=card()
        val metrics=TextView(this).apply { textSize=12f; setTextColor(Color.WHITE) }; c.addView(metrics)
        fun refresh() {
            val r=Runtime.getRuntime()
            metrics.text="Heap Java usado: ${(r.totalMemory()-r.freeMemory())/1048576} MB\\nHeap Java máximo: ${r.maxMemory()/1048576} MB\\nHeap nativo alocado: ${Debug.getNativeHeapAllocatedSize()/1048576} MB\\nModelo: ${android.os.Build.MODEL}\\nAndroid: ${android.os.Build.VERSION.RELEASE}\\nABI: ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "N/D"}"
        }
        refresh(); content.addView(Button(this).apply { text="ATUALIZAR MÉTRICAS"; setOnClickListener { refresh() } })
    }
    private fun note(s:String)=TextView(this).apply { text=s; textSize=10f; setTextColor(muted); setPadding(dp(2),dp(7),dp(2),dp(4)) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}

private class TrainingView(ctx:Context,private val prefs:android.content.SharedPreferences,private val aim:Boolean):View(ctx) {
    private data class Dummy(var x:Float,var y:Float,var distance:Int,var hp:Int,var ally:Boolean,var blocked:Boolean)
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val red=Color.rgb(229,57,53)
    private val targets= mutableListOf<Dummy>()
    private var reticle=PointF(.5f,.84f)
    private var score=0
    init { resetTargets(); isClickable=true }
    fun resetTargets() {
        targets.clear()
        targets.add(Dummy(.25f,.30f,35,100,false,false))
        targets.add(Dummy(.72f,.40f,80,80,false,true))
        targets.add(Dummy(.52f,.62f,125,65,true,false))
        invalidate()
    }
    override fun onDraw(c:Canvas) {
        super.onDraw(c); c.drawColor(Color.rgb(13,17,25))
        p.style=Paint.Style.STROKE; p.strokeWidth=1f; p.color=Color.rgb(38,45,58)
        for(i in 1..5)c.drawLine(width*i/6f,0f,width*i/6f,height.toFloat(),p)
        for(i in 1..4)c.drawLine(0f,height*i/5f,width.toFloat(),height*i/5f,p)
        if(aim && prefs.getBoolean("aim_fov",true)) { p.color=red;p.strokeWidth=2f;c.drawCircle(reticle.x*width,reticle.y*height,prefs.getInt("aim_fov_size",120).toFloat(),p) }
        val maxDist=prefs.getInt("v_distance",150)
        targets.forEachIndexed { i,t ->
            if(!aim && t.distance>maxDist) return@forEachIndexed
            if(!aim && prefs.getBoolean("v_team",false) && t.ally) return@forEachIndexed
            val x=t.x*width; val y=t.y*height
            val color=if(t.ally) Color.rgb(70,160,220) else if(t.blocked) Color.GRAY else Color.rgb(80,205,150)
            if(!aim && prefs.getBoolean("v_tracer",false)) { p.color=red;p.strokeWidth=1.5f;c.drawLine(width/2f,height.toFloat(),x,y,p) }
            if(!aim && prefs.getBoolean("v_highlight",true)) { p.style=Paint.Style.FILL;p.color=Color.argb(prefs.getInt("v_alpha",65)*255/100,red shr 16 and 255,red shr 8 and 255,red and 255);c.drawCircle(x,y,dp(26).toFloat(),p) }
            p.style=Paint.Style.STROKE;p.strokeWidth=2.5f;p.color=color
            c.drawCircle(x,y,dp(15).toFloat(),p)
            c.drawLine(x-dp(20),y,x+dp(20),y,p);c.drawLine(x,y-dp(20),x,y+dp(20),p)
            if(!aim && prefs.getBoolean("v_box",true)) c.drawRect(x-dp(22),y-dp(30),x+dp(22),y+dp(30),p)
            if(!aim && prefs.getBoolean("v_corner",false)) drawCorners(c,x,y)
            if(!aim && prefs.getBoolean("v_skeleton",false)) drawSkeleton(c,x,y)
            if(!aim && prefs.getBoolean("v_name",true)) { p.style=Paint.Style.FILL;p.textSize=dp(10).toFloat();p.color=Color.WHITE;c.drawText("${if(t.ally)"ALLY" else "TEST NPC"} ${i+1} • ${t.distance}m",x-dp(43),y-dp(39),p) }
            if(!aim && prefs.getBoolean("v_health",true)) { p.color=Color.DKGRAY;p.strokeWidth=dp(4).toFloat();c.drawLine(x-dp(21),y+dp(37),x+dp(21),y+dp(37),p);p.color=Color.GREEN;c.drawLine(x-dp(21),y+dp(37),x-dp(21)+dp(42)*t.hp/100f,y+dp(37),p) }
            if(t.blocked && aim) { p.style=Paint.Style.FILL;p.color=Color.LTGRAY;p.textSize=dp(9).toFloat();c.drawText("OBSTÁCULO",x-dp(30),y-dp(28),p) }
        }
        p.style=Paint.Style.FILL;p.color=Color.WHITE;p.textSize=dp(11).toFloat()
        c.drawText(if(aim)"TOQUE NO ALVO • PONTOS: $score" else "PREVIEW • ALVOS FICTÍCIOS",dp(8).toFloat(),height-dp(10).toFloat(),p)
        if(aim && prefs.getBoolean("aim_enabled",false)) {
            val candidates=targets.filter { it.distance<=prefs.getInt("aim_range",120) && !(prefs.getBoolean("aim_los",true)&&it.blocked) }
            val nearest=candidates.minByOrNull { hypot((it.x-reticle.x)*width,(it.y-reticle.y)*height) }
            if(nearest!=null) {
                val part=prefs.getInt("aim_part",1)
                val targetY=nearest.y + when(part){0->-.035f;2->.045f;else->0f}
                val tx=nearest.x*width; val ty=targetY*height
                val dx=tx-reticle.x*width;val dy=ty-reticle.y*height
                val fov=prefs.getInt("aim_fov_size",120)
                if(hypot(dx,dy)<=fov) {
                    val smooth=prefs.getInt("aim_smooth",35).coerceIn(1,100)/100f
                    reticle.x+=(nearest.x-reticle.x)*smooth*.08f
                    reticle.y+=(targetY-reticle.y)*smooth*.08f
                    postInvalidateOnAnimation()
                }
            }
        }
        if(aim) { p.style=Paint.Style.STROKE;p.color=Color.WHITE;p.strokeWidth=2f;c.drawCircle(reticle.x*width,reticle.y*height,dp(8).toFloat(),p);c.drawLine(reticle.x*width-dp(13),reticle.y*height,reticle.x*width+dp(13),reticle.y*height,p);c.drawLine(reticle.x*width,reticle.y*height-dp(13),reticle.x*width,reticle.y*height+dp(13),p) }
    }
    private fun drawCorners(c:Canvas,x:Float,y:Float) {
        val w=dp(25).toFloat();val h=dp(34).toFloat();val l=dp(9).toFloat()
        p.color=Color.YELLOW;p.strokeWidth=dp(3).toFloat()
        c.drawLine(x-w,y-h,x-w+l,y-h,p);c.drawLine(x-w,y-h,x-w,y-h+l,p)
        c.drawLine(x+w,y-h,x+w-l,y-h,p);c.drawLine(x+w,y-h,x+w,y-h+l,p)
        c.drawLine(x-w,y+h,x-w+l,y+h,p);c.drawLine(x-w,y+h,x-w,y+h-l,p)
        c.drawLine(x+w,y+h,x+w-l,y+h,p);c.drawLine(x+w,y+h,x+w,y+h-l,p)
    }
    private fun drawSkeleton(c:Canvas,x:Float,y:Float) {
        p.color=Color.CYAN;p.strokeWidth=dp(2).toFloat()
        val head=y-dp(22);val chest=y-dp(5);val hip=y+dp(13)
        c.drawLine(x,head,x,chest,p);c.drawLine(x,chest,x,hip,p)
        c.drawLine(x,chest,x-dp(14),y+dp(4),p);c.drawLine(x,chest,x+dp(14),y+dp(4),p)
        c.drawLine(x,hip,x-dp(10),y+dp(28),p);c.drawLine(x,hip,x+dp(10),y+dp(28),p)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        if(e.action==MotionEvent.ACTION_DOWN) {
            if(aim) {
                val hit=targets.indexOfFirst { hypot(it.x*width-e.x,it.y*height-e.y)<dp(32) }
                if(hit>=0) {
                    score++; val t=targets[hit]
                    t.x=(.15f+Math.random()*.7).toFloat();t.y=(.2f+Math.random()*.55).toFloat()
                    if(prefs.getBoolean("s_haptic",false)) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } else reticle=PointF(e.x/width,e.y/height)
            }
            invalidate();return true
        }
        return true
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
