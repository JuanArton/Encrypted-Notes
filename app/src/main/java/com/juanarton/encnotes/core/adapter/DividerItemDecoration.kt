package com.juanarton.encnotes.core.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecoration(private val dividerDrawable: Drawable) : RecyclerView.ItemDecoration() {
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val dividerTop = child.bottom + params.bottomMargin
            val dividerBottom = dividerTop + dividerDrawable.intrinsicHeight

            dividerDrawable.setBounds(child.left, dividerTop, child.right, dividerBottom)
            dividerDrawable.draw(c)
        }
    }
}
