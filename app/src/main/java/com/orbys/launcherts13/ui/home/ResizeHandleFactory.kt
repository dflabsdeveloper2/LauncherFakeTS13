package com.orbys.launcherts13.ui.home

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.orbys.launcherts13.R

/** TOP/LEFT crecen hacia arriba/izquierda (ancla el borde opuesto); BOTTOM/RIGHT crecen hacia abajo/derecha (ancla arriba/izquierda). */
enum class ResizeEdge { TOP, BOTTOM, LEFT, RIGHT }

/**
 * Tirador de resize compartido entre DesktopFragment (grid-snap) y WidgetFragment (free-form):
 * un punto por borde, con objetivo táctil a lo largo de todo el borde (no solo el punto visual),
 * centrado sobre la línea del editFrame. Qué eje mueve cada borde y cómo se traduce el arrastre
 * en tamaño/posición vive en cada fragment (difiere: grid columnas/filas vs. píxeles libres);
 * esto solo construye y posiciona la parte visual/táctil, idéntica en ambos.
 */
object ResizeHandleFactory {

    // Grosor del objetivo táctil de cada tirador y separación del editFrame respecto al borde
    // real de la card. EDIT_FRAME_INSET_DP debe ser >= la mitad de RESIZE_HANDLE_THICKNESS_DP
    // para que el tirador (centrado sobre la línea del editFrame) quepa entero dentro de la
    // card sin sobresalir.
    const val RESIZE_HANDLE_THICKNESS_DP = 14
    const val EDIT_FRAME_INSET_DP = 13

    // Corrección empírica: el trazo de editFrame (GradientDrawable.setStroke) no se renderiza
    // centrado exactamente sobre EDIT_FRAME_INSET_DP, sino un poco más hacia el borde real de
    // la card. Sin esto el punto queda visiblemente más hacia afuera que la línea azul.
    private const val DOT_VISUAL_CORRECTION_DP = 1

    /** La mitad del grosor del tirador: mínimo de insetDp para que quepa entero sin sobresalir. */
    const val MIN_EDIT_FRAME_INSET_DP = RESIZE_HANDLE_THICKNESS_DP / 2

    private fun dpToPx(context: Context, dp: Int) = (dp * context.resources.displayMetrics.density).toInt()

    fun editFrameInsetPx(context: Context, insetDp: Int = EDIT_FRAME_INSET_DP) = dpToPx(context, insetDp)

    fun buildDot(context: Context): View {
        val dot = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColor(R.color.widget_border_edit))
            }
        }
        return FrameLayout(context).apply {
            addView(dot, FrameLayout.LayoutParams(dpToPx(context, 10), dpToPx(context, 10), Gravity.CENTER))
            visibility = View.GONE
        }
    }

    // El objetivo táctil cubre todo el largo del borde (MATCH_PARENT), no solo el punto visual:
    // tocar en cualquier parte del borde de la card debe arrastrar como si se tocara el punto.
    // El grosor se centra sobre la línea del editFrame (a insetDp del borde real), no sobre el
    // borde real de la card, para que el punto visual quede justo encima de la línea que el
    // usuario ve. insetDp es opcional (por defecto EDIT_FRAME_INSET_DP): DesktopFragment lo
    // recorta por card para que el borde nunca quede por encima del propio widget (ver
    // effectiveContentInsetDp/effectiveEditFrameInsetDp en DesktopFragment).
    fun layoutParams(context: Context, edge: ResizeEdge, insetDp: Int = EDIT_FRAME_INSET_DP): FrameLayout.LayoutParams {
        val thickness = dpToPx(context, RESIZE_HANDLE_THICKNESS_DP)
        val margin = editFrameInsetPx(context, insetDp) + dpToPx(context, DOT_VISUAL_CORRECTION_DP) - thickness / 2
        return when (edge) {
            ResizeEdge.TOP -> FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, thickness, Gravity.TOP).apply {
                topMargin = margin
            }
            ResizeEdge.BOTTOM -> FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, thickness, Gravity.BOTTOM).apply {
                bottomMargin = margin
            }
            ResizeEdge.LEFT -> FrameLayout.LayoutParams(thickness, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START).apply {
                marginStart = margin
            }
            ResizeEdge.RIGHT -> FrameLayout.LayoutParams(thickness, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END).apply {
                marginEnd = margin
            }
        }
    }
}
