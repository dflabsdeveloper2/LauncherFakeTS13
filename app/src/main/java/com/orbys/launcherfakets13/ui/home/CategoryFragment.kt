package com.orbys.launcherfakets13.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.orbys.launcherfakets13.R
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.orbys.launcherfakets13.databinding.FragmentCategoryBinding
import com.orbys.launcherfakets13.domain.model.Environment
import com.orbys.launcherfakets13.domain.model.Shortcut
import com.orbys.launcherfakets13.services.overlay.DockOverlayService
import com.orbys.launcherfakets13.ui.dialog.AdminDisabledDialog
import com.orbys.launcherfakets13.ui.dialog.GoogleAppsFolderDialog
import com.orbys.launcherfakets13.ui.picker.AppPickerActivity
import com.orbys.launcherfakets13.util.DeviceAccountUtil
import com.orbys.launcherfakets13.util.FolderIconUtil
import com.orbys.launcherfakets13.util.viewBinding
import kotlinx.coroutines.launch

/**
 * Fragmento que gestiona las categorías de aplicaciones y sus accesos directos (slots).
 *
 * Muestra una lista de categorías con una cuadrícula de 2x2 para shortcuts.
 * Permite la personalización (añadir/eliminar categorías y slots) o muestra una vista
 * predefinida dependiendo del [Environment].
 */
class CategoryFragment : Fragment() {

    private val binding by viewBinding(FragmentCategoryBinding::bind)
    private val viewModel: MainViewModel by activityViewModels()

    // Evita volver a pedir/decodificar el mismo icono de PackageManager en cada reconstrucción
    // de categorías (slots guardados + widgets con useRealAppIcon). Vive mientras viva el
    // Fragment, que es prácticamente toda la sesión — un icono desactualizado tras una
    // actualización de app se corrige solo al recrear la Activity.
    private val appIconCache = HashMap<String, Drawable?>()

    private fun cachedAppIcon(packageName: String): Drawable? =
        appIconCache.getOrPut(packageName) {
            runCatching { requireContext().packageManager.getApplicationIcon(packageName) }.getOrNull()
        }

