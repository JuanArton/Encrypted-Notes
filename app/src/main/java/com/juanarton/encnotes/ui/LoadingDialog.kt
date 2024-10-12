package com.juanarton.encnotes.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.juanarton.encnotes.R

class LoadingDialog(context: Context) {
    private var dialog: Dialog? = null

    init {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.loading_dialog, null)
        builder.setView(view)

        dialog = builder.create()
        dialog?.setCancelable(true)

        val widhtHeight = 120
        val dimension = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            widhtHeight.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        dialog?.window?.setLayout(
            dimension,
            dimension
        )
    }

    fun show() {
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
