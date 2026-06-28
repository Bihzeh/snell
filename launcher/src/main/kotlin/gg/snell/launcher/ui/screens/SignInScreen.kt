package gg.snell.launcher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.snell.launcher.ui.LauncherViewModel
import gg.snell.launcher.ui.components.ButtonVariant
import gg.snell.launcher.ui.components.Brand
import gg.snell.launcher.ui.components.SnellButton
import gg.snell.launcher.ui.components.Spinner
import gg.snell.launcher.ui.components.SymIcon
import gg.snell.launcher.ui.theme.Snell
import gg.snell.launcher.ui.theme.SnellFonts
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun SignInScreen(vm: LauncherViewModel) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.width(420.dp).padding(24.dp),
        ) {
            // Brand lockup
            Image(
                painter = painterResource(Brand.TILE),
                contentDescription = "Snell",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)),
            )
            Text(
                "SNELL", fontFamily = SnellFonts.Display, fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.onBackground,
            )

            val prompt = vm.prompt
            when {
                prompt != null -> AwaitingCode(vm, prompt.userCode, prompt.verificationUri)
                vm.signInError != null -> Errored(vm)
                vm.signInBusy -> {
                    Spinner(28)
                    Text("Confirming with Microsoft…", color = Snell.text2, style = MaterialTheme.typography.bodyMedium)
                }
                else -> Idle(vm)
            }
        }
    }
}

@Composable
private fun Idle(vm: LauncherViewModel) {
    Text("Sign in to play", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    SnellButton("Sign in with Microsoft", { vm.signIn() }, leadingIcon = "person", fillWidth = true)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SymIcon("lock", 14.dp, Snell.text3)
        Text("Official Microsoft login · we never see your password", color = Snell.text3, style = MaterialTheme.typography.labelMedium)
    }
    if (vm.devMode) {
        SnellButton("Continue offline (dev)", { vm.continueOffline() }, variant = ButtonVariant.Tertiary, fillWidth = true)
    }
}

@Composable
private fun AwaitingCode(vm: LauncherViewModel, userCode: String, verificationUri: String) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
    Text("Go to $verificationUri and enter this code", color = Snell.text2, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Snell.s2)
            .border(1.dp, Snell.border, RoundedCornerShape(12.dp)).padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(userCode, fontFamily = SnellFonts.Mono, fontSize = 28.sp, letterSpacing = 6.sp, color = MaterialTheme.colorScheme.onBackground)
    }
    AnimatedVisibility(visible = copied, enter = fadeIn(), exit = fadeOut()) {
        Row(
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.background(Snell.success.copy(alpha = 0.16f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            SymIcon("check", 16.dp, Snell.success)
            Text("Copied to clipboard", color = Snell.success, style = MaterialTheme.typography.labelMedium)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SnellButton("Open link", { open(verificationUri) }, Modifier.weight(1f), leadingIcon = "open_in_new", fillWidth = true)
        SnellButton(if (copied) "Copied!" else "Copy code", { copy(userCode); copied = true }, Modifier.weight(1f), variant = ButtonVariant.Secondary, leadingIcon = "content_copy", fillWidth = true)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Spinner(16)
        Text("Waiting for you to finish in your browser…", color = Snell.text3, style = MaterialTheme.typography.bodySmall)
    }
    SnellButton("Cancel", { vm.cancelSignIn() }, variant = ButtonVariant.Tertiary, fillWidth = true)
}

@Composable
private fun Errored(vm: LauncherViewModel) {
    Box(
        Modifier.size(60.dp).clip(RoundedCornerShape(30.dp)).background(Snell.danger.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) { SymIcon("error", 32.dp, Snell.danger) }
    Text("Couldn't sign in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    Text(vm.signInError ?: "", color = Snell.text2, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.width(2.dp))
    SnellButton("Try again", { vm.signIn() }, leadingIcon = "refresh", fillWidth = true)
    SnellButton("Cancel", { vm.cancelSignIn() }, variant = ButtonVariant.Tertiary, fillWidth = true)
}

private fun open(uri: String) = runCatching {
    val u = java.net.URI(uri)
    require(u.scheme?.lowercase() in setOf("https", "http"))
    java.awt.Desktop.getDesktop().browse(u)
}
private fun copy(text: String) = runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
