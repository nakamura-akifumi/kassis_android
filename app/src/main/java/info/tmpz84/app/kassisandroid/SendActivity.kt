package info.tmpz84.app.kassisandroid

import allbegray.slack.SlackClientFactory
import allbegray.slack.exception.SlackResponseErrorException
import allbegray.slack.rtm.Event
import allbegray.slack.rtm.SlackRealTimeMessagingClient
import allbegray.slack.type.Channel
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_send.*



class SendActivity : AppCompatActivity() {

    lateinit var realm: Realm

    override fun onResume() {
        super.onResume()

        realm = Realm.getDefaultInstance()
    }

    override fun onPause() {
        super.onPause()

        realm.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        btnSend.setOnClickListener {
            Log.i("WorkActivity", "btnConnect.setOnClickListener")

            if ("sdk".equals(Build.PRODUCT)) {
                // エミュレータの場合はIPv6を無効
                Log.i("WorkActivity", "IPv6 disable")
                java.lang.System.setProperty("java.net.preferIPv6Addresses", "false")
                java.lang.System.setProperty("java.net.preferIPv4Stack", "true")
            }

            // Bot User OAuth Acces Token
            val preferencesName = PreferenceManager.getDefaultSharedPreferencesName(this.applicationContext)
            val mSharedPref = getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            var slackToken = mSharedPref.getString("pref_slack_api_token", "")

            var webSocketUrl = ""

            val items = realm.where(ItemDB::class.java).findAll()
            items.forEach {
                Log.i("SendActivity", "item=$it.str_item_identifier")
                Log.i("SendActivity", "datetime=$it.str_datetime")

            }


            Thread({
                Log.i("WorkActivity", "Thread @1 token=$slackToken")

                Log.i("WorkActivity", "Thread @2 get realm instance")
                var realm2:Realm = Realm.getDefaultInstance()

                var mWebApiClient = SlackClientFactory.createWebApiClient(slackToken)
                try {
                    webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText()
                } catch (e: Exception) {
                    Log.e("WorkActivity", e.message)
                    realm2.close()
                    return@Thread
                }
                Log.i("SendActivity", "Thread @3")
                var mRtmClient = SlackRealTimeMessagingClient(webSocketUrl)
                var mBotId = ""

                mRtmClient.addListener(Event.HELLO) { message ->

                    Log.i("SendActivity", "HELLO.onMessage")

                    val authentication = mWebApiClient.auth()
                    mBotId = authentication.user_id

                    Log.i("SendActivity", "User id: $mBotId")
                    Log.i("SendActivity", "Team name: " + authentication.team)
                    Log.i("SendActivity", "User name: " + authentication.user)

                    val items = realm2.where(ItemDB::class.java).findAll()
                    items.forEach {
                        Log.i("SendActivity", "item=$it.str_item_identifier")
                        Log.i("SendActivity", "datetime=$it.str_datetime")

                        mWebApiClient.postMessage("databus_dev", "item=$it.str_item_identifier")
                    }

                    realm2.close()
                    Toast.makeText(this@SendActivity, "送信しました", Toast.LENGTH_SHORT).show()

                }

                mRtmClient.addListener(Event.MESSAGE) { message ->
                    val channelId = message.findPath("channel").asText()
                    val userId = message.findPath("user").asText()
                    val text = message.findPath("text").asText()

                    if (userId != null && userId != mBotId) {
                        var channel: Channel?
                        try {
                            channel = mWebApiClient.getChannelInfo(channelId)
                        } catch (e: SlackResponseErrorException) {
                            channel = null
                        }

                        val user = mWebApiClient.getUserInfo(userId)
                        val userName = user.name

                        Log.i("WorkActivity","Channel id: $channelId")
                        Log.i("WorkActivity","Channel name: " + if (channel != null) "#" + channel!!.getName() else "DM")
                        Log.i("WorkActivity","User id: $userId")
                        Log.i("WorkActivity","User name: $userName")
                        Log.i("WorkActivity","Text: $text")

                        mWebApiClient.meMessage(channelId, "$userName: $text")
                    }
                }

                mRtmClient.connect();

            }).start()
        }
    }
}
