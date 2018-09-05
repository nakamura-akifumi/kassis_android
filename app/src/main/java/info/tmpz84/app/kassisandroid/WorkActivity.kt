package info.tmpz84.app.kassisandroid

import allbegray.slack.SlackClientFactory
import allbegray.slack.exception.SlackResponseErrorException
import allbegray.slack.rtm.Event
import allbegray.slack.rtm.SlackRealTimeMessagingClient
import allbegray.slack.type.Channel
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_work.*


class WorkActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        btnSendMessage.setOnClickListener {
            val mSharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val editor = mSharedPref.edit()
            editor.putString("slack_token", editSendMessage.text.toString())
            editor.apply()
        }

        btnConnect.setOnClickListener {
            Log.i("WorkActivity", "btnConnect.setOnClickListener")

            if ("sdk".equals(Build.PRODUCT)) {
                // エミュレータの場合はIPv6を無効
                Log.i("WorkActivity", "IPv6 disable")
                java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
                java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
            }

            val mSharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            // Bot User OAuth Access Token
            var slackToken = mSharedPref.getString("slack_token", "");
            Thread({
                Log.i("WorkActivity", "Thread @1 token=$slackToken")
                var mWebApiClient = SlackClientFactory.createWebApiClient(slackToken)
                var webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText()
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

                        println("Channel id: $channelId")
                        println("Channel name: " + if (channel != null) "#" + channel!!.getName() else "DM")
                        println("User id: $userId")
                        println("User name: $userName")
                        println("Text: $text")

                        // Copy cat
                        mWebApiClient.meMessage(channelId, "$userName: $text")
                    }
                }

                mRtmClient.connect();

            }).start()


        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)
    }
}
