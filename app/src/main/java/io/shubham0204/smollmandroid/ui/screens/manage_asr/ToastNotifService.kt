package io.shubham0204.smollmandroid.ui.screens.manage_asr

import android.content.Context
import android.widget.Toast
import org.koin.core.annotation.Single

@Single
class ToastNotifService(
    private val context: Context
) {
    fun showShortToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showLongToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
