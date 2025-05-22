package org.cssnr.noaaweather.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cssnr.noaaweather.db.WeatherStation

class HomeViewModel : ViewModel() {

    val position = MutableLiveData<Int?>()
    val data = MutableLiveData<List<WeatherStation>>()

}
