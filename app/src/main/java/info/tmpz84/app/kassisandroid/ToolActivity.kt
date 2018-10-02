package info.tmpz84.app.kassisandroid

import allbegray.slack.SlackClientFactory
import allbegray.slack.rtm.Event
import allbegray.slack.rtm.SlackRealTimeMessagingClient
import allbegray.slack.type.Channel
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_send.*
import kotlinx.android.synthetic.main.activity_tool.*
import java.util.*

class ToolActivity : AppCompatActivity() {

    val handler = Handler()

    val ItemSearch:Int = 0
    val PersonSearch:Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool)

        btnCreateTestData.setOnClickListener {

            val quantity = 2000

            var dialog = MessageDialogFragment()
            dialog.title = "アイテムツール"
            dialog.message = "${quantity}件追加しますか？"
            dialog.okText = "はい"
            dialog.cancelText = "いいえ"
            dialog.onOkClickListener = DialogInterface.OnClickListener { dialog, id ->
                Log.d("ToolActivity", "ok clicked")

                var sItemIdentifier: String = ""
                var count: Int = 1
                val realm = Realm.getDefaultInstance()
                for (i in 1..quantity) {
                    realm.executeTransaction {
                        val itemDB = realm.createObject(ItemDB::class.java, UUID.randomUUID().toString())

                        sItemIdentifier = "DUMMY-" + i.toString()
                        itemDB.str_item_identifier = sItemIdentifier
                        val date: Date = Date()
                        itemDB.str_datetime = DateFormat.format("yyyyMMddkkmmss", date).toString()
                        itemDB.str_eventid = "1"
                        itemDB.str_machinename = "1"

                        realm.copyToRealm(itemDB)
                    }
                }
                realm.close()

                var ts: Toast = Toast.makeText(this@ToolActivity, "作成しました。", Toast.LENGTH_SHORT)
                ts.setGravity(Gravity.CENTER, 0, 0);
                ts.show()
            }

        }

        btnRemoveAll.setOnClickListener {

            var dialog = MessageDialogFragment()
            dialog.title = "アイテムツール"
            dialog.message = "全件削除しますか？"
            dialog.okText = "はい"
            dialog.cancelText = "いいえ"
            dialog.onOkClickListener = DialogInterface.OnClickListener { dialog, id ->
                Log.d("ToolActivity", "ok clicked")

                var realm:Realm = Realm.getDefaultInstance()
                realm.beginTransaction();
                realm.delete(ItemDB::class.java)
                realm.commitTransaction();
                realm.close()
            }
            dialog.onCancelClickListener = DialogInterface.OnClickListener { dialog, id ->
                Log.d("ToolActivity", "cancel clicked")
            }
            dialog.show(supportFragmentManager, "tag")

        }

        btnItemSearch.setOnClickListener {
            network_adapter(ItemSearch)

        }

        btnPersonSearch.setOnClickListener {
            network_adapter(PersonSearch)

        }
    }

    fun network_adapter(action: Int) {
        val preferencesName = PreferenceManager.getDefaultSharedPreferencesName(this.applicationContext)
        val mSharedPref = getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val slackToken = mSharedPref.getString("pref_slack_api_token", "")
        val displayName = mSharedPref.getString("pref_display_name", "")
        val slackChannel = mSharedPref.getString("pref_slack_channel", "")
        var webSocketUrl = ""

        Log.i("ToolActivity", "btnItemSearch @1 token=${slackToken} channel=${slackChannel}")
        Thread({

            var mWebApiClient = SlackClientFactory.createWebApiClient(slackToken)
            try {
                webSocketUrl = mWebApiClient.startRealTimeMessagingApi().findPath("url").asText()
            } catch (e: Exception) {
                Log.e("SendActivity", e.message)
                handler.post(Runnable {
                    statusView.setText("slack接続時にエラーが発生しました。 ${e.message}")
                    var ts: Toast = Toast.makeText(this@ToolActivity, "接続時にエラーが発生しました。", Toast.LENGTH_SHORT)
                    ts.setGravity(Gravity.CENTER, 0, 0);
                    ts.show()
                })
                return@Thread
            }
            Log.i("SendActivity", "Thread @3")
            var mRtmClient = SlackRealTimeMessagingClient(webSocketUrl)
            var mBotId = ""
            var actionname:String = ""

            if (action == ItemSearch) {
                actionname = "is"
            }
            if (action == PersonSearch) {
                actionname = "ps"
            }

            mRtmClient.addListener(Event.HELLO) { message ->

                Log.i("ToolActivity", "HELLO.onMessage")

                val authentication = mWebApiClient.auth()
                mBotId = authentication.user_id

                var msg = "!buscmd/${actionname}/begin/${displayName}\n"
                msg = msg + "${txtQ.text.toString()}\n"
                msg = msg + "!buscmd/${actionname}/end/${displayName}"

                mWebApiClient.postMessage(slackChannel, msg)

                return@addListener
            }

            mRtmClient.addListener(Event.MESSAGE) { message ->
                val channelId = message.findPath("channel").asText()
                val userId = message.findPath("user").asText()
                val text = message.findPath("text").asText()

                Log.i("ToolActivity","MESSAGE Channel id: $channelId")

                if (userId != null && userId != mBotId) {
                    var channel: Channel?
                    channel = mWebApiClient.getChannelInfo(channelId)

                    if (channel!!.getName() == slackChannel) {

                        Log.i("ToolActivity", "Channel id: $channelId")
                        Log.i("ToolActivity", "Channel name: ${channel!!.getName()}")
                        Log.i("ToolActivity", text)

                        //mWebApiClient.meMessage(channelId, "$userName: $text")
                    }
                }
            }

            mRtmClient.connect();

        }).start()

    }

}
