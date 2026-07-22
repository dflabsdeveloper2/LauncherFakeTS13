package com.orbys.launcherfakets13.ui.picker

import com.orbys.launcherfakets13.domain.model.AppInfo
import com.orbys.launcherfakets13.domain.usecase.GetInstalledAppsUseCase
import com.orbys.launcherfakets13.ui.common.AppAdapter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.orbys.launcherfakets13.databinding.ActivityAppPickerBinding
import androidx.core.graphics.drawable.toDrawable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppPickerActivity : AppCompatActivity() {

    @Inject lateinit var getInstalledAppsUseCase: GetInstalledAppsUseCase

    private lateinit var binding: ActivityAppPickerBinding
    private var allApps: List<AppInfo> = emptyList()

    companion object {
        const val EXTRA_CATEGORY  = "extra_category"
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val EXTRA_TARGET_ROW = "extra_target_row"
        const val EXTRA_TARGET_COL = "extra_target_col"
        const val RESULT_PACKAGE  = "result_package"
        const val RESULT_LABEL    = "result_label"
        private const val NO_TARGET = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.apply {
            setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                     WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        binding = ActivityAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category  = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
        val slotIndex = intent.getIntExtra(EXTRA_SLOT_INDEX, 0)
        val targetRow = intent.getIntExtra(EXTRA_TARGET_ROW, NO_TARGET)
        val targetCol = intent.getIntExtra(EXTRA_TARGET_COL, NO_TARGET)

        binding.btnClose.setOnClickListener { finish() }
        binding.scrimBg.setOnClickListener  { finish() }

        allApps = getInstalledAppsUseCase()

        refreshList(allApps, category, slotIndex, targetRow, targetCol)

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.lowercase().orEmpty()
                val filtered = if (q.isEmpty()) allApps
                               else allApps.filter { it.label.toString().lowercase().contains(q) }
                refreshList(filtered, category, slotIndex, targetRow, targetCol)
            }
        })
    }

    private fun refreshList(
        apps: List<AppInfo>,
        category: String,
        slotIndex: Int,
        targetRow: Int,
        targetCol: Int
    ) {
        binding.rvApps.layoutManager = GridLayoutManager(this, 6)
        binding.rvApps.adapter = AppAdapter(
            apps = apps,
            onAppClick = { app ->
                setResult(RESULT_OK, Intent().apply {
                    putExtra(RESULT_PACKAGE,  app.packageName.toString())
                    putExtra(RESULT_LABEL,    app.label.toString())
                    putExtra(EXTRA_CATEGORY,  category)
                    putExtra(EXTRA_SLOT_INDEX, slotIndex)
                    if (targetRow != NO_TARGET) putExtra(EXTRA_TARGET_ROW, targetRow)
                    if (targetCol != NO_TARGET) putExtra(EXTRA_TARGET_COL, targetCol)
                })
                finish()
            }
        )
    }
}