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

        btnSend.setOnClickListener {
            Log.i("WorkActivity", "btnConnect.setOnClickListener")

            // Bot User OAuth Acces Token
            val preferencesName = PreferenceManager.getDefaultSharedPreferencesName(this.applicationContext)
            val mSharedPref = getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            var slackToken = mSharedPref.getString("pref_slack_api_token", "")
            var displayName = mSharedPref.getString("pref_display_name", "")
            var webSocketUrl = ""
            val sendItems = mutableListOf<ItemDB>()

            var realm:Realm = Realm.getDefaultInstance()
            val items = realm.where(ItemDB::class.java).findAll()
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
                Log.i("WorkActivity", "Thread @1 token=$slackToken")

                var mWebApiClient = SlackClientFactory.createWebApiClient(slackToken)
                try {
                    webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText()
                } catch (e: Exception) {
                    Log.e("WorkActivity", e.message)
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

                mRtmClient.addListener(Event.HELLO) { message ->

                    Log.i("SendActivity", "HELLO.onMessage")

                    val authentication = mWebApiClient.auth()
                    mBotId = authentication.user_id

                    Log.i("SendActivity", "User id: $mBotId")
                    Log.i("SendActivity", "Team name: " + authentication.team)
                    Log.i("SendActivity", "User name: " + authentication.user)

                    mWebApiClient.postMessage("databus_dev", "!buscmd/sendtrans/begin/${displayName}")

                    sendItems.forEach {
                        Log.i("SendActivity", "item=$it.str_item_identifier")
                        Log.i("SendActivity", "datetime=$it.str_datetime")

                        val msg = "${displayName},${it.str_item_identifier.toString()},${it.str_datetime.toString()}"

                        mWebApiClient.postMessage("databus_dev", msg)
                        sendCount++
                    }
                    mWebApiClient.postMessage("databus_dev", "!buscmd/sendtrans/end/${displayName}")

                    // Handlerを使用してメイン(UI)スレッドに処理を依頼する
                    handler.post(Runnable {
                        statusView.setText("送信しました。送信件数=${sendCount}")
                        var ts:Toast = Toast.makeText(this@SendActivity, "送信しました。", Toast.LENGTH_SHORT)
                        ts.setGravity(Gravity.CENTER, 0, 0);
                        ts.show()

                        postProcess(1, sendCount)
                    })
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

    fun postProcess(status: Int, count: Int, message: String = "") {
        Log.i("SendActivity", "start postProcess")
        // TODO: 正常時はレコード削除
        var realm:Realm = Realm.getDefaultInstance()
        realm.delete(ItemDB::class.java)
        realm.close()
        Log.d("SendActivity", "complete postProcess")

    }

}
