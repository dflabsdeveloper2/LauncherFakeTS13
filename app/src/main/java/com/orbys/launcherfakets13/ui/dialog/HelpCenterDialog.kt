package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

/**
 * Diálogo "Centro de ayuda" con pestañas FAQ, Teléfono y Chat.
 * Implementación simulada (fake).
 */
class HelpCenterDialog : DialogFragment() {

    private lateinit var tabFaq: TextView
    private lateinit var tabPhone: TextView
    private lateinit var tabChat: TextView

    private lateinit var layoutFaq: View
    private lateinit var layoutPhone: View
    private lateinit var layoutChat: View

    private lateinit var etChatInput: EditText
    private lateinit var llChatMessages: LinearLayout
    private lateinit var scrollChat: ScrollView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_help_center, null)

        initViews(view)
        setupTabs()
        setupFaqAccordion(view)
        setupChat(view)

        view.findViewById<View>(R.id.btn_close_help).setOnClickListener { dismiss() }

        return MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialogTheme)
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
                dialog.window?.setGravity(Gravity.CENTER)
            }
    }

    override fun onStart() {
        super.onStart()
        setupDialogSize(R.fraction.dialog_width_large, R.fraction.dialog_height_large)
    }

    private fun initViews(v: View) {
        tabFaq = v.findViewById(R.id.tab_help_faq)
        tabPhone = v.findViewById(R.id.tab_help_phone)
        tabChat = v.findViewById(R.id.tab_help_chat)

        layoutFaq = v.findViewById(R.id.layout_help_faq)
        layoutPhone = v.findViewById(R.id.layout_help_phone)
        layoutChat = v.findViewById(R.id.layout_help_chat)

        etChatInput = v.findViewById(R.id.et_chat_input)
        llChatMessages = v.findViewById(R.id.ll_chat_messages)
        scrollChat = v.findViewById(R.id.scroll_chat)
    }

    private fun setupTabs() {
        tabFaq.setOnClickListener { switchTab(0) }
        tabPhone.setOnClickListener { switchTab(1) }
        tabChat.setOnClickListener { switchTab(2) }
    }

    private fun switchTab(index: Int) {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_name_selector_button)
        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_timer_chip_unselected)
        val white = Color.WHITE
        val gray = ContextCompat.getColor(requireContext(), R.color.gray_800)

        tabFaq.apply {
            background = if (index == 0) selectedBg else unselectedBg
            setTextColor(if (index == 0) white else gray)
        }
        tabPhone.apply {
            background = if (index == 1) selectedBg else unselectedBg
            setTextColor(if (index == 1) white else gray)
        }
        tabChat.apply {
            background = if (index == 2) selectedBg else unselectedBg
            setTextColor(if (index == 2) white else gray)
        }

        layoutFaq.visibility = if (index == 0) View.VISIBLE else View.GONE
        layoutPhone.visibility = if (index == 1) View.VISIBLE else View.GONE
        layoutChat.visibility = if (index == 2) View.VISIBLE else View.GONE
    }

    private fun setupFaqAccordion(v: View) {
        val rows = listOf(
            v.findViewById<View>(R.id.row_faq_1) to (v.findViewById<TextView>(R.id.tv_faq_desc_1) to v.findViewById<TextView>(R.id.tv_faq_toggle_1)),
            v.findViewById<View>(R.id.row_faq_2) to (v.findViewById<TextView>(R.id.tv_faq_desc_2) to v.findViewById<TextView>(R.id.tv_faq_toggle_2)),
            v.findViewById<View>(R.id.row_faq_3) to (v.findViewById<TextView>(R.id.tv_faq_desc_3) to v.findViewById<TextView>(R.id.tv_faq_toggle_3)),
            v.findViewById<View>(R.id.row_faq_4) to (v.findViewById<TextView>(R.id.tv_faq_desc_4) to v.findViewById<TextView>(R.id.tv_faq_toggle_4)),
            v.findViewById<View>(R.id.row_faq_5) to (v.findViewById<TextView>(R.id.tv_faq_desc_5) to v.findViewById<TextView>(R.id.tv_faq_toggle_5))
        )

        rows.forEach { (row, refs) ->
            val (desc, toggle) = refs
            row.setOnClickListener {
                val isVisible = desc.visibility == View.VISIBLE
                desc.visibility = if (isVisible) View.GONE else View.VISIBLE
                toggle.text = if (isVisible) "+" else "−"
            }
        }
    }

    private fun setupChat(v: View) {
        v.findViewById<View>(R.id.btn_chat_send)?.setOnClickListener { sendMessage() }
        etChatInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun sendMessage() {
        RemoteModeDialog.newInstance().show(childFragmentManager, "remote_mode")
    }

    companion object {
        fun newInstance() = HelpCenterDialog()
    }
}
