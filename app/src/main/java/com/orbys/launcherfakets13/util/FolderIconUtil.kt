package com.orbys.launcherfakets13.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.orbys.launcherfakets13.R

/**
 * Compone un icono de "carpeta" con hasta 4 iconos reales de apps en una rejilla 2x2 sobre un
 * fondo redondeado, igual que el icono nativo de Android al agrupar apps arrastrando una encima
 * de otra.
 *
 * El resultado se cachea por lista de paquetes + tamaño: recomponer el bitmap (Binder por cada
 * icono + un ARGB_8888 nuevo) en cada apertura de la categoría "Colaboración" es puro
 * desperdicio cuando la lista de apps de la carpeta nunca cambia entre reconstrucciones.
 */
object FolderIconUtil {

    private val cache = HashMap<String, Drawable>()

    /**
     * @param apps pares de (packageName, drawable de respaldo). Se intenta primero el icono real
     * de la app instalada; si no está instalada (p. ej. sin GMS) se usa el drawable de respaldo
     * en vez de omitir el icono.
     */
    fun buildFolderPreviewIcon(context: Context, apps: List<Pair<String, Int>>, sizeDp: Int = 40): Drawable {
        val key = "$sizeDp:${apps.joinToString(",") { it.first }}"
        cache[key]?.let { return it }
        return composeFolderPreviewIcon(context, apps, sizeDp).also { cache[key] = it }
    }

    private fun composeFolderPreviewIcon(context: Context, apps: List<Pair<String, Int>>, sizeDp: Int): Drawable {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
        val icons = apps
            .mapNotNull { (pkg, fallbackRes) ->
                runCatching { context.packageManager.getApplicationIcon(pkg) }.getOrNull()
                    ?: context.getDrawable(fallbackRes)
            }
            .take(4)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.slot_bg_saved) }
        val radius = sizePx * 0.28f
        canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radius, radius, bgPaint)

        if (icons.isEmpty()) return BitmapDrawable(context.resources, bitmap)

        val padding = sizePx * 0.14f
        val gap = sizePx * 0.08f
        val cell = (sizePx - 2 * padding - gap) / 2f

        icons.forEachIndexed { index, drawable ->
            val col = index % 2
            val row = index / 2
            val left = padding + col * (cell + gap)
            val top = padding + row * (cell + gap)
            drawable.setBounds(left.toInt(), top.toInt(), (left + cell).toInt(), (top + cell).toInt())
            drawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}
