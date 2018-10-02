package info.tmpz84.app.kassisandroid

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_work.*
import java.util.*


class WorkActivity : AppCompatActivity() {

    lateinit var realm: Realm


    override fun onResume() {
        super.onResume()

        realm = Realm.getDefaultInstance()
    }

    override fun onPause() {
        super.onPause()
        Log.i("WorkActivity","WorkActivity onPause")
        realm.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)

        realm = Realm.getDefaultInstance()

        item_identifier.setOnKeyListener { v, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) {
                addItemIdentifier()

                true
            } else {
                false
            }
        }

        btnAppend.setOnClickListener {
            Log.i("WorkActivity", "btnAppend.setOnClickListener")

            addItemIdentifier()
        }

        val items = realm.where(ItemDB::class.java).findAll()
        items.forEach {
            Log.d("SendActivity", "item=$it.str_item_identifier")
            Log.d("SendActivity", "datetime=$it.str_datetime")

            item_list.append(it.str_item_identifier)
            item_list.append("\n")
        }
    }

    private fun addItemIdentifier() {
        var sItemIdentifier = item_identifier.text.toString()
        realm.executeTransaction {
            val itemDB = realm.createObject(ItemDB::class.java, UUID.randomUUID().toString())

            itemDB.str_item_identifier = sItemIdentifier
            val date: Date = Date()
            itemDB.str_datetime = DateFormat.format("yyyyMMddkkmmss", date).toString()
            itemDB.str_eventid = "1"
            itemDB.str_machinename = "1"

            realm.copyToRealm(itemDB)
        }

        item_list.append(sItemIdentifier)
        item_list.append("\n")
        item_identifier.setText("")

        Toast.makeText(this@WorkActivity, "登録しました", Toast.LENGTH_SHORT).show()
    }

}
