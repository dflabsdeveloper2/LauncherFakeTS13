package com.orbys.launcherfakets13.ui.home

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
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
import com.orbys.launcherfakets13.databinding.FragmentCategoryBinding
import com.orbys.launcherfakets13.domain.model.Environment
import com.orbys.launcherfakets13.domain.model.Shortcut
import com.orbys.launcherfakets13.ui.dialog.AdminDisabledDialog
import com.orbys.launcherfakets13.ui.dialog.ClockDialog
import com.orbys.launcherfakets13.ui.dialog.FakeGoogleCalendarDialogFragment
import com.orbys.launcherfakets13.ui.dialog.FakeOutlookDialogFragment
import com.orbys.launcherfakets13.ui.dialog.FocusModeDialog
import com.orbys.launcherfakets13.ui.dialog.GoogleAppsFolderDialog
import com.orbys.launcherfakets13.ui.dialog.GoogleLoginDialog
import com.orbys.launcherfakets13.ui.dialog.MicrosoftValidationDialog
import com.orbys.launcherfakets13.ui.dialog.NameSelectorDialog
import com.orbys.launcherfakets13.ui.dialog.RemoteModeDialog
import com.orbys.launcherfakets13.ui.dialog.SonometerDialog
import com.orbys.launcherfakets13.ui.dialog.TimerDialog
import com.orbys.launcherfakets13.ui.dialog.TranslateDialog
import com.orbys.launcherfakets13.ui.dialog.WeatherDialog
import com.orbys.launcherfakets13.ui.picker.AppPickerActivity
import com.orbys.launcherfakets13.util.FolderIconUtil
import com.orbys.launcherfakets13.util.viewBinding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
            Environment.OFFICE,
            Environment.SHOWROOM
        )

        for (catName in categories) {
            val card = inflater.inflate(R.layout.item_category_card, container, false)
            
            // Margen entre tarjetas de 8dp
            (card.layoutParams as? LinearLayout.LayoutParams)?.apply {
                marginEnd = dpToPx(8)
                card.layoutParams = this
            }

            val rowTop = card.findViewById<LinearLayout>(R.id.slots_row_top)
            val rowBot = card.findViewById<LinearLayout>(R.id.slots_row_bottom)

            // Título de categoría - Long press para eliminar si es personalizable
            card.findViewById<TextView>(R.id.tv_category_title).apply {
                text = catName
                if (!isHardcoded) {
                    setOnLongClickListener { showDeleteCategoryDialog(catName); true }
                }
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
        val slotPx = dpToPx(86)
        val gapPx = dpToPx(10)

        // Definición de widgets predefinidos por entorno y categoría
        val widgets = when (env) {
            Environment.CORPORATE -> when (catName) {
                "General" -> listOf(
                    CorpWidgetData(
                        "RELOJ",
                        R.drawable.ic_recents,
                        R.layout.widget_corporate_clock,
                        headerIconRes = R.drawable.ic_clock
                    ),
                    CorpWidgetData(
                        "AGENDA",
                        R.drawable.ic_cat_calendar,
                        R.layout.widget_corporate_calendar,
                        headerIconRes = R.drawable.ic_cat_calendar
                    ),
                    CorpWidgetData(
                        "CLIMA",
                        R.drawable.ic_cat_weather,
                        R.layout.widget_corporate_weather,
                        headerIconRes = R.drawable.ic_brightness
                    ),
                    CorpWidgetData(
                        "CONCENTRACIÓN",
                        R.drawable.ic_cat_focus,
                        R.layout.widget_corporate_focus,
                        headerIconRes = R.drawable.ic_not_disturb
                    )
                )

                "Sala" -> listOf(
                    CorpWidgetData(
                        "TEMPORIZADOR",
                        R.drawable.ic_cat_timer,
                        R.layout.widget_corporate_timer,
                        headerIconRes = R.drawable.ic_cat_timer
                    ),
                    CorpWidgetData(
                        "VIDEOLLAMADA",
                        R.drawable.ic_cat_teams,
                        R.layout.widget_corporate_videocall,
                        headerIconRes = R.drawable.ic_cat_teams,
                        packageName = "com.microsoft.teams"
                    ),
                    CorpWidgetData(
                        "ESHARE",
                        R.drawable.ic_cat_mirroring,
                        R.layout.widget_corporate_app,
                        subtitle = "Compartir pantalla",
                        packageName = "com.ecloud.eshare.server"
                    ),
                    CorpWidgetData(
                        "UNIRSE POR QR",
                        R.drawable.ic_qr,
                        R.layout.widget_corporate_qr,
                        headerIconRes = R.drawable.ic_qr
                    )
                )

                "Trabajo" -> listOf(
                    CorpWidgetData(
                        "CORREO",
                        R.drawable.ic_cat_mail,
                        R.layout.widget_corporate_app,
                        subtitle = "4 sin leer",
                        packageName = "com.microsoft.office.outlook"
                    ),
                    CorpWidgetData(
                        "ALMACENAMIENTO",
                        R.drawable.ic_cat_drive,
                        R.layout.widget_corporate_app,
                        subtitle = "Recientes",
                        packageName = "com.microsoft.skydrive"
                    ),
                    CorpWidgetData(
                        "DOCUMENTOS",
                        R.drawable.ic_cat_doc,
                        R.layout.widget_corporate_app,
                        subtitle = "3 recientes",
                        packageName = "com.microsoft.office.word"
                    ),
                    CorpWidgetData(
                        "NOTAS",
                        R.drawable.ic_cat_note,
                        R.layout.widget_corporate_notes,
                        headerIconRes = R.drawable.ic_cat_note
                    )
                )
                // CDD/EDLA: icono fijo de Play Store + carpeta de apps de Google, sustituyendo
                // el contenido anterior de esta categoría.
                "Colaboración" -> listOf(
                    playStoreWidgetData(),
                    googleAppsFolderWidgetData(),
                    null,
                    null
                )

                else -> emptyList()
            }

            Environment.GOOGLE -> when (catName) {
                "General" -> listOf(
                    CorpWidgetData(
                        "RELOJ",
                        R.drawable.ic_recents,
                        R.layout.widget_corporate_clock,
                        headerIconRes = R.drawable.ic_clock
                    ),
                    CorpWidgetData(
                        "GOOGLE CALENDAR",
                        R.drawable.ic_cat_calendar,
                        R.layout.widget_corporate_calendar,
                        headerIconRes = R.drawable.ic_cat_calendar
                    ),
                    CorpWidgetData(
                        "CLIMA",
                        R.drawable.ic_cat_weather,
                        R.layout.widget_corporate_weather,
                        headerIconRes = R.drawable.ic_brightness
                    ),
                    CorpWidgetData(
                        "CONCENTRACIÓN",
                        R.drawable.ic_cat_focus,
                        R.layout.widget_corporate_focus,
                        headerIconRes = R.drawable.ic_not_disturb
                    )
                )

                "Aula" -> listOf(
                    CorpWidgetData(
                        "TEMPORIZADOR",
                        R.drawable.ic_cat_timer,
                        R.layout.widget_corporate_timer,
                        headerIconRes = R.drawable.ic_cat_timer
                    ),
                    CorpWidgetData(
                        "SELECTOR DE ALUMNOS",
                        R.drawable.ic_cat_dice,
                        R.layout.widget_corporate_dice,
                        headerIconRes = R.drawable.ic_cat_dice
                    ),
                    CorpWidgetData(
                        "MEDIDOR DE RUIDO",
                        R.drawable.ic_cat_noise,
                        R.layout.widget_corporate_noise,
                        headerIconRes = R.drawable.ic_cat_noise
                    ),
                    CorpWidgetData(
                        "ORBYS TRANSLATE",
                        R.drawable.ic_cat_translate,
                        R.layout.widget_corporate_translate,
                        headerIconRes = R.drawable.ic_cat_translate
                    )
                )

                "Trabajo" -> listOf(
                    CorpWidgetData(
                        "GMAIL",
                        R.drawable.ic_cat_mail,
                        R.layout.widget_corporate_app,
                        subtitle = "4 sin leer",
                        packageName = "com.google.android.gm"
                    ),
                    CorpWidgetData(
                        "GOOGLE DRIVE",
                        R.drawable.ic_cat_drive,
                        R.layout.widget_corporate_app,
                        subtitle = "Recientes",
                        packageName = "com.google.android.apps.docs"
                    ),
                    CorpWidgetData(
                        "MEET",
                        R.drawable.ic_cat_teams,
                        R.layout.widget_corporate_app,
                        subtitle = "Iniciar reunión",
                        packageName = "com.google.android.apps.tachyon"
                    ),
                    CorpWidgetData(
                        "DOCS",
                        R.drawable.ic_cat_doc,
                        R.layout.widget_corporate_app,
                        subtitle = "3 recientes",
                        packageName = "com.google.android.apps.docs.editors.docs"
                    )
                )

                "Colaboración" -> listOf(
                    playStoreWidgetData(),
                    googleAppsFolderWidgetData(),
                    null,
                    null
                )

                else -> emptyList()
            }

            Environment.OFFICE -> when (catName) {
                "General" -> listOf(
                    CorpWidgetData(
                        "RELOJ",
                        R.drawable.ic_recents,
                        R.layout.widget_corporate_clock,
                        headerIconRes = R.drawable.ic_clock
                    ),
                    CorpWidgetData(
                        "CALENDARIO",
                        R.drawable.ic_cat_calendar,
                        R.layout.widget_corporate_calendar,
                        headerIconRes = R.drawable.ic_cat_calendar
                    ),
                    CorpWidgetData(
                        "CLIMA",
                        R.drawable.ic_cat_weather,
                        R.layout.widget_corporate_weather,
                        headerIconRes = R.drawable.ic_brightness
                    ),
                    CorpWidgetData(
                        "CONCENTRACIÓN",
                        R.drawable.ic_cat_focus,
                        R.layout.widget_corporate_focus,
                        headerIconRes = R.drawable.ic_not_disturb
                    )
                )

                "Aula" -> listOf(
                    CorpWidgetData(
                        "TEMPORIZADOR",
                        R.drawable.ic_cat_timer,
                        R.layout.widget_corporate_timer,
                        headerIconRes = R.drawable.ic_cat_timer
                    ),
                    CorpWidgetData(
                        "SELECTOR DE ALUMNOS",
                        R.drawable.ic_cat_dice,
                        R.layout.widget_corporate_dice,
                        headerIconRes = R.drawable.ic_cat_dice
                    ),
                    CorpWidgetData(
                        "MEDIDOR DE RUIDO",
                        R.drawable.ic_cat_noise,
                        R.layout.widget_corporate_noise,
                        headerIconRes = R.drawable.ic_cat_noise
                    ),
                    CorpWidgetData(
                        "ORBYS TRANSLATE",
                        R.drawable.ic_cat_translate,
                        R.layout.widget_corporate_translate,
                        headerIconRes = R.drawable.ic_cat_translate
                    )
                )

                "Trabajo" -> listOf(
                    CorpWidgetData(
                        "OUTLOOK",
                        R.drawable.ic_cat_mail,
                        R.layout.widget_corporate_app,
                        subtitle = "4 sin leer",
                        packageName = "com.microsoft.office.outlook"
                    ),
                    CorpWidgetData(
                        "ONEDRIVE",
                        R.drawable.ic_cat_drive,
                        R.layout.widget_corporate_app,
                        subtitle = "Recientes",
                        packageName = "com.microsoft.skydrive"
                    ),
                    CorpWidgetData(
                        "TEAMS",
                        R.drawable.ic_cat_teams,
                        R.layout.widget_corporate_app,
                        subtitle = "Iniciar reunión",
                        packageName = "com.microsoft.teams"
                    ),
                    CorpWidgetData(
                        "WORD",
                        R.drawable.ic_cat_doc,
                        R.layout.widget_corporate_app,
                        subtitle = "3 recientes",
                        packageName = "com.microsoft.office.word"
                    )
                )

                "Colaboración" -> listOf(
                    playStoreWidgetData(),
                    googleAppsFolderWidgetData(),
                    null,
                    null
                )

                else -> emptyList()
            }

            Environment.SHOWROOM -> when (catName) {
                "General" -> listOf(
                    CorpWidgetData(
                        header = "RELOJ",
                        iconRes = R.drawable.ic_recents,
                        layoutRes = R.layout.widget_corporate_clock,
                        headerIconRes = R.drawable.ic_info,
                        packageName = "com.google.android.deskclock"
                    ),
                    null,
                    CorpWidgetData(
                        header = "DIGITAL SIGNAGE",
                        iconRes = R.drawable.ic_play,
                        layoutRes = R.layout.widget_showroom_ds,
                        headerIconRes = R.drawable.ic_play,
                        packageName = "com.example.sampleds"
                    ),
                    null
                )

                else -> emptyList()
            }
        }

        // Infla y posiciona los widgets en la rejilla de 2x2
        widgets.forEachIndexed { i, data ->
            if (data == null && env == Environment.SHOWROOM) return@forEachIndexed

            val view =
                if (data != null) buildCorporateWidget(data) else FrameLayout(requireContext())

            // Tamaño estándar de 90dp, pero la carpeta de Google se hace 3dp más grande (93dp)
            val currentSlotPx = if (data?.opensGoogleFolder == true) dpToPx(93) else slotPx

            val lp = if (env == Environment.SHOWROOM && data != null) {
                LinearLayout.LayoutParams(dpToPx(182), slotPx)
            } else {
                LinearLayout.LayoutParams(currentSlotPx, currentSlotPx)
            }

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
        val subtitle: String? = null,
        val headerIconRes: Int? = null,
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
        val widgetView =
            LayoutInflater.from(requireContext()).inflate(R.layout.item_corporate_widget, null)
        val container = widgetView.findViewById<FrameLayout>(R.id.widget_content_container)
        val content =
            LayoutInflater.from(requireContext()).inflate(data.layoutRes, container, false)

        // Configuración de la cabecera
        widgetView.findViewById<TextView>(R.id.tv_widget_header)?.text = data.header
        val headerIconView = widgetView.findViewById<ImageView>(R.id.iv_widget_header_icon)
        if (data.headerIconRes != null) {
            headerIconView?.setImageResource(data.headerIconRes)
            headerIconView?.visibility = View.VISIBLE
        } else {
            headerIconView?.visibility = View.GONE
        }

        // Configuración específica para el layout genérico de widgets corporativos
        if (data.layoutRes == R.layout.widget_corporate_generic) {
            content.findViewById<TextView>(R.id.tv_corp_generic_title)?.text =
                data.title ?: data.header
            val iconView = content.findViewById<ImageView>(R.id.iv_corp_generic_icon)
            configureGenericIcon(iconView, data)
        } else if (data.layoutRes == R.layout.widget_corporate_app) {
            content.findViewById<TextView>(R.id.tv_corp_app_subtitle)?.text = data.subtitle
            val iconView = content.findViewById<ImageView>(R.id.iv_corp_app_icon)
            if (data.useRealAppIcon && data.packageName != null) {
                val realIcon = cachedAppIcon(data.packageName)
                if (realIcon != null) iconView?.setImageDrawable(realIcon) else iconView?.setImageResource(data.iconRes)
            } else {
                iconView?.setImageResource(data.iconRes)
            }
        } else if (data.layoutRes == R.layout.widget_showroom_ds) {
            content.findViewById<TextView>(R.id.tv_showroom_ds_subtitle)?.text = data.title ?: "Digital signage"
            content.findViewById<ImageView>(R.id.iv_showroom_ds_icon)?.setImageResource(data.iconRes)
        }

        container.addView(content)

        val isClock = data.header == getString(R.string.widget_clock) || data.header == "RELOJ"
        val isWeather = data.header == getString(R.string.widget_weather) || data.header == "CLIMA"
        val isSonometer = data.header == getString(R.string.widget_noise_meter) || data.header == "SONÓMETRO" || data.header == "MEDIDOR DE RUIDO"
        val isStudentSelector = data.header == getString(R.string.widget_student_selector) || data.header == "SELECTOR ALUMNOS" || data.header == "SELECTOR DE ALUMNOS"
        val isTimer = data.header == getString(R.string.widget_timer) || data.header == "TEMPORIZADOR"
        val isTranslate = data.header == getString(R.string.widget_orbys_translate) || data.header == "TRADUCTOR" || data.header == "ORBYS TRANSLATE"
        val isFocus = data.header == getString(R.string.widget_focus) || data.header == "CONCENTRACIÓN"
        val isCalendar = data.header == "CALENDARIO" || data.header == "GOOGLE CALENDAR" || data.packageName == "com.google.android.calendar"

        when {
            isClock -> {
                widgetView.setOnClickListener {
                    ClockDialog.newInstance().show(childFragmentManager, "clock_dialog")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            isCalendar -> {
                val env = viewModel.uiState.value.currentEnvironment
                widgetView.setOnClickListener {
                    if (env == Environment.GOOGLE) {
                        FakeGoogleCalendarDialogFragment.newInstance().show(childFragmentManager, "fake_calendar")
                    } else {
                        FakeOutlookDialogFragment.newInstance().show(childFragmentManager, "fake_outlook")
                    }
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            isWeather -> {
                widgetView.setOnClickListener {
                    WeatherDialog.newInstance().show(childFragmentManager, "weather_dialog")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            isSonometer -> {
                widgetView.setOnClickListener {
                    SonometerDialog.newInstance().show(childFragmentManager, "sonometer_dialog")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            isStudentSelector -> {
                widgetView.setOnClickListener {
                    NameSelectorDialog.newInstance().show(childFragmentManager, "name_selector")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            isTimer -> {
                widgetView.setOnClickListener {
                    TimerDialog.newInstance().show(childFragmentManager, "timer_dialog")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            isTranslate -> {
                widgetView.setOnClickListener {
                    TranslateDialog.newInstance().show(childFragmentManager, "translate_dialog")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            isFocus -> {
                widgetView.setOnClickListener {
                    FocusModeDialog.newInstance().show(childFragmentManager, "focus_mode")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            data.disabledByAdmin -> {
                widgetView.setOnClickListener {
                    AdminDisabledDialog.newInstance().show(childFragmentManager, "admin_disabled")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            data.opensGoogleFolder -> {
                widgetView.setOnClickListener {
                    GoogleAppsFolderDialog.newInstance()
                        .show(childFragmentManager, "google_apps_folder")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            data.packageName != null -> {
                widgetView.setOnClickListener { launchPackage(data.packageName) }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }

            else -> {
                // Bloqueo por defecto para cualquier otro widget si no es una de las excepciones
                widgetView.setOnClickListener {
                    RemoteModeDialog.newInstance().show(childFragmentManager, "remote_mode")
                }
                widgetView.isClickable = true
                widgetView.isFocusable = true
            }
        }

        return widgetView
    }

    private fun playStoreWidgetData() = CorpWidgetData(
        header = getString(R.string.play_store),
        iconRes = R.drawable.ic_play_store,
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
            "com.google.android.gm" to R.drawable.ic_gmail,
            "com.google.android.apps.maps" to R.drawable.ic_google_maps,
            "com.google.android.youtube" to R.drawable.ic_brand_youtube_official,
            "com.google.android.apps.photos" to R.drawable.ic_brand_photos
        ),
        disabledByAdmin = true
    )

    /**
     * Obtiene el slot (hueco) de aplicación para una categoría e índice específicos.
     */
    private fun configureGenericIcon(iconView: ImageView?, data: CorpWidgetData) {
        when {
            data.folderPreviewPackages != null -> {
                val sizeDp = if (data.opensGoogleFolder) 47 else 44
                iconView?.setImageDrawable(
                    FolderIconUtil.buildFolderPreviewIcon(
                        requireContext(),
                        data.folderPreviewPackages,
                        sizeDp = sizeDp
                    )
                )
                (iconView?.layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = if (data.opensGoogleFolder) dpToPx(45) else dpToPx(42)
                    height = if (data.opensGoogleFolder) dpToPx(45) else dpToPx(42)
                    bottomMargin = dpToPx(2)
                }?.let { iconView.layoutParams = it }
            }

            data.useRealAppIcon && data.packageName != null -> {
                val realIcon = cachedAppIcon(data.packageName)
                if (realIcon != null) iconView?.setImageDrawable(realIcon) else iconView?.setImageResource(
                    data.iconRes
                )
            }

            else -> iconView?.setImageResource(data.iconRes)
        }
    }

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
            RemoteModeDialog.newInstance().show(childFragmentManager, "remote_mode")
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
            imageTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.slot_add_icon_tint))
        }
        iv.layoutParams = FrameLayout.LayoutParams(dpToPx(48), dpToPx(48), Gravity.CENTER)
        view.addView(iv)
        view.setOnClickListener {
            RemoteModeDialog.newInstance().show(childFragmentManager, "remote_mode")
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
                RemoteModeDialog.newInstance().show(childFragmentManager, "remote_mode")
            }
        }
        card.layoutParams = LinearLayout.LayoutParams(dpToPx(172), dpToPx(110)).apply {
            topMargin = dpToPx(8)
            bottomMargin = dpToPx(8)
        }

        val iv = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_add_plus)
            imageTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.slot_add_icon_tint))
        }
        iv.layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
            .apply { gravity = Gravity.CENTER_HORIZONTAL }

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
            setOnClickListener {
                RemoteModeDialog.newInstance().show(childFragmentManager, "remote_mode")
            }
        }
        card.layoutParams = LinearLayout.LayoutParams(dpToPx(172), dpToPx(110)).also {
            it.topMargin = dpToPx(8)
            it.bottomMargin = dpToPx(8)
        }

        val iv = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_add_plus)
            imageTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.slot_add_icon_tint))
        }
        iv.layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
            .also { it.gravity = Gravity.CENTER_HORIZONTAL }

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
        val isClock = pkg == "com.google.android.deskclock" || pkg == "com.android.deskclock"
        if (isClock) {
            ClockDialog.newInstance().show(childFragmentManager, "clock_dialog")
            return
        }

        if (pkg == "com.google.android.calendar") {
            FakeGoogleCalendarDialogFragment.newInstance().show(childFragmentManager, "fake_calendar")
            return
        }

        if (isGooglePackage(pkg)) {
            GoogleLoginDialog.newInstance().show(childFragmentManager, "google_login")
            return
        }

        if (pkg == "com.microsoft.office.outlook") {
            FakeOutlookDialogFragment.newInstance().show(childFragmentManager, "fake_outlook")
            return
        }

        if (isMicrosoftPackage(pkg)) {
            MicrosoftValidationDialog.newInstance().show(childFragmentManager, "ms_validation")
            return
        }

        if (pkg == "com.example.sampleds") {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "App not found: $pkg", Toast.LENGTH_SHORT).show()
            }
            return
        }

        RemoteModeDialog.newInstance().show(childFragmentManager, "remote_mode")
    }

    private fun isMicrosoftPackage(pkg: String): Boolean {
        val microsoftPrefixes = listOf("com.microsoft.", "com.azure.authenticator")
        return microsoftPrefixes.any { pkg.startsWith(it) } || pkg == "com.microsoft.skydrive"
    }

    private fun isGooglePackage(pkg: String): Boolean {
        if (pkg == "com.google.android.deskclock" || pkg == "com.android.deskclock") return false
        val googlePrefixes = listOf("com.google.android.", "com.android.vending")
        return googlePrefixes.any { pkg.startsWith(it) }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
