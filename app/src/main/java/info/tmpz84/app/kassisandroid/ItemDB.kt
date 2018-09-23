package info.tmpz84.app.kassisandroid

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class ItemDB: RealmObject() {
    @PrimaryKey
    open var id : String = UUID.randomUUID().toString()
    var str_item_identifier: String = ""
    var str_datetime: String = ""
    var str_eventid: String = ""
    var str_machinename = ""
}