package com.vineyard.omnicam.app.ui.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vineyard.omnicam.app.core.theme.CyanAccent

@Composable
fun ByoFirebaseDialog(
    onDismiss: () -> Unit,
    onSaveJson: (String) -> Unit
) {
    var jsonText by remember {
        mutableStateOf(
            """{
  "project_info": {
    "project_id": "my-private-omnicam",
    "storage_bucket": "my-private-omnicam.appspot.com"
  },
  "client": [
    {
      "client_info": { "mobilesdk_app_id": "1:1234567890:android:abcdef" },
      "api_key": [ { "current_key": "AIzaSyPrivateApiKey123456" } ]
    }
  ]
}"""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "BYO-Firebase Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste the contents of your custom private google-services.json to initialize your own personal serverless cloud database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("google-services.json") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("byo_firebase_input"),
                    textStyle = MaterialTheme.typography.bodySmall,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveJson(jsonText)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier.testTag("byo_firebase_save_btn")
            ) {
                Text("Initialize Cloud", color = androidx.compose.ui.graphics.Color.Black)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("byo_firebase_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
