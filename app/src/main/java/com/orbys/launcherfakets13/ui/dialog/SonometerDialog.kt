package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.dp
import com.orbys.launcherfakets13.ui.util.setupDialogSize

class SonometerDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_sonometer, null)

        view.findViewById<View>(R.id.btn_close_sonometer).setOnClickListener {
            dismiss()
        }

        val imageView = view.findViewById<ImageView>(R.id.iv_sonometer_gif)
        loadGif(imageView)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
                dialog.window?.setGravity(Gravity.CENTER)
                dialog.window?.attributes = dialog.window?.attributes?.apply {
                    y -= 30.dp
                }
            }
    }

    private fun loadGif(imageView: ImageView) {
        try {
            val source = ImageDecoder.createSource(resources, R.raw.sonometro)
            val drawable = ImageDecoder.decodeDrawable(source)
            imageView.setImageDrawable(drawable)
            if (drawable is AnimatedImageDrawable) {
                drawable.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onStart() {
        super.onStart()
        setupDialogSize(R.fraction.dialog_width_small)
    }

    companion object {
        fun newInstance() = SonometerDialog()
    }
}
