package com.orbys.launcherfakets13.ui.home

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.databinding.FragmentDesktopBinding
import com.orbys.launcherfakets13.domain.model.DesktopItemInfo
import com.orbys.launcherfakets13.domain.model.DesktopItemType
import com.orbys.launcherfakets13.ui.dialog.DesktopAddItemDialog
import com.orbys.launcherfakets13.ui.picker.AppPickerActivity
import com.orbys.launcherfakets13.util.DesktopWidgetHost
import com.orbys.launcherfakets13.util.PendingPinnedWidgetsStore
import com.orbys.launcherfakets13.util.WidgetPickerHelper
import com.orbys.launcherfakets13.util.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Pantalla independiente ("Desktop"), accesible mediante swipe desde el home,
 * donde apps y widgets se colocan libremente en una grid con snap a celda.
 */
@AndroidEntryPoint
class DesktopFragment : Fragment() {

    private val binding by viewBinding(FragmentDesktopBinding::bind)
    private val viewModel: DesktopViewModel by viewModels()

    private val appWidgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(requireContext()) }
    private val appWidgetHost: AppWidgetHost by lazy {
        object : AppWidgetHost(requireContext(), DesktopWidgetHost.ID) {
            override fun onCreateView(
                context: android.content.Context,
                appWidgetId: Int,
                appWidget: AppWidgetProviderInfo?
            ): android.appwidget.AppWidgetHostView {
                return object : android.appwidget.AppWidgetHostView(requireContext().applicationContext) {
                    override fun updateAppWidget(remoteViews: android.widget.RemoteViews?) {
                        // remoteViews == null es la señal normal del framework para pintar el
                        // initialLayout del proveedor (p. ej. justo tras el bind, antes de que
                        // el proveedor empuje su primer RemoteViews). Descartarlo aquí dejaba
                        // el widget en blanco para siempre en vez de mostrar su vista por defecto.
                        runCatching { super.updateAppWidget(remoteViews) }
                    }
                }
            }
        }
    }

    private var widgetPickerHelper: WidgetPickerHelper? = null
    private var pendingTargetRow = 0
    private var pendingTargetCol = 0
    private val editExiters = mutableListOf<() -> Unit>()
    private val pendingPinnedWidgetsQueue = ArrayDeque<Int>()

    // Id del item actualmente en modo edición (o null si ninguno). Cada card lleva su propio
    // moveMode/resizeMode en un closure independiente — sin esto no hay forma de saber, desde
    // OTRA card, que ya hay una edición en curso que haya que cerrar primero.
    private var editingItemId: String? = null

    // Si un cambio de estado llega mientras hay un dedo pulsado en la pantalla (p. ej. tocar la
    // card B mientras la A está editándose cierra A -> commitMove -> viewModel.moveItem ->
    // Dispatchers.Main.immediate colecta el StateFlow al instante, DESDE DENTRO del propio
    // dispatchTouchEvent en curso), no se reconstruye la grid ahí mismo: el View que Android
    // sigue usando como destino de ESE gesto concreto quedaría huérfano y desconectado del
    // árbol, aunque su GestureDetector de long-press siga disparando sobre él más tarde. En vez
    // de eso se guarda el último estado pendiente y MainActivity avisa (flushPendingRebuild)
    // cuando el dedo se levanta del todo, ya con el dispatch de ese evento terminado.
    //
    // (Antes esto se intentó con un contador local de ACTION_DOWN/UP por-card + por-fondo, pero
    // ese enfoque es intrínsecamente frágil: si el DOWN de una card no llega a ser "reclamado"
    // por ningún hijo ni por la propia card (contenido sin superficie táctil en esa zona, común
    // en ciertos widgets), Android reenvía ESE MISMO DOWN al contenedor como fallback, así que
    // se contaba dos veces el inicio pero solo una vez el final -- el contador quedaba atascado
    // en positivo para siempre y la grid dejaba de refrescarse hasta el próximo onPageShown().
    // MainActivity.dispatchTouchEvent, en cambio, ve cada evento EXACTAMENTE una vez sin
    // importar quién lo consuma después, así que es la única fuente fiable de "hay un dedo
    // pulsado ahora mismo".)
    private var pendingRebuildItems: List<DesktopItemInfo>? = null

    /** Llamado por MainActivity justo después de procesar un ACTION_UP/ACTION_CANCEL. */
    fun flushPendingRebuild() {
        val pending = pendingRebuildItems ?: return
        pendingRebuildItems = null
        if (isAdded && binding.desktopGridContainer.width > 0) rebuildGrid(pending)
    }

    // Papelera compartida por todas las cards (una sola instancia reutilizada entre rebuilds,
    // ver rebuildGrid): aparece al entrar en modo edición y, si se suelta la card arrastrada
    // encima, la borra en vez de intentar encajarla en una celda.
    private var deleteZone: View? = null

    private val appPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val pkg = data.getStringExtra(AppPickerActivity.RESULT_PACKAGE) ?: return@registerForActivityResult
        val label = data.getStringExtra(AppPickerActivity.RESULT_LABEL) ?: pkg
        val row = data.getIntExtra(AppPickerActivity.EXTRA_TARGET_ROW, pendingTargetRow)
        val col = data.getIntExtra(AppPickerActivity.EXTRA_TARGET_COL, pendingTargetCol)
        if (isCellFree(row, col, 1, 1, excludeId = null)) {
            viewModel.addApp("app_${UUID.randomUUID()}", row, col, pkg, label)
        } else {
            Toast.makeText(requireContext(), R.string.desktop_cell_occupied, Toast.LENGTH_SHORT).show()
        }
    }

    private val widgetBindLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> widgetPickerHelper?.onBindResult(result.resultCode == Activity.RESULT_OK) }

    private val widgetConfigureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> widgetPickerHelper?.onConfigureResult(result.resultCode == Activity.RESULT_OK) }

    companion object {
        private const val CELL_DP = 90
        private const val GAP_DP = 8

        // El Dock flotante es una ventana overlay propia (TYPE_APPLICATION_OVERLAY) que se
        // pinta SIEMPRE encima de esta pantalla y capta sus propios toques (ver DockController),
        // así que ninguna celda puede colocarse en la franja inferior que ocupa o quedaría
        // inalcanzable debajo de él. Altura real del panel expandido (65dp, ver
        // view_dock_overlay.xml) + su margen inferior (20dp, ver DockController.dockMargin) +
        // un pequeño colchón de seguridad (5dp). Es la ÚNICA reserva de espacio abajo (ver
        // maxRows): antes se sumaba también GRID_MARGIN_DP por abajo, dejando una franja muerta
        // mayor de la necesaria entre la última fila y el Dock.
        private const val DOCK_RESERVED_DP = 0

        // Margen lateral y superior para que las cards no empiecen pegadas al borde de la
        // pantalla. Abajo NO se aplica (solo DOCK_RESERVED_DP, ver maxRows): con el Dock ya
        // hay un colchón de sobra ahí, sumar este margen otra vez solo restaba espacio útil.
        private const val GRID_MARGIN_DP = 16
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_desktop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.desktopGridContainer.setOnTouchListener { _, event ->
            // Cerrar en el propio ACTION_DOWN (no esperar a que GestureDetector confirme un
            // "single tap"): con cualquier temblor del dedo por encima del touch-slop interno de
            // GestureDetector, el toque se reclasifica como "scroll" y onSingleTapUp/onSingleTapConfirmed
            // nunca llega a dispararse, dejando el modo edición abierto pese a haber tocado fuera.
            // Este listener solo ve toques que ninguna card consumió (fondo vacío de la grid), así
            // que cualquier DOWN aquí ya significa "fuera de todo widget/app".
            if (event.action == MotionEvent.ACTION_DOWN && editingItemId != null) {
                exitEditMode()
            }
            gridGestureDetector.onTouchEvent(event)
            false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Ver el comentario de pendingRebuildItems más arriba: con un dedo pulsado
                    // en pantalla, solo se guarda el estado; MainActivity dispara
                    // flushPendingRebuild() al soltar.
                    if (binding.desktopGridContainer.width > 0) {
                        val pointerDown = (activity as? MainActivity)?.isPointerDown() == true
                        if (pointerDown) pendingRebuildItems = state.items else rebuildGrid(state.items)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        runCatching { appWidgetHost.stopListening() }
    }

    /**
     * MainActivity lo consulta para no interpretar como swipe de cambio de página un arrastre
     * en curso dentro de una card en modo edición (ver dispatchTouchEvent en MainActivity) --
     * ese detector de fling vive a nivel de Activity/Window, fuera de la cadena de Views, así
     * que requestDisallowInterceptTouchEvent no tiene ningún efecto sobre él.
     */
    fun isEditing(): Boolean = editingItemId != null

    /** Llamado por MainActivity cuando la página Desktop se hace visible. */
    fun onPageShown() {
        // El servidor de AppWidgetService puede lanzar RemoteException (p. ej. NPE interno al
        // calcular UIDs si algún widget de este host quedó huérfano tras desinstalar su
        // proveedor) — sin este runCatching, ese fallo del sistema tumbaba toda la Activity al
        // cambiar de página. Ver mismo tratamiento en onPageHidden() y onDestroyView().
        runCatching { appWidgetHost.startListening() }
        binding.desktopGridContainer.post {
            if (isAdded) {
                rebuildGrid(viewModel.state.value.items)
                processPendingPinnedWidgets()
            }
        }
    }

    /** Llamado por MainActivity cuando la página Desktop se oculta. */
    fun onPageHidden() {
        exitEditMode()
        runCatching { appWidgetHost.stopListening() }
    }

    // ── Grid geometry ────────────────────────────────────────────────────────

    private fun cellPx() = dpToPx(CELL_DP)
    private fun gapPx() = dpToPx(GAP_DP)
    private fun stridePx() = cellPx() + gapPx()
    private fun sideMarginPx() = dpToPx(GRID_MARGIN_DP)
    private fun topMarginPx() = dpToPx(GRID_MARGIN_DP)
    private fun maxCols() =
        ((binding.desktopGridContainer.width - 2 * sideMarginPx()) / stridePx()).coerceAtLeast(1)
    // Solo un topMarginPx() (arriba): abajo el único límite es DOCK_RESERVED_DP, que ya incluye
    // su propio colchón — sumar el margen superior también abajo dejaba una franja muerta extra
    // de 16dp entre la última fila y el Dock, sin ninguna necesidad.
    private fun maxRows() =
        ((binding.desktopGridContainer.height - dpToPx(DOCK_RESERVED_DP) - topMarginPx()) / stridePx())
            .coerceAtLeast(1)

    // maxCols() sale de una división entera, así que casi nunca consume el ancho disponible
    // exacto -- el sobrante, con un margen izquierdo fijo, se quedaba TODO amontonado como
    // margen extra a la derecha (columnas pegadas a la izquierda, hueco grande a la derecha).
    // Aquí se reparte ese sobrante a partes iguales entre ambos lados, para que la grid quede
    // centrada y el margen se vea igual en los dos bordes.
    private fun effectiveSideMarginPx(): Int {
        val contentWidth = maxCols() * stridePx() - gapPx()
        val leftover = binding.desktopGridContainer.width - 2 * sideMarginPx() - contentWidth
        return sideMarginPx() + (leftover / 2).coerceAtLeast(0)
    }

    // Único punto de conversión columna/fila <-> x/y en pantalla, para que el margen quede
    // aplicado igual al posicionar una card y al traducir un toque a columna/fila (mismo
    // principio que spanToDp/spanToPx más abajo).
    private fun colToX(col: Int): Float = (col * stridePx() + effectiveSideMarginPx()).toFloat()
    private fun xToCol(x: Float): Int = ((x - effectiveSideMarginPx()) / stridePx()).toInt()
    private fun xToColRounded(x: Float): Int = ((x - effectiveSideMarginPx()) / stridePx()).roundToInt()
    private fun rowToY(row: Int): Float = (row * stridePx() + topMarginPx()).toFloat()
    private fun yToRow(y: Float): Int = ((y - topMarginPx()) / stridePx()).toInt()
    private fun yToRowRounded(y: Float): Int = ((y - topMarginPx()) / stridePx()).roundToInt()

    // Única fuente de verdad para convertir "cuántas celdas ocupa" <-> "cuántos dp/px mide en
    // pantalla". Antes había fórmulas equivalentes duplicadas en varios sitios (creación de la
    // card, confirmación del resize, opciones enviadas al widget) que con ciertas densidades de
    // pantalla no daban exactamente el mismo resultado (una hace cellPx()+gapPx() término a
    // término, otra suma en dp y convierte una vez) — eso causaba que la card "saltara" de
    // tamaño cada vez que la grid se reconstruía tras un resize. Con un único punto de
    // conversión usado en todos los caminos, deja de poder pasar (mismo principio que
    // WidgetSizes.updateWidgetSizeRanges en Launcher3/AOSP).
    private fun spanToDp(span: Int): Int = span * CELL_DP + (span - 1) * GAP_DP
    private fun spanToPx(span: Int): Int = dpToPx(spanToDp(span))
    private fun dpToSpanCeil(dp: Int): Int = ceil(dp.toDouble() / CELL_DP).toInt().coerceAtLeast(1)

    private fun rowColAt(x: Float, y: Float): Pair<Int, Int> {
        val col = xToCol(x).coerceIn(0, maxCols() - 1)
        val row = yToRow(y).coerceIn(0, maxRows() - 1)
        return row to col
    }

    private fun isCellFree(row: Int, col: Int, colSpan: Int, rowSpan: Int, excludeId: String?): Boolean {
        if (row < 0 || col < 0 || row + rowSpan > maxRows() || col + colSpan > maxCols()) return false
        return viewModel.state.value.items.none { other ->
            other.id != excludeId &&
                col < other.col + other.colSpan && col + colSpan > other.col &&
                row < other.row + other.rowSpan && row + rowSpan > other.row
        }
    }

    private fun findFreeCell(colSpan: Int, rowSpan: Int): Pair<Int, Int>? {
        for (row in 0..(maxRows() - rowSpan)) {
            for (col in 0..(maxCols() - colSpan)) {
                if (isCellFree(row, col, colSpan, rowSpan, excludeId = null)) return row to col
            }
        }
        return null
    }

    private data class WidgetFit(val row: Int, val col: Int, val colSpan: Int, val rowSpan: Int)

    /**
     * Cuando el tamaño pedido (maxColSpan x maxRowSpan) no cabe en ningún hueco de la grid,
     * busca el mayor tamaño reducido (encogiendo ambas dimensiones a la vez, proporcionalmente)
     * que sí quepa en algún sitio, para ofrecérselo al usuario en vez de cancelar sin más.
     */
    private fun findLargestFittingSpan(maxColSpan: Int, maxRowSpan: Int): WidgetFit? {
        for (shrink in 0 until maxOf(maxColSpan, maxRowSpan)) {
            val colSpan = (maxColSpan - shrink).coerceAtLeast(1)
            val rowSpan = (maxRowSpan - shrink).coerceAtLeast(1)
            findFreeCell(colSpan, rowSpan)?.let { (row, col) -> return WidgetFit(row, col, colSpan, rowSpan) }
            if (colSpan == 1 && rowSpan == 1) return null
        }
        return null
    }

    // ── Add-item flow (long-press on empty cell) ────────────────────────────

    private val gridGestureDetector by lazy {
        GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val (row, col) = rowColAt(e.x, e.y)
                if (isCellFree(row, col, 1, 1, excludeId = null)) showAddChooser(row, col)
            }
            // Salir de modo edición al tocar fondo vacío ya se resuelve en el ACTION_DOWN del
            // OnTouchListener del contenedor (ver onViewCreated) -- más fiable que onSingleTapUp.
        })
    }

    private fun showAddChooser(row: Int, col: Int) {
        DesktopAddItemDialog.newInstance().apply {
            onAddApp = { launchAppPickerFor(row, col) }
            onAddWidget = { launchWidgetPickerFor(row, col) }
        }.show(childFragmentManager, "desktop_add_item")
    }

    private fun launchAppPickerFor(row: Int, col: Int) {
        pendingTargetRow = row
        pendingTargetCol = col
        appPickerLauncher.launch(
            Intent(requireContext(), AppPickerActivity::class.java).apply {
                putExtra(AppPickerActivity.EXTRA_TARGET_ROW, row)
                putExtra(AppPickerActivity.EXTRA_TARGET_COL, col)
            }
        )
    }

    private fun launchWidgetPickerFor(row: Int, col: Int) {
        val helper = WidgetPickerHelper(
            context = requireContext(),
            appWidgetManager = appWidgetManager,
            host = appWidgetHost,
            bindLauncher = widgetBindLauncher,
            configureLauncher = widgetConfigureLauncher,
            onBound = { id, info -> finalizeWidgetAdd(id, info, row, col) },
            onFailed = { Toast.makeText(requireContext(), R.string.widget_add_failed, Toast.LENGTH_SHORT).show() }
        )
        widgetPickerHelper = helper
        helper.launchPicker()
    }

    /**
     * minWidth/minHeight es el tamaño con el que el proveedor diseñó que se vea bien por
     * defecto (no un mínimo cualquiera) — se usa tal cual como tamaño inicial. Antes partíamos
     * de minResizeWidth/Height (su mínimo absoluto) para que cupieran más widgets de entrada,
     * pero con TODOS los widgets ya redimensionables (ver buildCard) eso ya no hace falta: el
     * usuario puede encogerlo él mismo si quiere, y de entrada se ve como está pensado que se
     * vea, no recortado.
     *
     * Algunos proveedores (visto en logs: com.skg.browserwidget) declaran minWidth=0/minHeight=0
     * en vez de su tamaño real — metadata rota, no un widget que de verdad mida 0. Si se toma
     * literal, sale colSpan=rowSpan=1 (una celda), claramente más pequeño de lo que el widget
     * necesita. Cuando el eje reporta <=0 usamos un tamaño por defecto razonable en su lugar.
     *
     * Usamos dpToSpanCeil (misma fuente única de verdad que el resto de conversiones celda<->dp)
     * en vez de redondear al más cercano: así la celda asignada NUNCA queda por debajo de
     * minWidth/minHeight — redondear hacia abajo podía darle al widget MENOS espacio del que su
     * proveedor declara necesitar, lo cual sí sería un bug nuestro. Aun así, que el contenido
     * real renderizado coincida con ese tamaño depende del proveedor de terceros (si no
     * implementa onAppWidgetOptionsChanged de forma responsive, ignorará el tamaño que le demos y
     * seguirá pintando su layout fijo) — no hay forma de forzar eso desde el host, ni siquiera
     * Launcher3 lo hace.
     *
     * Quirk confirmado por pruebas: los proveedores de la familia com.skg.* declaran un
     * minWidth/minHeight muy por encima de lo que realmente pintan (visto en logs:
     * IotDeviceFourWidget declara 768x768dp; IotDeviceTwoWidget 768x384dp — la magnitud absoluta
     * no es de fiar para esta familia, pero la PROPORCIÓN entre ancho y alto sí parece
     * consistente entre sus proveedores (768x768 cuadrado vs. 768x384 el doble de ancho que
     * alto), así que en vez de ignorar el dato entero (dejaba todos los widgets en el mismo
     * tamaño mínimo fijo, sin distinguir un widget cuadrado de uno alargado) se escala el valor
     * declarado a la baja manteniendo su forma, acotado a un rango razonable. Sigue siendo una
     * estimación, no el tamaño real — el usuario lo ajusta a mano si no encaja.
     */
    private fun initialWidgetSpan(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val isOversizedSkgQuirk = info.provider.packageName.startsWith("com.skg.")
        val colSpan = when {
            isOversizedSkgQuirk -> dpToSpanCeil(info.minWidth / 3).coerceIn(2, 4).coerceAtMost(maxCols())
            info.minWidth > 0 -> dpToSpanCeil(info.minWidth).coerceIn(1, maxCols())
            else -> 2.coerceAtMost(maxCols())
        }
        val rowSpan = when {
            isOversizedSkgQuirk -> dpToSpanCeil(info.minHeight / 3).coerceIn(2, 4).coerceAtMost(maxRows())
            info.minHeight > 0 -> dpToSpanCeil(info.minHeight).coerceIn(1, maxRows())
            else -> 1.coerceAtMost(maxRows())
        }
        android.util.Log.d(
            "DesktopWidget",
            "provider=${info.provider} minWidth=${info.minWidth} minHeight=${info.minHeight} " +
                "minResizeWidth=${info.minResizeWidth} minResizeHeight=${info.minResizeHeight} " +
                "resizeMode=${info.resizeMode} -> colSpan=$colSpan rowSpan=$rowSpan " +
                "maxCols=${maxCols()} maxRows=${maxRows()}"
        )
        return colSpan to rowSpan
    }

    private fun finalizeWidgetAdd(appWidgetId: Int, info: AppWidgetProviderInfo, row: Int, col: Int) {
        val (colSpan, rowSpan) = initialWidgetSpan(info)
        val clampedCol = col.coerceAtMost((maxCols() - colSpan).coerceAtLeast(0))
        val clampedRow = row.coerceAtMost((maxRows() - rowSpan).coerceAtLeast(0))

        if (isCellFree(clampedRow, clampedCol, colSpan, rowSpan, excludeId = null)) {
            viewModel.addWidget("w_$appWidgetId", clampedRow, clampedCol, colSpan, rowSpan, appWidgetId)
            Toast.makeText(requireContext(), R.string.pin_widget_added, Toast.LENGTH_SHORT).show()
            return
        }

        // No cabe justo donde se soltó: si hay otro hueco libre del mismo tamaño en el resto
        // de la grid, lo colocamos ahí en vez de cancelar.
        findFreeCell(colSpan, rowSpan)?.let { (freeRow, freeCol) ->
            viewModel.addWidget("w_$appWidgetId", freeRow, freeCol, colSpan, rowSpan, appWidgetId)
            Toast.makeText(requireContext(), R.string.desktop_widget_placed_elsewhere, Toast.LENGTH_SHORT).show()
            return
        }

        // Tampoco cabe a tamaño completo en ningún otro sitio: si existe un tamaño más pequeño
        // que sí quepa en algún hueco, se lo ofrecemos al usuario en vez de cancelar sin más.
        val fit = findLargestFittingSpan(colSpan, rowSpan)
        if (fit == null) {
            runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
            Toast.makeText(requireContext(), R.string.desktop_no_space_for_pinned_widget, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_widget_shrink_to_fit_title)
            .setMessage(getString(R.string.dialog_widget_shrink_to_fit_msg, fit.colSpan, fit.rowSpan))
            .setPositiveButton(R.string.add_smaller) { _, _ ->
                viewModel.addWidget("w_$appWidgetId", fit.row, fit.col, fit.colSpan, fit.rowSpan, appWidgetId)
                Toast.makeText(requireContext(), R.string.pin_widget_added, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
            }
            .setOnCancelListener { runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) } }
            .show()
    }

    // ── Pending widgets pinned by third-party apps (CDD 3.8.1/H-SR-1) ───────

    /**
     * Coloca en el grid los widgets ya aceptados por PinItemConfirmActivity. Esta Activity
     * solo puede alojar el appWidgetId (no conoce la geometría del grid), así que la
     * colocación real se completa aquí, la primera vez que se muestra la página Desktop tras
     * la aceptación.
     */
    private fun processPendingPinnedWidgets() {
        val ids = PendingPinnedWidgetsStore.consumeAll(requireContext())
        if (ids.isEmpty()) return
        pendingPinnedWidgetsQueue.addAll(ids)
        processNextPendingPinnedWidget()
    }

    /**
     * Procesa los widgets pendientes de uno en uno: widgetPickerHelper/widgetBindLauncher/
     * widgetConfigureLauncher solo soportan un flujo de bind/configure a la vez, así que el
     * siguiente no arranca hasta que el actual termina (onBound u onFailed).
     */
    private fun processNextPendingPinnedWidget() {
        val appWidgetId = pendingPinnedWidgetsQueue.removeFirstOrNull() ?: return
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info == null) {
            runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
            processNextPendingPinnedWidget()
            return
        }

        val (colSpan, rowSpan) = initialWidgetSpan(info)
        val (row, col) = findFreeCell(colSpan, rowSpan) ?: run {
            runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
            Toast.makeText(requireContext(), R.string.desktop_no_space_for_pinned_widget, Toast.LENGTH_SHORT).show()
            processNextPendingPinnedWidget()
            return
        }

        val helper = WidgetPickerHelper(
            context = requireContext(),
            appWidgetManager = appWidgetManager,
            host = appWidgetHost,
            bindLauncher = widgetBindLauncher,
            configureLauncher = widgetConfigureLauncher,
            onBound = { id, boundInfo ->
                finalizeWidgetAdd(id, boundInfo, row, col)
                processNextPendingPinnedWidget()
            },
            onFailed = {
                Toast.makeText(requireContext(), R.string.widget_add_failed, Toast.LENGTH_SHORT).show()
                processNextPendingPinnedWidget()
            }
        )
        widgetPickerHelper = helper
        helper.continueBoundWidget(appWidgetId)
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    fun exitEditMode() {
        // Copia SOLO para iterar de forma segura (una closure puede disparar una llamada
        // reentrante a exitEditMode() -- ver historial del ConcurrentModificationException que
        // esto arregló originalmente) -- pero la lista real NO se vacía aquí. editExiters solo
        // se repuebla dentro de rebuildGrid() (al reconstruir las cards, ver buildCard), y la
        // mayoría de las veces que se llama a exitEditMode() NO hay rebuild de por medio (p. ej.
        // cambiar de card en edición sin que la anterior llegue a moverse de sitio, o tocar
        // fuera sin que hubiera ningún cambio de estado pendiente) -- vaciar la lista aquí la
        // dejaba permanentemente vacía para el resto de la vida de esas cards, así que la
        // SIGUIENTE vez que hubiera que cerrar cualquier edición no encontraba ningún closure
        // que invocar: la card se quedaba visualmente "atascada" en modo edición para siempre,
        // aunque editingItemId sí se limpiara. rebuildGrid() ya vacía la lista por su cuenta,
        // justo antes de repoblarla, que es el único momento en que de verdad hace falta.
        val exiters = editExiters.toList()
        exiters.forEach { it() }
        editingItemId = null
    }

    private fun rebuildGrid(items: List<DesktopItemInfo>) {
        // NO se llama a exitEditMode() aquí: eso comitearía y CERRARÍA la edición en curso
        // (editingItemId = null) cada vez que se reconstruye la grid, incluso cuando la
        // reconstrucción no tiene nada que ver con "el usuario dejó de editar" (p. ej. un
        // rebuild diferido -- ver pendingRebuildItems/flushPendingRebuild -- que se aplica justo
        // después de que el usuario seleccionase otra card, cuya edición sigue en curso
        // legítimamente). Solo se limpia la lista de closures de las cards viejas (van a
        // desaparecer con removeAllViews, así que ya no sirven) sin invocarlas: si de verdad hay
        // que comitear/cerrar algo, eso lo hace quien llame a exitEditMode() explícitamente
        // (tocar fuera, cambiar de página, tocar otra card). editingItemId se conserva tal cual
        // -- si sigue apuntando a un item, buildCard() más abajo restaura su estado visual en la
        // card nueva (ver el bloque final de buildCard).
        editExiters.clear()
        binding.desktopGridContainer.removeAllViews()
        for (item in items) {
            buildCard(clampToGridBounds(item))?.let { binding.desktopGridContainer.addView(it) }
        }
        // Añadida la última: queda por encima de todas las cards mientras se arrastra alguna.
        binding.desktopGridContainer.addView(ensureDeleteZone())
    }

    /**
     * Instancia única reutilizada entre rebuilds (removeAllViews la desprende del contenedor,
     * pero sigue siendo el mismo View, así que su visibility/scale sobreviven a la reconstrucción
     * de la grid mientras no haya un rebuild real de por medio).
     */
    private fun ensureDeleteZone(): View = deleteZone ?: buildDeleteZone().also { deleteZone = it }

    private fun buildDeleteZone(): View {
        val icon = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_trash)
            setColorFilter(Color.WHITE)
        }
        return FrameLayout(requireContext()).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(requireContext().getColor(R.color.widget_btn_delete_bg))
            }
            addView(icon, FrameLayout.LayoutParams(dpToPx(26), dpToPx(26), Gravity.CENTER))
            layoutParams = FrameLayout.LayoutParams(dpToPx(64), dpToPx(64), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dpToPx(20)
            }
            visibility = View.GONE
        }
    }

    /**
     * true si [card] se solapa con la papelera (no si su centro cae dentro): con un widget
     * grande, exigir que el CENTRO llegue hasta la papelera obligaba a arrastrar una barbaridad
     * -- con solapamiento basta con tocarla con cualquier borde, como el resto de UIs de
     * arrastrar-para-borrar. Solo tiene sentido durante un drag.
     */
    private fun isOverDeleteZone(card: FrameLayout): Boolean {
        val zone = deleteZone ?: return false
        if (zone.visibility != View.VISIBLE || zone.width == 0) return false
        val cardLeft = card.x
        val cardTop = card.y
        val cardRight = card.x + card.width
        val cardBottom = card.y + card.height
        return cardLeft < zone.right && cardRight > zone.left && cardTop < zone.bottom && cardBottom > zone.top
    }

    /**
     * Reubica un item que haya quedado fuera de los límites actuales de la grid (p. ej. bajo la
     * franja reservada para el Dock, tras introducir [DOCK_RESERVED_DP], o si la pantalla cambió
     * de tamaño). Sin esto, el item quedaría dibujado bajo el Dock: visible a medias pero
     * inalcanzable a los toques, sin forma de moverlo ni borrarlo desde la UI.
     */
    private fun clampToGridBounds(item: DesktopItemInfo): DesktopItemInfo {
        var colSpan = item.colSpan
        var rowSpan = item.rowSpan

        // Corrige widgets ya guardados (de antes de este fix, o de un resize que se pasó de
        // frenada) cuyo colSpan/rowSpan supere lo que el propio widget declara poder soportar
        // (maxResizeWidth/Height) — sin esto, un dato viejo se queda para siempre con una card
        // mucho más grande de lo que su contenido real llena.
        if (item.type == DesktopItemType.WIDGET) {
            val info = item.appWidgetId?.let { appWidgetManager.getAppWidgetInfo(it) }
            if (info != null) {
                if (info.maxResizeWidth > info.minWidth) {
                    colSpan = colSpan.coerceAtMost(dpToSpanCeil(info.maxResizeWidth))
                }
                if (info.maxResizeHeight > info.minHeight) {
                    rowSpan = rowSpan.coerceAtMost(dpToSpanCeil(info.maxResizeHeight))
                }
            }
        }

        val clampedCol = item.col.coerceIn(0, (maxCols() - colSpan).coerceAtLeast(0))
        val clampedRow = item.row.coerceIn(0, (maxRows() - rowSpan).coerceAtLeast(0))
        if (clampedRow == item.row && clampedCol == item.col && colSpan == item.colSpan && rowSpan == item.rowSpan) {
            return item
        }

        if (colSpan != item.colSpan || rowSpan != item.rowSpan) {
            viewModel.resizeItem(item.id, colSpan, rowSpan)
        }
        if (clampedRow != item.row || clampedCol != item.col) {
            viewModel.moveItem(item.id, clampedRow, clampedCol)
        }
        return item.copy(row = clampedRow, col = clampedCol, colSpan = colSpan, rowSpan = rowSpan)
    }

    private fun removeFailedWidget(appWidgetId: Int) {
        viewModel.removeFailedWidget(appWidgetId)
        Toast.makeText(requireContext(), R.string.widget_load_failed, Toast.LENGTH_SHORT).show()
    }

    /** Siempre BOTH: forzamos resize en ambos ejes para todos los widgets (ver buildCard). */
    private enum class ResizeAxis { BOTH, HORIZONTAL, VERTICAL }

    private fun buildCard(item: DesktopItemInfo): FrameLayout? {
        val content: View
        val widgetInfo: AppWidgetProviderInfo? = if (item.type == DesktopItemType.WIDGET) {
            item.appWidgetId?.let { appWidgetManager.getAppWidgetInfo(it) }
        } else null

        when (item.type) {
            DesktopItemType.APP -> content = buildAppContent(item) ?: return null
            DesktopItemType.WIDGET -> content = buildWidgetContent(item, widgetInfo) ?: return null
        }

        // Forzamos el tirador de resize en todos los widgets, ignorando resizeMode: hemos visto
        // proveedores con metadata poco fiable (minWidth=0 siendo mentira) cuyo resizeMode
        // probablemente tampoco refleje la realidad. El usuario decide si el resultado le vale.
        val canResizeH = widgetInfo != null
        val canResizeV = widgetInfo != null
        val resizable = widgetInfo != null
        if (item.type == DesktopItemType.WIDGET) {
            android.util.Log.d(
                "DesktopWidgetEdit",
                "buildCard id=${item.id} appWidgetId=${item.appWidgetId} resizeMode=${widgetInfo?.resizeMode} " +
                    "canResizeH=$canResizeH canResizeV=$canResizeV resizable=$resizable colSpan=${item.colSpan} rowSpan=${item.rowSpan}"
            )
        }
        val resizeAxis = when {
            canResizeH && canResizeV -> ResizeAxis.BOTH
            canResizeH -> ResizeAxis.HORIZONTAL
            else -> ResizeAxis.VERTICAL
        }

        var startTouchX = 0f
        var startTouchY = 0f
        var startCardX = 0f
        var startCardY = 0f
        var dragging = false
        var moveMode = false
        var resizeMode = false
        var longPressFired = false
        var resizeTouching = false
        var wasOverDeleteZone = false
        var editOverlay: View? = null
        var longPressDetector: GestureDetector? = null
        var onEditDone: (() -> Unit)? = null
        var downOnEditControl = false
        val editControlViews = mutableListOf<View>()

        // Construidos antes que `card`: su onTouchEvent (más abajo) necesita poder referenciarlos
        // (borrar-al-soltar-sobre-la-papelera consulta resizeHandleSpecs para ocultarlos).
        val resizeHandleSpecs: List<ResizeHandleSpec> = if (resizable) buildResizeHandles(resizeAxis) else emptyList()

        // Todos los widgets son redimensionables (ver arriba), así que todos usan el tamaño de
        // celda actual (colSpan/rowSpan) — igual que setupResize() al confirmar un resize, con
        // la misma fórmula (spanToPx) para que nunca haya un salto de tamaño entre ambos caminos.
        val widthPx = spanToPx(item.colSpan)
        val heightPx = spanToPx(item.rowSpan)

        val card = object : FrameLayout(requireContext()) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                // Si se toca esta card mientras OTRA está en modo edición, esa otra se cierra
                // primero (commit de su move/resize en curso) y este toque se procesa desde
                // cero como un gesto normal: un tap corto aquí es un click normal (no selecciona
                // esta card), y un long-press aquí sí entra en modo edición de ESTA card (ver
                // longPressDetector.onLongPress más abajo) — así A se deselecciona y B queda
                // seleccionado en el mismo gesto, sin pasos intermedios.
                if (ev.action == MotionEvent.ACTION_DOWN && editingItemId != null && editingItemId != item.id) {
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
                        startCardX = x
                        startCardY = y
                        dragging = false
                        // Si el dedo empezó sobre el aspa de borrar o el tirador de resize, un
                        // tembleque (>8dp) mientras se pulsa NUNCA debe convertirse en "arrastrar
                        // el widget entero" — eso robaba el toque al control y hacía que pareciera
                        // que borrar/redimensionar "no funciona".
                        downOnEditControl = editControlViews.any { control ->
                            control.visibility == View.VISIBLE &&
                                ev.x >= control.left && ev.x <= control.right &&
                                ev.y >= control.top && ev.y <= control.bottom
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (moveMode && !resizeTouching && !dragging && !downOnEditControl &&
                            (abs(ev.rawX - startTouchX) > dpToPx(8) || abs(ev.rawY - startTouchY) > dpToPx(8))
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
                            (abs(ev.rawX - startTouchX) > dpToPx(8) || abs(ev.rawY - startTouchY) > dpToPx(8))
                        ) {
                            dragging = true
                        }
                        if (dragging) {
                            x = startCardX + (ev.rawX - startTouchX)
                            y = startCardY + (ev.rawY - startTouchY)
                            elevation = dpToPx(12).toFloat()
                            val overZone = isOverDeleteZone(this)
                            alpha = if (overZone) 0.4f else 0.85f
                            // Feedback al cruzar el umbral (no en cada ACTION_MOVE): agranda la
                            // papelera y da un toque háptico, igual que soltar-para-borrar nativo.
                            if (overZone != wasOverDeleteZone) {
                                wasOverDeleteZone = overZone
                                deleteZone?.animate()?.scaleX(if (overZone) 1.25f else 1f)?.scaleY(if (overZone) 1.25f else 1f)
                                    ?.setDuration(120)?.start()
                                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            }
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> when {
                        dragging -> {
                            dragging = false
                            // La papelera se oculta SIEMPRE al soltar, se borre o no -- antes solo
                            // se ocultaba en la rama de borrado, así que tras un arrastre normal
                            // (soltar en otra celda, o en la misma) se quedaba visible en pantalla.
                            deleteZone?.apply { visibility = GONE; scaleX = 1f; scaleY = 1f }
                            if (wasOverDeleteZone) {
                                wasOverDeleteZone = false
                                resizeHandleSpecs.forEach { it.view.visibility = GONE }
                                editOverlay?.visibility = GONE
                                elevation = 0f
                                alpha = 1f
                                if (editingItemId == item.id) editingItemId = null
                                if (item.type == DesktopItemType.WIDGET && item.appWidgetId != null) {
                                    runCatching { appWidgetHost.deleteAppWidgetId(item.appWidgetId) }
                                }
                                viewModel.removeItem(item.id)
                            } else {
                                wasOverDeleteZone = false
                                commitMove(item, this)
                            }
                            return true
                        }
                        // Un tap simple sobre el propio widget mientras se edita YA NO sale del
                        // modo edición (a petición expresa): solo se sale tocando otra card, una
                        // zona vacía de la grid, o cambiando de página — evita que un roce cerca
                        // de los botones de borrar/resize entre y salga de la edición sin querer.
                        moveMode -> return true
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
            elevation = 0f
            layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
            x = colToX(item.col)
            y = rowToY(item.row)
        }

        // Solo widgets: el layout de una app (icono + etiqueta, item_desktop_app.xml) ya está
        // ajustado para llenar la celda entera y no tiene margen de sobra — encogerlo aquí
        // recorta el icono o el texto. El widget se encoge lo justo (effectiveContentInsetDp,
        // nunca por debajo de su minWidth/minHeight real) para dejar sitio a los tiradores de
        // resize entre el borde real de la card y el propio widget.
        val contentInsetPx = dpToPx(effectiveContentInsetDp(item, widgetInfo))
        card.addView(
            content,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                setMargins(contentInsetPx, contentInsetPx, contentInsetPx, contentInsetPx)
            }
        )

        val editFrame = FrameLayout(requireContext()).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dpToPx(2), requireContext().getColor(R.color.widget_border_edit))
                cornerRadius = dpToPx(16).toFloat()
            }
            visibility = View.GONE
        }
        // MIN_EDIT_FRAME_INSET_DP (no el valor por defecto, más generoso, que usa WidgetFragment):
        // como el contenido ya no se encoge (ver arriba), cualquier inset de más aquí dibujaría
        // el borde por dentro del widget en vez de pegado a su borde real. Este es el mínimo
        // imprescindible para que los tiradores de resize (centrados sobre esta línea) no se
        // corten contra las esquinas redondeadas de la card -- pura necesidad técnica, no hueco
        // cosmético.
        val editFrameInset = ResizeHandleFactory.editFrameInsetPx(requireContext(), ResizeHandleFactory.MIN_EDIT_FRAME_INSET_DP)
        card.addView(
            editFrame,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                setMargins(editFrameInset, editFrameInset, editFrameInset, editFrameInset)
            }
        )
        editOverlay = editFrame

        resizeHandleSpecs.forEach { spec ->
            card.addView(spec.view, spec.layoutParams)
            editControlViews.add(spec.view)
        }

        longPressDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onLongPress(e: MotionEvent) {
                android.util.Log.d("DesktopWidgetEdit", "onLongPress id=${item.id} type=${item.type} moveMode=$moveMode resizeMode=$resizeMode")
                if (moveMode || resizeMode) return
                // No hace falta comprobar aquí si YA hay otro widget editándose: el ACTION_DOWN
                // de este mismo gesto (dispatchTouchEvent más arriba) ya llamó a exitEditMode()
                // si tocaba una card distinta a la que estaba en edición, así que al llegar aquí
                // editingItemId ya está a null o ya es el de esta card. Eso es lo que permite que
                // un long-press sobre B mientras A está editándose cierre A y seleccione B
                // directamente, sin tener que tocar fuera primero.
                longPressFired = true
                card.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                moveMode = true
                resizeMode = resizable
                editingItemId = item.id
                resizeHandleSpecs.forEach { it.view.visibility = View.VISIBLE }
                editOverlay.visibility = View.VISIBLE
                ensureDeleteZone().apply { scaleX = 1f; scaleY = 1f; visibility = View.VISIBLE }
                android.util.Log.d("DesktopWidgetEdit", "entered edit mode id=${item.id} resizeHandles=${resizeHandleSpecs.size}")
            }
        })

        resizeHandleSpecs.forEach { spec ->
            setupResize(card, spec.view, item, spec.edge, widgetInfo, setResizeTouching = { resizeTouching = it })
        }

        // Invocado por exitEditMode() (tocar otra card, tocar una zona vacía de la grid, o
        // cambiar de página) - commit del move/resize en curso, sin paso de confirmación
        // aparte, igual que hace Android de forma nativa.
        onEditDone = {
            if (moveMode || resizeMode) {
                moveMode = false
                resizeMode = false
                // Si esto se dispara a media de un drag en curso (p. ej. el swipe que cambia de
                // página también cuenta como fling y llama a esto ANTES de que la card reciba su
                // propio ACTION_UP), hay que dejar dragging/resizeTouching/wasOverDeleteZone en
                // false: si no, ese ACTION_UP que llega justo después se re-procesa como si
                // siguiéramos en modo edición (commitMove otra vez, o hasta un borrado si el
                // drag venía pasando por encima de la papelera) con el estado ya a medio limpiar
                // -- la papelera se quedaba visible en pantalla aunque el modo edición ya hubiera
                // terminado.
                dragging = false
                resizeTouching = false
                wasOverDeleteZone = false
                resizeHandleSpecs.forEach { it.view.visibility = View.GONE }
                editOverlay?.visibility = View.GONE
                deleteZone?.apply { visibility = View.GONE; scaleX = 1f; scaleY = 1f }
                card.elevation = 0f
                card.alpha = 1f
                commitMove(item, card)
                if (editingItemId == item.id) editingItemId = null
            }
        }
        editExiters.add { onEditDone?.invoke() }

        // Si al reconstruirse la grid esta card resulta ser justo la que editingItemId dice que
        // está en edición (p. ej. una reconstrucción quedó pendiente -- ver pendingRebuildItems --
        // durante el propio gesto que seleccionó este item, y se aplica ya con la nueva
        // instancia creada), hay que restaurar aquí el estado visual: si no, editingItemId y la
        // vista quedan desincronizados -- el dato dice "se está editando" pero la card nueva
        // nace con moveMode=false, sin borde ni tiradores, hasta el próximo toque.
        if (editingItemId == item.id) {
            moveMode = true
            resizeMode = resizable
            resizeHandleSpecs.forEach { it.view.visibility = View.VISIBLE }
            editOverlay?.visibility = View.VISIBLE
            ensureDeleteZone().apply { scaleX = 1f; scaleY = 1f; visibility = View.VISIBLE }
        }

        return card
    }

    // Un tirador por cada borde del eje redimensionable, en vez de un único icono de esquina:
    // arriba/abajo controlan el alto, izquierda/derecha el ancho (ver setupResize). La
    // construcción/posición del tirador en sí (idéntica a WidgetFragment) vive en
    // ResizeHandleFactory.
    private data class ResizeHandleSpec(val view: View, val layoutParams: FrameLayout.LayoutParams, val edge: ResizeEdge)

    private fun buildResizeHandles(
        axis: ResizeAxis,
        editFrameInsetDp: Int = ResizeHandleFactory.MIN_EDIT_FRAME_INSET_DP
    ): List<ResizeHandleSpec> {
        val ctx = requireContext()
        fun spec(edge: ResizeEdge) = ResizeHandleSpec(ResizeHandleFactory.buildDot(ctx), ResizeHandleFactory.layoutParams(ctx, edge, editFrameInsetDp), edge)
        return when (axis) {
            ResizeAxis.BOTH -> listOf(spec(ResizeEdge.TOP), spec(ResizeEdge.BOTTOM), spec(ResizeEdge.LEFT), spec(ResizeEdge.RIGHT))
            // Grip centered on the right edge: only width changes when dragged.
            ResizeAxis.HORIZONTAL -> listOf(spec(ResizeEdge.RIGHT))
            // Grip centered on the bottom edge: only height changes when dragged.
            ResizeAxis.VERTICAL -> listOf(spec(ResizeEdge.BOTTOM))
        }
    }

    private fun buildAppContent(item: DesktopItemInfo): View? {
        val pkg = item.packageName ?: return null
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_desktop_app, null)
        val icon = view.findViewById<ImageView>(R.id.iv_desktop_app_icon)
        val label = view.findViewById<TextView>(R.id.tv_desktop_app_label)
        label.text = item.label ?: pkg
        val drawable = runCatching { requireContext().packageManager.getApplicationIcon(pkg) }.getOrNull()
        if (drawable != null) icon.setImageDrawable(drawable) else icon.setImageResource(R.drawable.ic_android)
        view.setOnClickListener {
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) startActivity(launchIntent)
        }
        return view
    }

    /**
     * El borde tiene que coincidir con el borde del widget, no dejar hueco de más -- así que el
     * widget solo se encoge lo estrictamente necesario para que los tiradores de resize no se
     * corten contra las esquinas de la card (ResizeHandleFactory.MIN_EDIT_FRAME_INSET_DP), nunca
     * más que eso. Ese mínimo, a su vez, nunca puede hacer que el widget reciba MENOS espacio
     * real que su propio minWidth/minHeight declarado -- eso es justo lo que initialWidgetSpan()
     * ya garantiza no pasar al elegir colSpan/rowSpan (ver su comentario) -- así que se recorta
     * más todavía (nunca se aumenta) si hace falta; en el caso extremo de un widget sin nada de
     * margen de sobra, el borde puede acabar un poco por dentro del propio widget en vez de
     * coincidir exactamente con él, pero eso ya es inevitable sin violar su mínimo declarado.
     */
    private fun effectiveContentInsetDp(item: DesktopItemInfo, info: AppWidgetProviderInfo?): Int {
        if (item.type != DesktopItemType.WIDGET) return 0
        val maxInsetW = ((spanToDp(item.colSpan) - (info?.minWidth ?: 0)) / 2).coerceAtLeast(0)
        val maxInsetH = ((spanToDp(item.rowSpan) - (info?.minHeight ?: 0)) / 2).coerceAtLeast(0)
        return minOf(ResizeHandleFactory.MIN_EDIT_FRAME_INSET_DP, maxInsetW, maxInsetH)
    }

    // El borde/tiradores se quedan siempre en ResizeHandleFactory.MIN_EDIT_FRAME_INSET_DP (fijo,
    // pegado al borde real de la card -- ver su uso en buildCard), independiente del contenido:
    // es un mínimo técnico (que los tiradores no se corten contra las esquinas redondeadas de la
    // card), no algo que dependa de cuánto se encoja el widget. Mientras effectiveContentInsetDp
    // sea mayor que ese mínimo (caso normal, salvo widgets sin nada de margen sobre su propio
    // mínimo), el widget queda más adentro que el borde y se ve el hueco entre ambos.

    private fun buildWidgetContent(item: DesktopItemInfo, info: AppWidgetProviderInfo?): View? {
        val appWidgetId = item.appWidgetId ?: return null
        if (info == null) {
            removeFailedWidget(appWidgetId)
            return null
        }
        val widgetView = try {
            appWidgetHost.createView(requireContext(), appWidgetId, info)
        } catch (e: Exception) {
            removeFailedWidget(appWidgetId)
            return null
        }
        // Todos los widgets son redimensionables (ver buildCard), así que siempre usan el
        // tamaño de celda actual — misma fórmula (spanToDp) que el resto de caminos, menos el
        // inset efectivo (ver effectiveContentInsetDp) para que coincida con el espacio real
        // que le da card.addView(content, ...) en buildCard.
        val insetDp = effectiveContentInsetDp(item, info)
        val widthDp = spanToDp(item.colSpan) - insetDp * 2
        val heightDp = spanToDp(item.rowSpan) - insetDp * 2
        val options = widgetSizeOptions(widthDp, heightDp)
        appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
        widgetView.updateAppWidgetSize(options, listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
        widgetView.setPadding(0, 0, 0, 0)
        return widgetView
    }

    /**
     * updateAppWidgetSize(Bundle, List<SizeF>) es la API moderna para layouts adaptables
     * (plegables), pero la mayoría de proveedores de widgets solo miran las claves clásicas
     * OPTION_APPWIDGET_*_WIDTH/HEIGHT dentro del Bundle (vía onAppWidgetOptionsChanged). Sin
     * ellas, el widget renderiza con su propio tamaño por defecto, sin relación con la celda
     * real que le hemos asignado en la grid.
     */
    private fun widgetSizeOptions(widthDp: Int, heightDp: Int): Bundle = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
    }

    private fun commitMove(item: DesktopItemInfo, card: FrameLayout) {
        val snappedCol = xToColRounded(card.x).coerceIn(0, (maxCols() - item.colSpan).coerceAtLeast(0))
        val snappedRow = yToRowRounded(card.y).coerceIn(0, (maxRows() - item.rowSpan).coerceAtLeast(0))
        if (snappedRow == item.row && snappedCol == item.col) {
            card.animate().x(colToX(item.col)).y(rowToY(item.row)).start()
            return
        }
        if (isCellFree(snappedRow, snappedCol, item.colSpan, item.rowSpan, excludeId = item.id)) {
            card.animate().x(colToX(snappedCol)).y(rowToY(snappedRow)).start()
            viewModel.moveItem(item.id, snappedRow, snappedCol)
        } else {
            card.animate().x(colToX(item.col)).y(rowToY(item.row)).start()
            Toast.makeText(requireContext(), R.string.desktop_cell_occupied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupResize(
        card: FrameLayout,
        handle: View,
        item: DesktopItemInfo,
        edge: ResizeEdge,
        info: AppWidgetProviderInfo?,
        setResizeTouching: (Boolean) -> Unit
    ) {
        var startX = 0f
        var startY = 0f
        var startW = 0
        var startH = 0
        var startCardX = 0f
        var startCardY = 0f
        var currentW = 0
        var currentH = 0

        // Límite real que el propio widget declara poder soportar (maxResizeWidth/Height, "sin
        // efecto" si es <= minWidth/minHeight según la documentación de la API) — sin esto, el
        // rectángulo se puede arrastrar mucho más grande de lo que el widget realmente sabe
        // renderizar, y su contenido se queda pequeño/clavado dentro de un hueco enorme.
        val maxWidthPx = info?.takeIf { it.maxResizeWidth > it.minWidth }?.let { dpToPx(it.maxResizeWidth) }
        val maxHeightPx = info?.takeIf { it.maxResizeHeight > it.minHeight }?.let { dpToPx(it.maxResizeHeight) }
        val maxColSpanFromWidget = info?.takeIf { it.maxResizeWidth > it.minWidth }?.let { dpToSpanCeil(it.maxResizeWidth) }
        val maxRowSpanFromWidget = info?.takeIf { it.maxResizeHeight > it.minHeight }?.let { dpToSpanCeil(it.maxResizeHeight) }

        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    android.util.Log.d("DesktopWidgetEdit", "resize handle DOWN id=${item.id} edge=$edge")
                    setResizeTouching(true)
                    startX = event.rawX
                    startY = event.rawY
                    startW = card.width
                    startH = card.height
                    startCardX = card.x
                    startCardY = card.y
                    currentW = startW
                    currentH = startH
                    handle.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // LEFT/TOP anclan el borde opuesto: crecen hacia su propio lado moviendo
                    // también la posición de la card, en vez de solo el tamaño (como RIGHT/BOTTOM).
                    when (edge) {
                        ResizeEdge.RIGHT -> {
                            currentW = (startW + (event.rawX - startX).toInt()).coerceAtLeast(cellPx())
                            if (maxWidthPx != null) currentW = currentW.coerceAtMost(maxWidthPx)
                        }
                        ResizeEdge.LEFT -> {
                            var newW = (startW - (event.rawX - startX).toInt()).coerceAtLeast(cellPx())
                            if (maxWidthPx != null) newW = newW.coerceAtMost(maxWidthPx)
                            currentW = newW
                            card.x = startCardX + startW - newW
                        }
                        ResizeEdge.BOTTOM -> {
                            currentH = (startH + (event.rawY - startY).toInt()).coerceAtLeast(cellPx())
                            if (maxHeightPx != null) currentH = currentH.coerceAtMost(maxHeightPx)
                        }
                        ResizeEdge.TOP -> {
                            var newH = (startH - (event.rawY - startY).toInt()).coerceAtLeast(cellPx())
                            if (maxHeightPx != null) newH = newH.coerceAtMost(maxHeightPx)
                            currentH = newH
                            card.y = startCardY + startH - newH
                        }
                    }
                    (card.layoutParams as FrameLayout.LayoutParams).width = currentW
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

                    var newCol = item.col
                    var newRow = item.row
                    var newColSpan = item.colSpan
                    var newRowSpan = item.rowSpan

                    when (edge) {
                        ResizeEdge.RIGHT -> {
                            val gridMax = (maxCols() - item.col).coerceAtLeast(1)
                            val upperBound = maxColSpanFromWidget?.coerceIn(1, gridMax) ?: gridMax
                            newColSpan = ((currentW + gapPx()).toFloat() / stridePx()).roundToInt().coerceIn(1, upperBound)
                        }
                        ResizeEdge.LEFT -> {
                            // El borde derecho (col + colSpan) se mantiene fijo; solo cambian col y colSpan.
                            val rightCellFixed = item.col + item.colSpan
                            val upperBound = maxColSpanFromWidget?.coerceIn(1, rightCellFixed) ?: rightCellFixed
                            newColSpan = ((currentW + gapPx()).toFloat() / stridePx()).roundToInt().coerceIn(1, upperBound)
                            newCol = (rightCellFixed - newColSpan).coerceAtLeast(0)
                        }
                        ResizeEdge.BOTTOM -> {
                            val gridMax = (maxRows() - item.row).coerceAtLeast(1)
                            val upperBound = maxRowSpanFromWidget?.coerceIn(1, gridMax) ?: gridMax
                            newRowSpan = ((currentH + gapPx()).toFloat() / stridePx()).roundToInt().coerceIn(1, upperBound)
                        }
                        ResizeEdge.TOP -> {
                            // El borde inferior (row + rowSpan) se mantiene fijo; solo cambian row y rowSpan.
                            val bottomCellFixed = item.row + item.rowSpan
                            val upperBound = maxRowSpanFromWidget?.coerceIn(1, bottomCellFixed) ?: bottomCellFixed
                            newRowSpan = ((currentH + gapPx()).toFloat() / stridePx()).roundToInt().coerceIn(1, upperBound)
                            newRow = (bottomCellFixed - newRowSpan).coerceAtLeast(0)
                        }
                    }

                    val snappedW = spanToPx(newColSpan)
                    val snappedH = spanToPx(newRowSpan)
                    (card.layoutParams as FrameLayout.LayoutParams).width = snappedW
                    (card.layoutParams as FrameLayout.LayoutParams).height = snappedH
                    card.x = colToX(newCol)
                    card.y = rowToY(newRow)
                    card.requestLayout()
                    android.util.Log.d(
                        "DesktopWidgetEdit",
                        "resize handle UP id=${item.id} newRow=$newRow newCol=$newCol newColSpan=$newColSpan newRowSpan=$newRowSpan " +
                            "cellFree=${isCellFree(newRow, newCol, newColSpan, newRowSpan, excludeId = item.id)}"
                    )
                    if (isCellFree(newRow, newCol, newColSpan, newRowSpan, excludeId = item.id)) {
                        if (newRow != item.row || newCol != item.col) viewModel.moveItem(item.id, newRow, newCol)
                        // resizeItem() ya dispara, vía el StateFlow, un rebuildGrid() que
                        // reconstruye esta card desde cero con buildWidgetContent() -- que es
                        // quien de verdad debe decidir el tamaño que se le reporta al widget
                        // (con el hueco del borde ya descontado, ver effectiveContentInsetDp).
                        // Repetir aquí ese cálculo a mano (como se hacía antes) usando el tamaño
                        // de celda COMPLETO, sin restar el hueco, pisaba el valor correcto del
                        // rebuild con uno que no coincidía con el margen real de la card -- por
                        // eso el borde se desalineaba justo después de redimensionar.
                        viewModel.resizeItem(item.id, newColSpan, newRowSpan)
                    } else {
                        // El hueco de destino no está libre: revierte a la posición/tamaño
                        // originales (para LEFT/TOP esto también deshace el desplazamiento).
                        card.x = colToX(item.col)
                        card.y = rowToY(item.row)
                        (card.layoutParams as FrameLayout.LayoutParams).width = spanToPx(item.colSpan)
                        (card.layoutParams as FrameLayout.LayoutParams).height = spanToPx(item.rowSpan)
                        card.requestLayout()
                        Toast.makeText(requireContext(), R.string.desktop_cell_occupied, Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
