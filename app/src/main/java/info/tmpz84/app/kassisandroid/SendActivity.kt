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
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        btnSend.setOnClickListener {
            Log.i("WorkActivity", "btnConnect.setOnClickListener")

            if ("sdk".equals(Build.PRODUCT)) {
                // エミュレータの場合はIPv6を無効
                Log.i("WorkActivity", "IPv6 disable")
                java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
                java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
            }

            // Bot User OAuth Access Token
            // Bot User OAuth Acces Token
            val preferencesName = PreferenceManager.getDefaultSharedPreferencesName(this.applicationContext)
            val mSharedPref = getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            var slackToken = mSharedPref.getString("pref_slack_api_token", "");

            var webSocketUrl = ""

            Thread({
                Log.i("WorkActivity", "Thread @1 token=$slackToken")
                var mWebApiClient = SlackClientFactory.createWebApiClient(slackToken)
                try {
                    webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText()
                } catch (e: Exception) {
                    Log.e("WorkActivity", e.message)
                    return@Thread
                }
                Log.i("WorkActivity", "Thread @3")
                var mRtmClient = SlackRealTimeMessagingClient(webSocketUrl)
                var mBotId = ""

                mRtmClient.addListener(Event.HELLO) { message ->

                    Log.i("WorkActivity", "HELLO.onMessage")

                    val authentication = mWebApiClient.auth()
                    mBotId = authentication.user_id

                    Log.i("WorkActivity", "User id: $mBotId")
                    Log.i("WorkActivity", "Team name: " + authentication.team)
                    Log.i("WorkActivity", "User name: " + authentication.user)
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

                        // Copy catx
                        mWebApiClient.meMessage(channelId, "$userName: $text")
                    }
                }

                mRtmClient.connect();

            }).start()
        }
    }
}
