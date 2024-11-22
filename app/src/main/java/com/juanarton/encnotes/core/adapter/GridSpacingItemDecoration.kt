package com.juanarton.encnotes.core.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class GridSpacingItemDecoration(
    private val space: Int,
    private val topMarginFirstRow: Int = space,
    private val bottomMarginLastRow: Int = space
) : RecyclerView.ItemDecoration() {

    private val halfSpace = space / 2

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager
        val itemCount = parent.adapter?.itemCount ?: 0
        val position = parent.getChildAdapterPosition(view) // current item's position

        if (layoutManager is GridLayoutManager) {
            val spanCount = layoutManager.spanCount
            val isFirstRow = position < spanCount
            val isLastRow = position >= itemCount - spanCount

            outRect.top = if (isFirstRow) topMarginFirstRow else halfSpace
            outRect.left = if (position % spanCount == 0) space else halfSpace
            outRect.right = halfSpace
            outRect.bottom = if (isLastRow) bottomMarginLastRow else halfSpace

        } else if (layoutManager is StaggeredGridLayoutManager) {
            if (parent.paddingLeft != halfSpace) {
                parent.setPadding(halfSpace, halfSpace, halfSpace, halfSpace)
                parent.clipToPadding = false
            }

            val isFirstRow = position < layoutManager.spanCount
            val isLastRow = position >= itemCount - layoutManager.spanCount

            outRect.top = if (isFirstRow) topMarginFirstRow else halfSpace
            outRect.bottom = if (isLastRow) bottomMarginLastRow else halfSpace
            outRect.left = halfSpace
            outRect.right = halfSpace
        }
    }
}

