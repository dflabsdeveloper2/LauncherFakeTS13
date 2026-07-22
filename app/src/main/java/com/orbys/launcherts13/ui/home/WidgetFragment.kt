package com.orbys.launcherts13.ui.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.SizeF
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.orbys.launcherts13.R
import com.orbys.launcherts13.databinding.FragmentWidgetBinding
import com.orbys.launcherts13.domain.model.WidgetInfo
import com.orbys.launcherts13.util.viewBinding
import kotlinx.coroutines.launch
import kotlin.math.abs

class WidgetFragment : Fragment() {

    private val binding by viewBinding(FragmentWidgetBinding::bind)
    private val viewModel: MainViewModel by activityViewModels()

    private val appWidgetHost: AppWidgetHost by lazy {
        object : AppWidgetHost(requireContext(), WIDGET_HOST_ID) {
            override fun onCreateView(
                context: android.content.Context,
                appWidgetId: Int,
                appWidget: android.appwidget.AppWidgetProviderInfo?
            ): android.appwidget.AppWidgetHostView {
                return object : android.appwidget.AppWidgetHostView(requireContext().applicationContext) {
                    override fun updateAppWidget(remoteViews: android.widget.RemoteViews?) {
                        if (remoteViews == null) return
                        runCatching { super.updateAppWidget(remoteViews) }
                    }
                }
            }
        }
    }
    private val appWidgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(requireContext()) }
    private val widgetEditExiters = mutableListOf<() -> Unit>()

    /** appWidgetId del widget actualmente en modo edición (move/resize), o null si ninguno. */
    private var editingWidgetId: Int? = null

    companion object {
        private const val WIDGET_HOST_ID = 1337
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_widget, container, false)
    }

    // Tocar una zona vacía de la capa de widgets deselecciona el widget en edición, igual que
    // el gridGestureDetector de DesktopFragment.
    private val emptySpaceGestureDetector by lazy {
        GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                exitEditMode()
                return true
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.widgetsContainer.setOnTouchListener { _, event ->
            emptySpaceGestureDetector.onTouchEvent(event)
            false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    rebuildWidgets(state.widgets)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // El servidor de AppWidgetService puede devolver RemoteException (p. ej. NPE interno si
        // algún widget de este host quedó huérfano tras desinstalar su proveedor) — sin este
        // runCatching, ese fallo del sistema tumba toda la Activity. Ver mismo tratamiento en
        // DesktopFragment.onPageShown()/onPageHidden().
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { appWidgetHost.stopListening() }
    }

    override fun onPause() {
        super.onPause()
        exitEditMode()
    }

    fun exitEditMode() {
        widgetEditExiters.forEach { it() }
        widgetEditExiters.clear()
        editingWidgetId = null
    }

    /** Ver DesktopFragment.isEditing(): MainActivity lo consulta para no confundir un arrastre
     * en modo edición con un swipe de cambio de página. */
    fun isEditing(): Boolean = editingWidgetId != null

    fun obtainAppWidgetHost() = appWidgetHost

    private fun rebuildWidgets(widgets: List<WidgetInfo>) {
        exitEditMode()
        binding.widgetsContainer.removeAllViews()
        for (widget in widgets) {
            buildWidgetCard(widget)?.let { binding.widgetsContainer.addView(it) }
        }
    }

    private fun removeFailedWidget(id: Int) {
        viewModel.removeWidget(id)
        Toast.makeText(requireContext(), R.string.widget_load_failed, Toast.LENGTH_SHORT).show()
    }

    private fun buildWidgetCard(widget: WidgetInfo): FrameLayout? {
        val info = appWidgetManager.getAppWidgetInfo(widget.appWidgetId) ?: run {
            removeFailedWidget(widget.appWidgetId)
            return null
        }

        val widgetView = try {
            appWidgetHost.createView(requireContext(), widget.appWidgetId, info)
        } catch (e: Exception) {
            removeFailedWidget(widget.appWidgetId)
            return null
        }
        widgetView.updateAppWidgetSize(Bundle(), listOf(SizeF(widget.widthDp.toFloat(), widget.heightDp.toFloat())))
        widgetView.setPadding(0, 0, 0, 0)

        var startTouchX = 0f
        var startTouchY = 0f
        var startCardX  = 0f
        var startCardY  = 0f
        var dragging        = false
        var moveMode        = false
        var resizeMode      = false
        var longPressFired  = false
        var resizeTouching  = false
        var editOverlay: View? = null
        var longPressDetector: GestureDetector? = null

        val card = object : FrameLayout(requireContext()) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                // Si se toca este widget mientras OTRO está en modo edición, ese otro se cierra
                // primero (commit de su move/resize en curso) y este toque se procesa desde cero
                // como un gesto normal -- un tap corto aquí es un click normal, NO selecciona
                // este widget también (solo un long-press hace eso, ver longPressDetector).
                if (ev.action == MotionEvent.ACTION_DOWN && editingWidgetId != null && editingWidgetId != widget.appWidgetId) {
                    exitEditMode()
                }
                longPressDetector?.onTouchEvent(ev)
                if (longPressFired) {
                    return when (ev.action) {
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            longPressFired = false
                            val cancel = MotionEvent.obtain(ev).also { it.action = MotionEvent.ACTION_CANCEL }
                            val r = super.dispatchTouchEvent(cancel)
                            cancel.recycle()
                            r
                        }
                        else -> true
                    }
                }
                return super.dispatchTouchEvent(ev)
            }

            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                if (moveMode && !resizeTouching) return
                super.requestDisallowInterceptTouchEvent(disallowIntercept)
            }

            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startTouchX = ev.rawX
                        startTouchY = ev.rawY
                        startCardX  = x
                        startCardY  = y
                        dragging    = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (moveMode && !resizeTouching && !dragging &&
                            (abs(ev.rawX - startTouchX) > dpToPx(8) ||
                             abs(ev.rawY - startTouchY) > dpToPx(8))
                        ) {
                            dragging = true
                        }
                        if (dragging) return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (dragging) { dragging = false; return true }
                    }
                }
                return false
            }

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> if (moveMode) return true
                    MotionEvent.ACTION_MOVE -> {
                        if (moveMode && !dragging &&
                            (abs(ev.rawX - startTouchX) > dpToPx(8) ||
                             abs(ev.rawY - startTouchY) > dpToPx(8))
                        ) {
                            dragging = true
                        }
                        if (dragging) {
                            x = startCardX + (ev.rawX - startTouchX)
                            y = startCardY + (ev.rawY - startTouchY)
                            elevation = dpToPx(12).toFloat()
                            alpha = 0.85f
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                        viewModel.updateWidgetPosition(
                        widget.appWidgetId, pxToDp(x.toInt()), pxToDp(y.toInt())
                    )
                        elevation = 0f
                        alpha = 1f
                        dragging = false
                        return true
                    }
                }
                return super.onTouchEvent(ev)
            }
        }.apply {
            background = null
            val radius = dpToPx(16).toFloat()
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
            clipToOutline = true
            elevation    = 0f
            layoutParams = FrameLayout.LayoutParams(dpToPx(widget.widthDp), dpToPx(widget.heightDp))
            x = dpToPx(widget.xDp).toFloat()
            y = dpToPx(widget.yDp).toFloat()
        }

        card.addView(widgetView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val editFrame = FrameLayout(requireContext()).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dpToPx(2), requireContext().getColor(R.color.widget_border_edit))
                cornerRadius = dpToPx(16).toFloat()
            }
            visibility = View.GONE
        }

        val actionBar = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(36), Gravity.TOP)
        }

        val btnConfirm = TextView(requireContext()).apply {
            text = "✓"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(requireContext().getColor(R.color.widget_btn_confirm_bg))
                cornerRadius = dpToPx(4).toFloat()
            }
            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnDelete = TextView(requireContext()).apply {
            text = "✕"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(requireContext().getColor(R.color.widget_btn_delete_bg))
                cornerRadius = dpToPx(4).toFloat()
            }
            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val spacer = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        actionBar.addView(btnConfirm)
        actionBar.addView(spacer)
        actionBar.addView(btnDelete)
        editFrame.addView(actionBar)
        editOverlay = editFrame

        // Un tirador por cada borde -- arriba/abajo para el alto, izquierda/derecha para el
        // ancho -- en vez de un único icono de esquina, cada uno con un punto del mismo color
        // que el borde de edición (ver setupWidgetResize, que restringe el eje por separado).
        // Construcción/posición del tirador (idéntica a DesktopFragment) en ResizeHandleFactory.
        val resizeHandleTop = ResizeHandleFactory.buildDot(requireContext())
        val resizeHandleBottom = ResizeHandleFactory.buildDot(requireContext())
        val resizeHandleLeft = ResizeHandleFactory.buildDot(requireContext())
        val resizeHandleRight = ResizeHandleFactory.buildDot(requireContext())
        val resizeHandles = listOf(resizeHandleTop, resizeHandleBottom, resizeHandleLeft, resizeHandleRight)

        // Los tiradores van ANTES que editFrame en el árbol: editFrame (con los botones
        // confirmar/borrar) se dibuja encima y gana el toque donde su actionBar se solapa con
        // la tira TOP (que cubre todo el borde, ver ResizeHandleFactory).
        card.addView(resizeHandleTop, ResizeHandleFactory.layoutParams(requireContext(), ResizeEdge.TOP))
        card.addView(resizeHandleBottom, ResizeHandleFactory.layoutParams(requireContext(), ResizeEdge.BOTTOM))
        card.addView(resizeHandleLeft, ResizeHandleFactory.layoutParams(requireContext(), ResizeEdge.LEFT))
        card.addView(resizeHandleRight, ResizeHandleFactory.layoutParams(requireContext(), ResizeEdge.RIGHT))

        // El inset deja sitio para que los tiradores de resize (centrados sobre esta línea, ver
        // ResizeHandleFactory) quepan enteros dentro de la card sin sobresalir.
        val editFrameInset = ResizeHandleFactory.editFrameInsetPx(requireContext())
        card.addView(
            editFrame,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                setMargins(editFrameInset, editFrameInset, editFrameInset, editFrameInset)
            }
        )

        btnConfirm.setOnClickListener { exitEditMode() }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_widget_title)
                .setMessage(R.string.dialog_delete_widget_msg)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.removeWidget(widget.appWidgetId)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        longPressDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onLongPress(e: MotionEvent) {
                if (moveMode || resizeMode) return
                longPressFired = true
                exitEditMode()
                card.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                moveMode = true
                resizeMode = true
                editingWidgetId = widget.appWidgetId
                resizeHandles.forEach { it.visibility = View.VISIBLE }
                editOverlay.visibility = View.VISIBLE
            }
        })

        setupWidgetResize(card, resizeHandleTop, widget.appWidgetId, widgetView, ResizeEdge.TOP,
            setResizeTouching = { resizeTouching = it })
        setupWidgetResize(card, resizeHandleBottom, widget.appWidgetId, widgetView, ResizeEdge.BOTTOM,
            setResizeTouching = { resizeTouching = it })
        setupWidgetResize(card, resizeHandleLeft, widget.appWidgetId, widgetView, ResizeEdge.LEFT,
            setResizeTouching = { resizeTouching = it })
        setupWidgetResize(card, resizeHandleRight, widget.appWidgetId, widgetView, ResizeEdge.RIGHT,
            setResizeTouching = { resizeTouching = it })

        widgetEditExiters.add {
            if (!moveMode && !resizeMode) return@add
            moveMode = false
            resizeMode = false
            resizeHandles.forEach { it.visibility = View.GONE }
            editOverlay.visibility = View.GONE
            card.elevation = 0f
            card.alpha = 1f
            if (editingWidgetId == widget.appWidgetId) editingWidgetId = null
            viewModel.updateWidgetPosition(widget.appWidgetId, pxToDp(card.x.toInt()), pxToDp(card.y.toInt()))
            (card.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                viewModel.updateWidgetSize(widget.appWidgetId, pxToDp(lp.width), pxToDp(lp.height))
            }
        }

        return card
    }

    /** TOP/LEFT anclan el borde opuesto y mueven la posición; BOTTOM/RIGHT solo crecen, como antes. */
    private fun setupWidgetResize(
        card: FrameLayout,
        handle: View,
        widgetId: Int,
        widgetView: android.appwidget.AppWidgetHostView,
        edge: ResizeEdge,
        setResizeTouching: (Boolean) -> Unit
    ) {
        var startX    = 0f
        var startY    = 0f
        var startW    = 0
        var startH    = 0
        var startCardX = 0f
        var startCardY = 0f
        var currentW  = 0
        var currentH  = 0

        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setResizeTouching(true)
                    startX   = event.rawX
                    startY   = event.rawY
                    startW   = card.width
                    startH   = card.height
                    startCardX = card.x
                    startCardY = card.y
                    currentW = startW
                    currentH = startH
                    handle.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // LEFT/TOP anclan el borde opuesto: crecen hacia su propio lado moviendo
                    // también la posición de la card, en vez de solo el tamaño (RIGHT/BOTTOM).
                    when (edge) {
                        ResizeEdge.RIGHT -> {
                            currentW = (startW + (event.rawX - startX).toInt()).coerceAtLeast(dpToPx(80))
                        }
                        ResizeEdge.LEFT -> {
                            val newW = (startW - (event.rawX - startX).toInt()).coerceAtLeast(dpToPx(80))
                            currentW = newW
                            card.x = startCardX + startW - newW
                        }
                        ResizeEdge.BOTTOM -> {
                            currentH = (startH + (event.rawY - startY).toInt()).coerceAtLeast(dpToPx(80))
                        }
                        ResizeEdge.TOP -> {
                            val newH = (startH - (event.rawY - startY).toInt()).coerceAtLeast(dpToPx(80))
                            currentH = newH
                            card.y = startCardY + startH - newH
                        }
                    }
                    (card.layoutParams as FrameLayout.LayoutParams).width  = currentW
                    (card.layoutParams as FrameLayout.LayoutParams).height = currentH
                    val wSpec = View.MeasureSpec.makeMeasureSpec(currentW, View.MeasureSpec.EXACTLY)
                    val hSpec = View.MeasureSpec.makeMeasureSpec(currentH, View.MeasureSpec.EXACTLY)
                    card.measure(wSpec, hSpec)
                    card.layout(card.left, card.top, card.left + currentW, card.top + currentH)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    setResizeTouching(false)
                    handle.parent?.requestDisallowInterceptTouchEvent(false)
                    val wDp = pxToDp(currentW)
                    val hDp = pxToDp(currentH)
                    viewModel.updateWidgetSize(widgetId, wDp, hDp)
                    viewModel.updateWidgetPosition(widgetId, pxToDp(card.x.toInt()), pxToDp(card.y.toInt()))
                    widgetView.updateAppWidgetSize(Bundle(), listOf(SizeF(wDp.toFloat(), hDp.toFloat())))
                    true
                }
                else -> false
            }
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
    private fun pxToDp(px: Int) = (px / resources.displayMetrics.density).toInt()
}
