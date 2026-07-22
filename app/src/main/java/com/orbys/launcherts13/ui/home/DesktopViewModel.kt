package com.orbys.launcherts13.ui.home

import androidx.lifecycle.ViewModel
import com.orbys.launcherts13.domain.model.DesktopItemInfo
import com.orbys.launcherts13.domain.usecase.AddDesktopAppUseCase
import com.orbys.launcherts13.domain.usecase.AddDesktopWidgetUseCase
import com.orbys.launcherts13.domain.usecase.GetDesktopItemsUseCase
import com.orbys.launcherts13.domain.usecase.MoveDesktopItemUseCase
import com.orbys.launcherts13.domain.usecase.RemoveDesktopItemUseCase
import com.orbys.launcherts13.domain.usecase.RemoveDesktopWidgetByIdUseCase
import com.orbys.launcherts13.domain.usecase.ResizeDesktopItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado inmutable de la pantalla Desktop (grid con snap para apps + widgets).
 */
data class DesktopState(val items: List<DesktopItemInfo> = emptyList())

@HiltViewModel
class DesktopViewModel @Inject constructor(
    private val getDesktopItemsUseCase: GetDesktopItemsUseCase,
    private val addDesktopAppUseCase: AddDesktopAppUseCase,
    private val addDesktopWidgetUseCase: AddDesktopWidgetUseCase,
    private val moveDesktopItemUseCase: MoveDesktopItemUseCase,
    private val resizeDesktopItemUseCase: ResizeDesktopItemUseCase,
    private val removeDesktopItemUseCase: RemoveDesktopItemUseCase,
    private val removeDesktopWidgetByIdUseCase: RemoveDesktopWidgetByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DesktopState())
    val state: StateFlow<DesktopState> = _state.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        _state.update { it.copy(items = getDesktopItemsUseCase()) }
    }

    fun addApp(id: String, row: Int, col: Int, packageName: String, label: String) {
        addDesktopAppUseCase(id, row, col, packageName, label)
        loadItems()
    }

    fun addWidget(id: String, row: Int, col: Int, colSpan: Int, rowSpan: Int, appWidgetId: Int) {
        addDesktopWidgetUseCase(id, row, col, colSpan, rowSpan, appWidgetId)
        loadItems()
    }

    fun moveItem(id: String, row: Int, col: Int) {
        moveDesktopItemUseCase(id, row, col)
        loadItems()
    }

    fun resizeItem(id: String, colSpan: Int, rowSpan: Int) {
        resizeDesktopItemUseCase(id, colSpan, rowSpan)
        loadItems()
    }

    fun removeItem(id: String) {
        removeDesktopItemUseCase(id)
        loadItems()
    }

    fun removeFailedWidget(appWidgetId: Int) {
        removeDesktopWidgetByIdUseCase(appWidgetId)
        loadItems()
    }
}
