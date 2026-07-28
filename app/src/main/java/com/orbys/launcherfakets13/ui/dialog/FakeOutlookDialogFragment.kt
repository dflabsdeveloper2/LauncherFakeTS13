package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

class FakeOutlookDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_fake_outlook)

            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            setCancelable(false)

            findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
                dismiss()
            }

            // Botón fake
            findViewById<Button>(R.id.btnValidate).setOnClickListener {
                // Sin funcionalidad
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setupDialogSize(R.fraction.dialog_width_medium, R.fraction.dialog_height_large)
    }

    companion object {
        fun newInstance() = FakeOutlookDialogFragment()
    }
}
