package com.udacity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.udacity.databinding.ActivityDetailBinding

const val EXTRA_FILENAME = "filename"
const val EXTRA_DOWNLOAD_STATUS = "downloadstatus"

class DetailActivity : AppCompatActivity() {

    lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        setSupportActionBar(binding.toolbar)

        val viewModel: DetailViewModel = ViewModelProvider(this).get(DetailViewModel::class.java)
        val fileName = intent?.extras?.getString(EXTRA_FILENAME, getString(R.string.unknown))
        val downloadStatusText = intent?.extras?.getString(EXTRA_DOWNLOAD_STATUS, getString(R.string.unknown))
        fileName?.let { viewModel.setFileName(it) }
        downloadStatusText?.let {
            viewModel.setDownloadStatusText(it) }
        binding.detailContent.viewModel = viewModel

    }

    fun onOkButtonClick(view: View) {
        finish()
    }
}
