package com.udacity

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("statusImage")
fun bindImage(imgView: ImageView, downloadStatus: String?) {
    downloadStatus?.let {
        when (downloadStatus) {
            "Successful" -> imgView.setImageResource(R.drawable.ic_check_circle_outline)
            "Failed" -> imgView.setImageResource(R.drawable.ic_error)
        }
    }
}

@BindingAdapter("statusColor")
fun bindTextColor(textView: TextView, downloadStatus: String?) {
    downloadStatus?.let {
        when (downloadStatus) {
            "Successful" -> textView.setTextColor(Color.parseColor("#004349"))
            "Failed" -> textView.setTextColor(Color.parseColor("#B00020"))
        }
    }
}