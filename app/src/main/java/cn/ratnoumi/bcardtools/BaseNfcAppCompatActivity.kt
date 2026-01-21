package cn.ratnoumi.bcardtools

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

abstract class BaseNfcAppCompatActivity : AppCompatActivity() {
    lateinit var pendingIntent: PendingIntent
    var nfcAdapter: NfcAdapter? = null

    /**
     * onNewIntent 事件的转发
     *
     * @param intent
     */
    @Throws(Exception::class)
    abstract fun processTag(intent: Intent?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter != null && !nfcAdapter!!.isEnabled) {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS));
        }
        // 创建 PendingIntent
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }


    override fun onResume() {
        super.onResume()
        try {
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        try {
            processTag(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}