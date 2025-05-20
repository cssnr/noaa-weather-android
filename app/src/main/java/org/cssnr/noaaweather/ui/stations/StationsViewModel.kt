package org.cssnr.noaaweather.ui.stations

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cssnr.noaaweather.db.WeatherStation

class StationsViewModel : ViewModel() {

    //val stationData = MutableLiveData<List<WeatherStation>>()

    val stationData: MutableLiveData<List<WeatherStation>> by lazy {
        MutableLiveData<List<WeatherStation>>()
    }

}
