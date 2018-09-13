package info.tmpz84.app.kassisandroid

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import kotlinx.android.synthetic.main.activity_work.*


class WorkActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)

        item_identifier.setOnKeyListener { v, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) {
                item_list.append(item_identifier.text.toString())
                item_list.append("\n")
                item_identifier.text.clear()
                true
            } else {
                false
            }
        }


        btnAppend.setOnClickListener {
            Log.i("WorkActivity", "btnAppend.setOnClickListener")

            item_list.append(item_identifier.text.toString())
            item_list.append("\n")
            item_identifier.text.clear()
        }
    }
}
