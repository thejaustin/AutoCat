package app.lawnchair.bugreport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.ui.theme.AutoCatTheme
import com.patrykmichalik.opto.core.setBlocking

class BugReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val reportId = intent.getIntExtra(EXTRA_REPORT_ID, -1)
        val report = if (reportId != -1) {
            AutoCatBugReporter.INSTANCE.get(this).getReport(reportId)
        } else {
            null
        }

        if (report == null) {
            finish()
            return
        }

        setContent {
            AutoCatTheme {
                BugReportScreen(
                    report = report,
                    onDismiss = {
                        PreferenceManager2.getInstance(this).lastCrashId.setBlocking(-1)
                        finish()
                    },
                    onShare = {
                        startActivity(report.createShareIntent(this))
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_REPORT_ID = "report_id"

        fun show(context: Context, reportId: Int) {
            val intent = Intent(context, BugReportActivity::class.java).apply {
                putExtra(EXTRA_REPORT_ID, reportId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(
    report: BugReport,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Crash Detected") },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "AutoCat encountered a crash. Here are the details:",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = report.contents,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.size(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Dismiss")
            }
        }
    }
}
