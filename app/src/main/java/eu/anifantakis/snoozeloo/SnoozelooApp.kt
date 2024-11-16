package eu.anifantakis.snoozeloo

import android.app.Application
import eu.anifantakis.snoozeloo.di.appModule
import eu.anifantakis.snoozeloo.di.dataModule
import eu.anifantakis.snoozeloo.di.dbModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class SnoozelooApp: Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@SnoozelooApp)
            modules(
                appModule,
                dbModule,
                dataModule
            )
        }
    }

}