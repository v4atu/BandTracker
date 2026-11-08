package com.v4atu.bandtracker

import android.app.Application

class BandTrackerApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
