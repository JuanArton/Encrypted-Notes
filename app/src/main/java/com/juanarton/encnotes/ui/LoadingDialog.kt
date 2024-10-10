package com.juanarton.encnotes.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.juanarton.encnotes.R

class LoadingDialog(context: Context) {
    private var dialog: Dialog? = null

    init {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.loading_dialog, null)
        builder.setView(view)

        dialog = builder.create()
        dialog?.setCancelable(false)
    }

    fun show() {
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
