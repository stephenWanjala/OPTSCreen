package com.github.stephenwanjala.totp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Telephony
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER"
        const val SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"
    }

    var otpCode by mutableStateOf("")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SMS_DELIVER_ACTION || intent.action == SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.messageBody
                val otp = extractOtpFromMessage(body)
                if (otp.isNotEmpty()) {
                    otpCode = otp
                    break
                }
            }
        }
    }

    private fun extractOtpFromMessage(messageBody: String): String {
        // Perform the logic to extract OTP from the message body
        // Here's an example assuming OTP is a 6-digit number
        val otpRegex = Regex("\\b\\d{6}\\b")
        val matchResult = otpRegex.find(messageBody)
        return matchResult?.value ?: ""
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen() {
    val context = LocalContext.current
    val smsReceiver = remember { SmsReceiver() }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Register the BroadcastReceiver
    DisposableEffect(Unit) {
        val intentFilter = IntentFilter().apply {
            addAction(SmsReceiver.SMS_DELIVER_ACTION)
            addAction(SmsReceiver.SMS_RECEIVED_ACTION)
        }
        context.registerReceiver(smsReceiver, intentFilter)

        onDispose {
            context.unregisterReceiver(smsReceiver)
        }
    }
    val otpString = smsReceiver.otpCode

    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }

    // Split the OTP string into individual digits
    val otpDigitsList = otpString.map { it.toString() }

    // Populate the otpDigits list with the individual digits
    otpDigitsList.forEachIndexed { index, digit ->
        if (index < otpDigits.size) {
            otpDigits[index] = digit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (keyboardController != null) {
            OtpDigitsRow(
                otpDigits = otpDigits,
                focusRequesters = focusRequesters,
                keyboardController = keyboardController
            )
        }
        Button(
            onClick = { verifyOtp(context = context, otp = otpDigits.joinToString("")) }
        ) {
            Text(text = "Verify")
        }
    }

}

fun verifyOtp(context: Context, otp: String) {
    // Perform verification logic here
    if (otp.length == 6) {
        // Verification successful
        Toast.makeText(context, "OTP verified successfully", Toast.LENGTH_SHORT).show()
    } else {
        // Invalid OTP
        Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpDigitsRow(
    otpDigits: MutableList<String>,
    focusRequesters: List<FocusRequester>,
    keyboardController: SoftwareKeyboardController
) {
    Row(modifier = Modifier.wrapContentWidth()) {
        otpDigits.forEachIndexed { index, digit ->
            OtpTextField(
                value = digit,
                onValueChange = { newValue ->
                    if (newValue.length <= 1) {
                        otpDigits[index] = newValue
                        if (newValue.isNotEmpty() && index < otpDigits.lastIndex) {
                            focusRequesters[index + 1].requestFocus()
                        } else if (index == otpDigits.lastIndex) {
                            keyboardController.hide() // Dismiss the keyboard
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (index == otpDigits.lastIndex) ImeAction.Done else ImeAction.Next
                ),
                focusRequester = focusRequesters[index],
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        maxLines = 1,
        modifier = modifier
            .focusRequester(focusRequester)
    )
}