    // Maneja el resultado del selector de aplicaciones para añadir un acceso directo
    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val pkg = data.getStringExtra(AppPickerActivity.RESULT_PACKAGE)
                ?: return@registerForActivityResult
            val label = data.getStringExtra(AppPickerActivity.RESULT_LABEL) ?: pkg
            val cat = data.getStringExtra(AppPickerActivity.EXTRA_CATEGORY)
                ?: return@registerForActivityResult
            val idx = data.getIntExtra(AppPickerActivity.EXTRA_SLOT_INDEX, -1)
            if (idx in 0..3) {
                viewModel.setShortcut(cat, idx, pkg, label)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observa solo lo relevante para esta pantalla (categorías, entorno y el "algo cambió
        // en los shortcuts" de refreshTrigger) — así actualizaciones ajenas del estado global
        // (wifi/bluetooth/hotspot/usb, que también re-emiten MainState) no fuerzan una
        // reconstrucción completa de las tarjetas en cada parpadeo de conectividad.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { Triple(it.categories, it.currentEnvironment, it.refreshTrigger) }
                    .distinctUntilChanged()
                    .collect { (categories, _, _) ->
                        rebuildCategories(categories)
                    }
            }
        }
    }

    /**
     * Reconstruye dinámicamente el contenedor de categorías.
     */
    private fun rebuildCategories(categories: List<String>) {
        binding.categoriesContainer.removeAllViews()
        val container = binding.categoriesContainer
        val inflater = LayoutInflater.from(requireContext())
        val slotPx = dpToPx(120)
        val gapPx = dpToPx(10)

        // Verifica si el entorno es uno de los predefinidos (hardcoded views)
        val isHardcoded = viewModel.uiState.value.currentEnvironment in listOf(
            Environment.CORPORATE,
            Environment.GOOGLE,
            Environment.OFFICE
        )

        for (catName in categories) {
            val card = inflater.inflate(R.layout.item_category_card, container, false)
            val rowTop = card.findViewById<LinearLayout>(R.id.slots_row_top)
            val rowBot = card.findViewById<LinearLayout>(R.id.slots_row_bottom)
            val spacer = card.findViewById<View>(R.id.rows_spacer)

            // Título de categoría - Long press para eliminar si es personalizable
            card.findViewById<TextView>(R.id.tv_category_title).apply {
                text = catName
                if (!isHardcoded) {
                    setOnLongClickListener { showDeleteCategoryDialog(catName); true }
                }
            }

            // Botón para colapsar/expandir categoría
            card.findViewById<View>(R.id.btn_category_collapse).setOnClickListener {
                val show = rowTop.visibility != View.VISIBLE
                val vis = if (show) View.VISIBLE else View.GONE
                rowTop.visibility = vis
                rowBot.visibility = vis
                spacer.visibility = vis
            }

            if (isHardcoded) {
                fillHardcodedCategory(
                    catName,
                    rowTop,
                    rowBot,
                    viewModel.uiState.value.currentEnvironment
                )
            } else {
                // Slots 0, 1 → Fila superior
                for (i in 0..1) {
                    val sv = slotView(catName, i)
                    val lp = LinearLayout.LayoutParams(slotPx, slotPx)
                    if (i == 1) lp.marginStart = gapPx
                    sv.layoutParams = lp
                    rowTop.addView(sv)
                }

                // Slots 2, 3 → Fila inferior
                for (i in 2..3) {
                    val sv = slotView(catName, i)
                    val lp = LinearLayout.LayoutParams(slotPx, slotPx)
                    if (i == 3) lp.marginStart = gapPx
                    sv.layoutParams = lp
                    rowBot.addView(sv)
                }
            }

            container.addView(card)
        }

        // Si es personalizable, añade tarjetas para crear nueva categoría o widget
        if (!isHardcoded) {
            val addColumn = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            }
            addColumn.addView(buildAddWidgetCard())
            addColumn.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(1, dpToPx(8))
            })
            addColumn.addView(buildNewCategoryCard())
            container.addView(addColumn)
        }
    }

    /**
     * Llena una categoría con contenido predefinido para entornos corporativos/educativos.
     */
    private fun fillHardcodedCategory(
        catName: String,
        rowTop: LinearLayout,
        rowBot: LinearLayout,
        env: Environment
    ) {
        val slotPx = dpToPx(90)
        val gapPx = dpToPx(10)

        // Definición de widgets predefinidos por entorno y categoría
        val widgets = when (env) {
            Environment.CORPORATE -> when (catName) {
                "General" -> listOf(
                    CorpWidgetData(getString(R.string.widget_clock), R.drawable.ic_recents, R.layout.widget_corporate_clock, packageName = "com.google.android.deskclock"),
                    CorpWidgetData(getString(R.string.widget_agenda), R.drawable.ic_cat_calendar, R.layout.widget_corporate_generic, packageName = "com.google.android.calendar"),
                    CorpWidgetData(getString(R.string.widget_weather), R.drawable.ic_cat_weather, R.layout.widget_corporate_weather),
                    CorpWidgetData(getString(R.string.widget_focus), R.drawable.ic_cat_focus, R.layout.widget_corporate_generic, packageName = "com.android.settings")
                )
                "Sala" -> listOf(
                    CorpWidgetData(getString(R.string.widget_timer), R.drawable.ic_cat_timer, R.layout.widget_corporate_generic, packageName = "com.google.android.deskclock"),
                    CorpWidgetData(getString(R.string.widget_videocall), R.drawable.ic_cat_teams, R.layout.widget_corporate_generic, packageName = "com.google.android.apps.tachyon"),
                    CorpWidgetData(getString(R.string.widget_eshare), R.drawable.ic_cat_mirroring, R.layout.widget_corporate_generic, packageName = "com.ecloud.eshare.server")
                )
                "Trabajo" -> listOf(
                    CorpWidgetData(getString(R.string.widget_mail), R.drawable.ic_cat_mail, R.layout.widget_corporate_generic, packageName = "com.google.android.gm"),
                    CorpWidgetData(getString(R.string.widget_storage), R.drawable.ic_cat_drive, R.layout.widget_corporate_generic, packageName = "com.google.android.apps.docs"),
                    CorpWidgetData(getString(R.string.widget_docs), R.drawable.ic_cat_doc, R.layout.widget_corporate_generic, packageName = "com.google.android.apps.docs.editors.docs"),
                    CorpWidgetData(getString(R.string.widget_notes), R.drawable.ic_cat_note, R.layout.widget_corporate_generic, packageName = "com.google.android.apps.docs.editors.docs")
                )
                // CDD/EDLA: icono fijo de Play Store + carpeta de apps de Google, sustituyendo
                // el contenido anterior de esta categoría.
                "Colaboración" -> listOf(playStoreWidgetData(), googleAppsFolderWidgetData(), null, null)
                else -> emptyList()
            }
            Environment.GOOGLE -> when (catName) {
                "General" -> listOf(
                    CorpWidgetData("RELOJ", R.drawable.ic_recents, R.layout.widget_corporate_clock, packageName = "com.google.android.deskclock"),
                    CorpWidgetData("GOOGLE CALENDAR", R.drawable.ic_cat_calendar, R.layout.widget_corporate_generic, packageName = "com.google.android.calendar"),
                    CorpWidgetData("CLIMA", R.drawable.ic_cat_weather, R.layout.widget_corporate_generic),
                    CorpWidgetData("CONCENTRACIÓN", R.drawable.ic_cat_focus, R.layout.widget_corporate_generic, packageName = "com.android.settings")
                )
                "Aula" -> listOf(
                    CorpWidgetData("TEMPORIZADOR", R.drawable.ic_cat_timer, R.layout.widget_corporate_generic, packageName = "com.google.android.deskclock"),
                    CorpWidgetData(getString(R.string.widget_dice), R.drawable.ic_cat_dice, R.layout.widget_corporate_generic),
                    CorpWidgetData(getString(R.string.widget_noise_meter), R.drawable.ic_cat_noise, R.layout.widget_corporate_generic),
                    CorpWidgetData(getString(R.string.widget_orbys_translate), R.drawable.ic_cat_translate, R.layout.widget_corporate_generic, packageName = "com.orbys.aitranslate")
                )
                "Trabajo" -> listOf(
                    CorpWidgetData("GMAIL", R.drawable.ic_cat_mail, R.layout.widget_corporate_generic, packageName = "com.google.android.gm"),
                    CorpWidgetData("GOOGLE DRIVE", R.drawable.ic_cat_drive, R.layout.widget_corporate_generic, packageName = "com.google.android.apps.docs"),
                    CorpWidgetData("MEET", R.drawable.ic_cat_teams, R.layout.widget_corporate_generic, packageName = "com.google.android.apps.tachyon"),
                    CorpWidgetData("DOCS", R.drawable.ic_cat_doc, R.layout.widget_corporate_generic, packageName = "com.google.android.apps.docs.editors.docs")
                )
                "Colaboración" -> listOf(playStoreWidgetData(), googleAppsFolderWidgetData(), null, null)
                else -> emptyList()
            }
            Environment.OFFICE -> when (catName) {
                "General" -> listOf(
                    CorpWidgetData("RELOJ", R.drawable.ic_recents, R.layout.widget_corporate_clock, packageName = "com.google.android.deskclock"),
                    CorpWidgetData("CALENDARIO", R.drawable.ic_cat_calendar, R.layout.widget_corporate_generic, packageName = "com.microsoft.office.outlook"),
                    CorpWidgetData("CLIMA", R.drawable.ic_cat_weather, R.layout.widget_corporate_generic),
                    CorpWidgetData("CONCENTRACIÓN", R.drawable.ic_cat_focus, R.layout.widget_corporate_generic, packageName = "com.android.settings")
                )
                "Aula" -> listOf(
                    CorpWidgetData(getString(R.string.widget_timer), R.drawable.ic_cat_timer, R.layout.widget_corporate_generic, packageName = "com.google.android.deskclock"),
                    CorpWidgetData(getString(R.string.widget_student_selector), R.drawable.ic_cat_dice, R.layout.widget_corporate_generic),
                    CorpWidgetData(getString(R.string.widget_noise_meter), R.drawable.ic_cat_noise, R.layout.widget_corporate_generic),
                    CorpWidgetData(getString(R.string.widget_orbys_translate), R.drawable.ic_cat_translate, R.layout.widget_corporate_generic, packageName = "com.orbys.aitranslate")
                )
                "Trabajo" -> listOf(
                    CorpWidgetData("OUTLOOK", R.drawable.ic_cat_mail, R.layout.widget_corporate_generic, packageName = "com.microsoft.office.outlook"),
                    CorpWidgetData("ONEDRIVE", R.drawable.ic_cat_drive, R.layout.widget_corporate_generic, packageName = "com.microsoft.skydrive"),
                    CorpWidgetData("TEAMS", R.drawable.ic_cat_teams, R.layout.widget_corporate_generic, packageName = "com.microsoft.teams"),
                    CorpWidgetData("WORD", R.drawable.ic_cat_doc, R.layout.widget_corporate_generic, packageName = "com.microsoft.office.word")
                )
                "Colaboración" -> listOf(playStoreWidgetData(), googleAppsFolderWidgetData(), null, null)
                else -> emptyList()
            }
        }

        // Infla y posiciona los widgets en la rejilla de 2x2
        widgets.forEachIndexed { i, data ->
            val view = if (data != null) buildCorporateWidget(data) else FrameLayout(requireContext())
            val lp = LinearLayout.LayoutParams(slotPx, slotPx)
            if (i % 2 == 1) lp.marginStart = gapPx
            view.layoutParams = lp
            if (i < 2) rowTop.addView(view) else rowBot.addView(view)
        }
    }

    private data class CorpWidgetData(
        val header: String,
        val iconRes: Int,
        val layoutRes: Int,
        val title: String? = null,
        val packageName: String? = null,
        // CDD/EDLA: usa el icono real instalado de packageName en vez de iconRes.
        val useRealAppIcon: Boolean = false,
        // CDD/EDLA: icono compuesto con los iconos reales de estas apps (carpeta de Google);
        // par de (packageName, drawable de respaldo si la app no está instalada).
        val folderPreviewPackages: List<Pair<String, Int>>? = null,
        // CDD/EDLA: al tocar, abre GoogleAppsFolderDialog en vez de lanzar packageName.
        val opensGoogleFolder: Boolean = false,
        // CDD/EDLA: al tocar, no hace nada real — solo muestra un aviso de "Desactivado por
        // el admin" (icono puramente visual, sin GMS instalado en este entorno).
        val disabledByAdmin: Boolean = false
    )

    /**
     * Construye un widget visual para los entornos predefinidos.
     */
    private fun buildCorporateWidget(data: CorpWidgetData): View {
        val widgetView = LayoutInflater.from(requireContext()).inflate(R.layout.item_corporate_widget, null)
        val container = widgetView.findViewById<FrameLayout>(R.id.widget_content_container)
        val content = LayoutInflater.from(requireContext()).inflate(data.layoutRes, container, false)

        // Configuración específica para el layout genérico de widgets corporativos
        if (data.layoutRes == R.layout.widget_corporate_generic) {
            content.findViewById<TextView>(R.id.tv_corp_generic_title)?.text = data.title ?: data.header
            val iconView = content.findViewById<ImageView>(R.id.iv_corp_generic_icon)
            when {
                data.folderPreviewPackages != null -> {
                    iconView?.setImageDrawable(
                        FolderIconUtil.buildFolderPreviewIcon(requireContext(), data.folderPreviewPackages, sizeDp = 44)
                    )
                    // El slot de 90dp solo deja ~54dp de alto para icono + texto: agranda el
                    // icono pero recorta su margen inferior para que el título no se salga.
                    (iconView?.layoutParams as? LinearLayout.LayoutParams)?.apply {
                        width = dpToPx(45)
                        height = dpToPx(45)
                        bottomMargin = dpToPx(2)
                    }?.let { iconView.layoutParams = it }
                }
                data.useRealAppIcon && data.packageName != null -> {
                    val realIcon = cachedAppIcon(data.packageName)
                    if (realIcon != null) iconView?.setImageDrawable(realIcon) else iconView?.setImageResource(data.iconRes)
                }
                else -> iconView?.setImageResource(data.iconRes)
            }
        }

        container.addView(content)

        when {
            data.disabledByAdmin -> {
                widgetView.setOnClickListener {
                    AdminDisabledDialog.newInstance().show(childFragmentManager, "admin_disabled")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }
            data.opensGoogleFolder -> {
                widgetView.setOnClickListener {
                    GoogleAppsFolderDialog.newInstance().show(childFragmentManager, "google_apps_folder")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }
            data.packageName != null -> {
                widgetView.setOnClickListener { launchPackage(data.packageName) }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }
        }

        return widgetView
    }

    private fun playStoreWidgetData() = CorpWidgetData(
        header = getString(R.string.play_store),
        iconRes = R.drawable.google_play_store_logo_svgrepo_com,
        layoutRes = R.layout.widget_corporate_generic,
        title = getString(R.string.play_store),
        packageName = "com.android.vending",
        useRealAppIcon = true,
        disabledByAdmin = true
    )

    private fun googleAppsFolderWidgetData() = CorpWidgetData(
        header = getString(R.string.google_apps_folder_title),
        iconRes = R.drawable.ic_google_apps_folder,
        layoutRes = R.layout.widget_corporate_generic,
        title = getString(R.string.google_apps_folder_title),
        folderPreviewPackages = listOf(
            "com.google.android.gm" to R.drawable.gmail_svgrepo_com,
            "com.google.android.apps.maps" to R.drawable.google_maps_platform_svgrepo_com,
            "com.google.android.youtube" to R.drawable.ic_brand_youtube_official,
            "com.google.android.apps.photos" to R.drawable.google_calendar_svgrepo_com
        ),
        disabledByAdmin = true
    )

    /**
     * Obtiene el slot (hueco) de aplicación para una categoría e índice específicos.
     */
    private fun slotView(catName: String, idx: Int): FrameLayout {
        val saved = viewModel.getShortcut(catName, idx)
        return if (saved != null) buildSavedSlot(catName, idx, saved)
        else buildAddButton(catName, idx)
    }

    /**
     * Construye un slot ocupado por una aplicación.
     */
    private fun buildSavedSlot(
        catName: String,
        idx: Int,
        shortcut: Shortcut
    ): FrameLayout {
        val view = roundedSlot(R.color.slot_bg_saved)

        val icon = cachedAppIcon(shortcut.packageName)

        val iv = ImageView(requireContext()).apply {
            if (icon != null) setImageDrawable(icon)
            else setImageResource(R.drawable.ic_android)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        iv.layoutParams = FrameLayout.LayoutParams(dpToPx(80), dpToPx(80), Gravity.CENTER)
        view.addView(iv)

        view.setOnClickListener { launchPackage(shortcut.packageName) }

        view.setOnLongClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_shortcut_title)
                .setMessage(getString(R.string.dialog_delete_shortcut_msg, shortcut.label))
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.removeShortcut(catName, idx)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            true
        }
        return view
    }

    /**
     * Construye un botón de "+" para añadir una aplicación a un slot vacío.
     */
    private fun buildAddButton(catName: String, idx: Int): FrameLayout {
        val view = roundedSlot(R.color.slot_bg_empty)
        val iv = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_add_plus)
            imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.slot_add_icon_tint))
        }
        iv.layoutParams = FrameLayout.LayoutParams(dpToPx(48), dpToPx(48), Gravity.CENTER)
        view.addView(iv)
        view.setOnClickListener {
            pickerLauncher.launch(Intent(requireContext(), AppPickerActivity::class.java).apply {
                putExtra(AppPickerActivity.EXTRA_CATEGORY, catName)
                putExtra(AppPickerActivity.EXTRA_SLOT_INDEX, idx)
            })
        }
        return view
    }

    /** Helper para crear un FrameLayout con esquinas redondeadas. */
    private fun roundedSlot(colorRes: Int): FrameLayout =
        FrameLayout(requireContext()).apply {
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                setColor(requireContext().getColor(colorRes))
                cornerRadius = dpToPx(20).toFloat()
            }
        }

    /** Construye la tarjeta para añadir widgets. */
    private fun buildAddWidgetCard(): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(requireContext().getColor(R.color.add_card_bg))
                cornerRadius = dpToPx(20).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                (requireActivity() as? MainActivity)?.launchWidgetPicker()
            }
        }
        card.layoutParams = LinearLayout.LayoutParams(dpToPx(172), dpToPx(110)).apply {
            topMargin = dpToPx(8)
            bottomMargin = dpToPx(8)
        }

        val iv = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_add_plus)
            imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.slot_add_icon_tint))
        }
        iv.layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42)).apply { gravity = Gravity.CENTER_HORIZONTAL }

        val tv = TextView(requireContext()).apply {
            setText(R.string.add_widget)
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.add_card_text))
        }
        tv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dpToPx(8)
        }

        card.addView(iv)
        card.addView(tv)
        return card
    }

    /** Construye la tarjeta para añadir una nueva categoría. */
    private fun buildNewCategoryCard(): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(requireContext().getColor(R.color.add_card_bg))
                cornerRadius = dpToPx(20).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { showAddCategoryDialog() }
        }
        card.layoutParams = LinearLayout.LayoutParams(dpToPx(172), dpToPx(110)).also {
            it.topMargin = dpToPx(8)
            it.bottomMargin = dpToPx(8)
        }

        val iv = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_add_plus)
            imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.slot_add_icon_tint))
        }
        iv.layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42)).also { it.gravity = Gravity.CENTER_HORIZONTAL }

        val tv = TextView(requireContext()).apply {
            setText(R.string.new_category)
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.add_card_text))
        }
        tv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also {
            it.gravity = Gravity.CENTER_HORIZONTAL
            it.topMargin = dpToPx(8)
        }

        card.addView(iv)
        card.addView(tv)
        return card
    }

    private fun showAddCategoryDialog() {
        val et = EditText(requireContext()).apply {
            setHint(R.string.dialog_new_category_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_new_category_title)
            .setView(et)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = et.text.toString().trim().uppercase()
                if (name.isNotEmpty()) viewModel.addCategory(name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteCategoryDialog(catName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_category_title)
            .setMessage(getString(R.string.dialog_delete_category_msg, catName))
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.removeCategory(catName) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Lanza una aplicación por su nombre de paquete, aplicando redirecciones de seguridad
     * si se encuentra en un entorno corporativo (OFFICE).
     */
    private fun launchPackage(pkg: String) {
        var targetPkg = pkg

        // Lógica de redirección a Authenticator para apps de Microsoft en entorno OFFICE
        if (viewModel.uiState.value.currentEnvironment == Environment.OFFICE && isMicrosoftPackage(pkg)) {
            if (pkg != "com.azure.authenticator" && !DeviceAccountUtil.hasMicrosoftAccount(requireContext())) {
                targetPkg = "com.azure.authenticator"
                Toast.makeText(requireContext(), R.string.error_auth_ms_authenticator, Toast.LENGTH_LONG).show()
            }
        }

        val pm = requireContext().packageManager
        val intent = pm.getLaunchIntentForPackage(targetPkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            DockOverlayService.minimize()
            startActivity(intent)
            requireActivity().moveTaskToBack(true)
        } else {
            val errorMsg = if (targetPkg == "com.azure.authenticator") getString(R.string.error_ms_authenticator_not_installed) else getString(R.string.error_app_not_installed)
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isMicrosoftPackage(pkg: String): Boolean {
        val microsoftPrefixes = listOf("com.microsoft.", "com.azure.authenticator")
        return microsoftPrefixes.any { pkg.startsWith(it) } || pkg == "com.microsoft.skydrive"
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
