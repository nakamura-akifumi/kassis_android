package info.tmpz84.app.kassisandroid

import android.app.Application
import io.realm.Realm

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        //init realm
        Realm.init(this)
    }
}