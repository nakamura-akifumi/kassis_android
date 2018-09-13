package info.tmpz84.app.kassisandroid

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button


const val MY_REQUEST_CODE = 0

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val settingButton: Button = findViewById(R.id.settingButton) as Button
        settingButton.setOnClickListener {
            val intent: Intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, MY_REQUEST_CODE)
        }
        val sendButton: Button = findViewById(R.id.sendButton) as Button
        sendButton.setOnClickListener {
            val intent: Intent = Intent(this, SendActivity::class.java)
            startActivityForResult(intent, MY_REQUEST_CODE)
        }
        val workButton: Button = findViewById(R.id.workButton) as Button
        workButton.setOnClickListener {

            val intent: Intent = Intent(this, WorkActivity::class.java)
            intent.putExtra("number", 120)
            intent.putExtra("string", "The message from MainActivity")

            startActivityForResult(intent, MY_REQUEST_CODE)
        }

        val selectButton: Button = findViewById(R.id.selectButton) as Button
        selectButton.setOnClickListener {
        }


    }
}
