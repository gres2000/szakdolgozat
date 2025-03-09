package com.taskraze.myapplication.main.calendar.calendar.calendar_details

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class UsersRecyclerItemDecoration() : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        view.setPadding(0,0,0,0)
        view.scaleX = 0.8.toFloat()
        view.scaleY = 0.8.toFloat()

        // convert dp to pixel
        val dpWidth = 80 // dp
        val widthPixels = (dpWidth * Resources.getSystem().displayMetrics.density).toInt()

        val dpHeight = 100 // dp
        val heightPixels = (dpHeight * Resources.getSystem().displayMetrics.density).toInt()

        // Apply width and height to the view
        val layoutParams = view.layoutParams as RecyclerView.LayoutParams
        layoutParams.width = widthPixels
        layoutParams.height = heightPixels
        view.layoutParams = layoutParams
    }
}