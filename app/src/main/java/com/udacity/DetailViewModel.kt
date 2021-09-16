package com.udacity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DetailViewModel : ViewModel() {

    private val _filename = MutableLiveData<String>()
    private val _downloadStatusText = MutableLiveData<String>()

    val filename: LiveData<String>
        get() = _filename

    val downloadStatus: LiveData<String>
        get() = _downloadStatusText

    fun setFileName(fileName: String) {
        _filename.value = fileName
    }

    fun setDownloadStatusText(downloadStatus: String) {
        _downloadStatusText.value = downloadStatus
    }
}