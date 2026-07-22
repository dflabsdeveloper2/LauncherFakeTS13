package com.orbys.launcherfakets13.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbys.launcherfakets13.data.repository.DefaultShortcuts
import com.orbys.launcherfakets13.domain.model.*
import com.orbys.launcherfakets13.domain.repository.WallpaperRepository
import com.orbys.launcherfakets13.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado inmutable de la pantalla principal (Main).
 */
data class MainState(
    val categories: List<String> = emptyList(),
    val widgets: List<WidgetInfo> = emptyList(),
    val refreshTrigger: Int = 0,
    val isWifiConnected: Boolean = false,
    val isEthernetConnected: Boolean = false,
    val isHotspotEnabled: Boolean = false,
    val isUsbConnected: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val currentEnvironment: Environment = Environment.OFFICE,
    val wallpaperType: WallpaperType = WallpaperType.DEFAULT,
)

/**
 * ViewModel principal que orquesta el estado de la pantalla de inicio y el dock.
 *
 * Sigue el patrón MVVM utilizando StateFlow para exponer un estado reactivo e inmutable a la UI.
 * Delega la lógica de negocio en UseCases de la capa de dominio.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val setCategoriesUseCase: SetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val removeCategoryUseCase: RemoveCategoryUseCase,
    private val getWidgetsUseCase: GetWidgetsUseCase,
    private val addWidgetUseCase: AddWidgetUseCase,
    private val updateWidgetUseCase: UpdateWidgetUseCase,
    private val removeWidgetUseCase: RemoveWidgetUseCase,
    private val setShortcutUseCase: SetShortcutUseCase,
    private val removeShortcutUseCase: RemoveShortcutUseCase,
    private val getShortcutUseCase: GetShortcutUseCase,
    private val setDefaultWallpaperUseCase: SetDefaultWallpaperUseCase,
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainState())
    /** Estado reactivo observable por la View. */
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    init {
        loadData()
        provisionDefaults()
    }

    /**
     * Asegura que existan accesos directos por defecto si no han sido configurados previamente.
     */
    private fun provisionDefaults() {
        _uiState.update { it.copy(wallpaperType = wallpaperRepository.getWallpaperType()) }
        val state = _uiState.value
        val env = state.currentEnvironment
        
        // Carga diferida de defaults si es necesario
        DefaultShortcuts.defaults[env]?.forEach { (category, shortcuts) ->
            shortcuts.forEachIndexed { index, shortcut ->
                if (getShortcutUseCase(category, index) == null) {
                    setShortcutUseCase(category, index, shortcut.packageName, shortcut.label)
                }
            }
        }
        loadData()
    }

    /**
     * Carga las categorías y widgets actuales del repositorio.
     */
    fun loadData() {
        viewModelScope.launch {
            val cats = getCategoriesUseCase()
            val widgets = getWidgetsUseCase()
            _uiState.update { it.copy(
                categories = cats, 
                widgets = widgets, 
                refreshTrigger = it.refreshTrigger + 1 
            ) }
        }
    }

    /**
     * Refresca todos los datos volátiles (apps, ajustes).
     */
    fun refreshAll() {
        loadData()
    }

    // --- Métodos de gestión de categorías ---

    fun addCategory(name: String) {
        addCategoryUseCase(name)
        loadData()
    }

    fun removeCategory(name: String) {
        removeCategoryUseCase(name)
        loadData()
    }

    // --- Métodos de gestión de widgets ---

    fun addWidget(id: Int, xDp: Int, yDp: Int, w: Int, h: Int) {
        addWidgetUseCase(id, xDp, yDp, w, h)
        loadData()
    }

    fun updateWidgetPosition(id: Int, x: Int, y: Int) {
        updateWidgetUseCase.updatePosition(id, x, y)
    }

    fun updateWidgetSize(id: Int, w: Int, h: Int) {
        updateWidgetUseCase.updateSize(id, w, h)
    }

    fun removeWidget(id: Int) {
        removeWidgetUseCase(id)
        loadData()
    }

    // --- Métodos de gestión de accesos directos (Shortcuts) ---

    fun setShortcut(category: String, index: Int, pkg: String, label: String) {
        setShortcutUseCase(category, index, pkg, label)
        loadData()
    }

    fun removeShortcut(category: String, index: Int) {
        removeShortcutUseCase(category, index)
        loadData()
    }

    fun getShortcut(category: String, index: Int) = getShortcutUseCase(category, index)

    // --- Métodos de actualización de estado de sistema (Conectividad) ---

    fun updateWifiStatus(connected: Boolean) {
        _uiState.update { it.copy(isWifiConnected = connected) }
    }

    fun updateEthernetStatus(connected: Boolean) {
        _uiState.update { it.copy(isEthernetConnected = connected) }
    }

    fun updateHotspotStatus(enabled: Boolean) {
        _uiState.update { it.copy(isHotspotEnabled = enabled) }
    }

    fun updateUsbStatus(connected: Boolean) {
        _uiState.update { it.copy(isUsbConnected = connected) }
    }

    fun updateBluetoothStatus(enabled: Boolean) {
        _uiState.update { it.copy(isBluetoothEnabled = enabled) }
    }

    /**
     * Cambia el entorno actual del launcher y aplica las configuraciones predeterminadas.
     *
     * @param environment El nuevo entorno a aplicar [Environment].
     */
    fun updateEnvironment(environment: Environment) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentEnvironment = environment) }
            
            // Aplicar categorías por defecto para el nuevo entorno
            setCategoriesUseCase(environment.defaultCategories)

            // Aplicar shortcuts por defecto del entorno
            DefaultShortcuts.defaults[environment]?.forEach { (category, shortcuts) ->
                shortcuts.forEachIndexed { index, shortcut ->
                    setShortcutUseCase(category, index, shortcut.packageName, shortcut.label)
                }
            }

            // Si estamos en modo DEFAULT, actualizamos también el wallpaper al del nuevo entorno
            if (wallpaperRepository.getWallpaperType() == WallpaperType.DEFAULT) {
                restoreDefaultWallpaper(environment)
            }

            loadData()
        }
    }

    /**
     * Comprueba si el fondo de pantalla ha cambiado externamente (vía picker del sistema).
     *
     * @param currentSystemId El ID actual del fondo de pantalla del sistema.
     */
    fun checkWallpaperConsistency(currentSystemId: Int) {
        val lastId = wallpaperRepository.getLastWallpaperId()
        if (lastId != -1 && (lastId != currentSystemId)) {
            // El ID cambió fuera de nuestro control (vía Picker)
            wallpaperRepository.setWallpaperType(WallpaperType.CUSTOM)
            wallpaperRepository.setLastWallpaperId(currentSystemId)
            _uiState.update { it.copy(wallpaperType = WallpaperType.CUSTOM) }
        }
    }

    /**
     * Restaura el fondo de pantalla predeterminado para el entorno dado.
     *
     * @param environment El entorno del cual tomar el fondo predeterminado.
     */
    fun restoreDefaultWallpaper(environment: Environment) {
        val resId = com.orbys.launcherfakets13.ui.util.EnvironmentMapper.getBackgroundRes(environment)
        setDefaultWallpaperUseCase(resId)
        _uiState.update { it.copy(wallpaperType = WallpaperType.DEFAULT) }
    }

    /**
     * Registra el ID actual del fondo de pantalla antes de lanzar el picker.
     */
    fun prepareForWallpaperChange(currentId: Int) {
        wallpaperRepository.setLastWallpaperId(currentId)
    }
}
