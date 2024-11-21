package com.juanarton.encnotes.core.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class GridSpacingItemDecoration(space: Int) : RecyclerView.ItemDecoration() {

    private val halfSpace = space / 2

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {

        val layoutManager = parent.layoutManager
        if (layoutManager is GridLayoutManager) {
            val spanCount = layoutManager.spanCount
            val position = parent.getChildAdapterPosition(view)
            val isFirstRow = position < spanCount
            val isFirstColumn = position % spanCount == 0

            outRect.top = if (isFirstRow) 0 else halfSpace
            outRect.left = if (isFirstColumn) 0 else halfSpace
            outRect.right = halfSpace
            outRect.bottom = halfSpace

        } else if (layoutManager is StaggeredGridLayoutManager) {
            if (parent.paddingLeft != halfSpace) {
                parent.setPadding(halfSpace, halfSpace, halfSpace, halfSpace)
                parent.clipToPadding = false
            }

            outRect.top = halfSpace
            outRect.bottom = halfSpace
            outRect.left = halfSpace
            outRect.right = halfSpace
        }
    }
}

