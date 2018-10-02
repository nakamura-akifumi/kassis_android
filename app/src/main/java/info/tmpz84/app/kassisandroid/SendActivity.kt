package info.tmpz84.app.kassisandroid

import allbegray.slack.SlackClientFactory
import allbegray.slack.exception.SlackResponseErrorException
import allbegray.slack.rtm.Event
import allbegray.slack.rtm.SlackRealTimeMessagingClient
import allbegray.slack.type.Channel
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : AppCompatActivity() {

    val handler = Handler()

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        progressBar.visibility = android.widget.ProgressBar.INVISIBLE
        progressBar.max = 100

        var realm:Realm = Realm.getDefaultInstance()
        val items = realm.where(ItemDB::class.java).findAll()
        val item_size = items.size
        realm.close()
        statusView.setText("現在の保存件数:${item_size}")

        btnSend.setOnClickListener {
            Log.i("WorkActivity", "btnConnect.setOnClickListener")

            // Bot User OAuth Acces Token
            val preferencesName = PreferenceManager.getDefaultSharedPreferencesName(this.applicationContext)
            val mSharedPref = getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            val slackToken = mSharedPref.getString("pref_slack_api_token", "")
            val displayName = mSharedPref.getString("pref_display_name", "")
            val slackChannel = mSharedPref.getString("pref_slack_channel", "")
            var webSocketUrl = ""
            val sendItems = mutableListOf<ItemDB>()

            var realm:Realm = Realm.getDefaultInstance()
            val items = realm.where(ItemDB::class.java).findAll()
            val item_size = items.size
            items.forEach {
                // TODO: 送信スレッド側にコピーせずにしたい。
                val item = ItemDB()
                item.str_item_identifier = it.str_item_identifier.toString()
                item.str_datetime = it.str_datetime.toString()

                sendItems.add(item)
            }
            realm.close()
            Log.d("SendActivity", "prepare sendings.")

            Thread({
                Log.i("SendActivity", "Thread @1 token=${slackToken} channel=${slackChannel}")

                var mWebApiClient = SlackClientFactory.createWebApiClient(slackToken)
                try {
                    webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText()
                } catch (e: Exception) {
                    Log.e("SendActivity", e.message)
                    handler.post(Runnable {
                        statusView.setText("slack接続時にエラーが発生しました。 ${e.message}")
                        var ts:Toast = Toast.makeText(this@SendActivity, "接続時にエラーが発生しました。", Toast.LENGTH_SHORT)
                        ts.setGravity(Gravity.CENTER, 0, 0);
                        ts.show()
                    })
                    return@Thread
                }
                Log.i("SendActivity", "Thread @3")
                var mRtmClient = SlackRealTimeMessagingClient(webSocketUrl)
                var mBotId = ""
                var sendCount = 0

                handler.post(Runnable {
                    progressBar.visibility = android.widget.ProgressBar.VISIBLE
                })

                mRtmClient.addListener(Event.HELLO) { message ->

                    Log.i("SendActivity", "HELLO.onMessage")

                    val authentication = mWebApiClient.auth()
                    mBotId = authentication.user_id

                    mWebApiClient.postMessage(slackChannel, "!buscmd/sendtrans/begin/${displayName}")

                    sendItems.forEach {
                        Log.d("SendActivity", "item=$it.str_item_identifier")
                        Log.d("SendActivity", "datetime=$it.str_datetime")

                        val msg = "${displayName},${it.str_item_identifier.toString()},${it.str_datetime.toString()}"

                        mWebApiClient.postMessage(slackChannel, msg)
                        sendCount++

                        handler.post(Runnable {
                            progressBar.progress = (sendCount / item_size) * 100
                        })
                    }
                    mWebApiClient.postMessage(slackChannel, "!buscmd/sendtrans/end/${displayName}")

                    // Handlerを使用してメイン(UI)スレッドに処理を依頼する
                    handler.post(Runnable {
                        statusView.setText("送信しました。送信件数:${sendCount}")
                        var ts:Toast = Toast.makeText(this@SendActivity, "送信しました。", Toast.LENGTH_SHORT)
                        ts.setGravity(Gravity.CENTER, 0, 0);
                        ts.show()

                        //progressBar.visibility = android.widget.ProgressBar.INVISIBLE

                        postProcess(1, sendCount)
                    })

                    return@addListener
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

                        Log.d("WorkActivity","Channel id: $channelId")
                        Log.d("WorkActivity","Channel name: " + if (channel != null) "#" + channel!!.getName() else "DM")

                        mWebApiClient.meMessage(channelId, "$userName: $text")
                    }
                }

                mRtmClient.connect();

            }).start()
        }
    }

    fun postProcess(status: Int, count: Int, message: String = "") {
        Log.i("SendActivity", "start postProcess status=${status} cnt=${count}")
        var realm:Realm = Realm.getDefaultInstance()
        realm.beginTransaction();
        realm.delete(ItemDB::class.java)
        realm.commitTransaction();
        realm.close()
        Log.d("SendActivity", "complete postProcess")
    }

}
