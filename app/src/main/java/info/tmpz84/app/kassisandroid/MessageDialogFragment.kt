package info.tmpz84.app.kassisandroid

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment

class MessageDialogFragment : DialogFragment() {

    var title = "title"
    var message = "DialogMessage"
    var okText = "ok"
    var cancelText = "cancel"

    /** ok押下時の挙動 */
    var onOkClickListener : DialogInterface.OnClickListener? = null
    /** cancel押下時の挙動 デフォルトでは何もしない */
    var onCancelClickListener : DialogInterface.OnClickListener? = DialogInterface.OnClickListener { _, _ -> }

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(okText, onOkClickListener)
                .setNegativeButton(cancelText, onCancelClickListener)
        return builder.create()
    }

    override fun onPause() {
        super.onPause()
        // onPause でダイアログを閉じる場合
        dismiss()
    }
}