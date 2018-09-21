package info.tmpz84.app.kassisandroid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


const val MY_REQUEST_CODE = 0


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectButton.isClickable = false

        settingButton.setOnClickListener {
            val intent: Intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, MY_REQUEST_CODE)
        }

        sendButton.setOnClickListener {
            val intent: Intent = Intent(this, SendActivity::class.java)
            startActivityForResult(intent, MY_REQUEST_CODE)
        }

        workButton.setOnClickListener {
            val intent: Intent = Intent(this, WorkActivity::class.java)
            startActivityForResult(intent, MY_REQUEST_CODE)
        }



    }

    override fun onResume() {
        super.onResume()

        val preferencesName = PreferenceManager.getDefaultSharedPreferencesName(this.applicationContext)
        val mSharedPref = getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        var slackToken = mSharedPref.getString("pref_slack_api_token", "")

        if (slackToken == "") {
            workButton.isClickable = false
            sendButton.isClickable = false
            Toast.makeText(this, "設定をしてください。", Toast.LENGTH_LONG).show();
        } else {
            workButton.isClickable = true
            sendButton.isClickable = true
        }
    }
}
