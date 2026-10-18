package com.novaris.transformationlab

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import org.json.JSONObject

private val scheme = lightColorScheme(
    primary = Teal, onPrimary = Color.White, secondary = DeepNavy,
    background = Canvas, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFF0F3F1), outline = Rule
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { MaterialTheme(colorScheme = scheme) { NovarisApp() } }
    }
}

@Composable
private fun NovarisApp() {
    val context = LocalContext.current
    val vault = remember { SessionVault(context) }
    val recentTests = remember { RecentTests(context) }
    val service = remember { AuthService() }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(vault.load()) }
    var screen by remember { mutableStateOf(if (account == null) "auth" else "home") }
    var tab by remember { mutableStateOf("explore") }
    var activeTest by remember { mutableStateOf<Assessment?>(null) }
    var recent by remember { mutableStateOf(recentTests.get()) }
    var accountNotice by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val saved = account ?: return@LaunchedEffect
        try { account = service.verify(saved).also { vault.save(it) } }
        catch (e: AuthFailure) {
            if (e.status == 401) { vault.clear(); account = null; accountNotice = "Your session expired. Please sign in again." }
        }
    }

    BackHandler(enabled = activeTest == null && screen != "home") { screen = "home"; tab = "explore" }

    if (activeTest != null) {
        AssessmentWebScreen(activeTest!!, account, onClose = { activeTest = null })
        return
    }

    if (screen == "auth") {
        AuthScreen(
            service = service, notice = accountNotice,
            onSuccess = { a -> vault.save(a); account = a; accountNotice = ""; screen = "home"; tab = "explore" },
            onGuest = { screen = "home"; tab = "explore" }
        )
        return
    }

    Scaffold(
        containerColor = Canvas,
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                listOf(Triple("explore", "Explore", Icons.Default.Home), Triple("journey", "My journey", Icons.Default.MenuBook), Triple("profile", "Profile", Icons.Default.Person)).forEach { (key, label, icon) ->
                    NavigationBarItem(
                        selected = tab == key, onClick = { tab = key },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp)) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Teal, selectedTextColor = Teal, indicatorColor = Mint, unselectedIconColor = Muted, unselectedTextColor = Muted)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                "explore" -> ExploreScreen(account, onProfile = { tab = "profile" }, onOpen = { assessment ->
                    recentTests.add(assessment.key); recent = recentTests.get(); activeTest = assessment
                })
                "journey" -> JourneyScreen(recent, onOpen = { assessment ->
                    recentTests.add(assessment.key); recent = recentTests.get(); activeTest = assessment
                })
                else -> ProfileScreen(account, onAuth = { screen = "auth" }, onSignOut = {
                    val old = account
                    account = null; vault.clear(); accountNotice = ""; screen = "auth"
                    if (old != null) scope.launch { try { service.signOut(old.token) } catch (_: Exception) { } }
                })
            }
        }
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier, inverse: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(R.drawable.novaris_mark), "Novaris logo", Modifier.size(43.dp))
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text("NOVARIS", fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 2.2.sp, color = if (inverse) Color.White else DeepNavy)
            Text("TRANSFORMATION LAB", fontWeight = FontWeight.SemiBold, fontSize = 8.sp, letterSpacing = 1.9.sp, color = if (inverse) Color.White.copy(alpha=.8f) else Muted)
        }
    }
}

@Composable
private fun AuthScreen(service: AuthService, notice: String, onSuccess: (Account) -> Unit, onGuest: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(notice) }
    var loading by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Canvas).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(262.dp).clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))) {
            Image(painterResource(R.drawable.photo_advisory), "Indian business owners and advisers", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepNavy.copy(alpha=.88f), DeepNavy.copy(alpha=.60f), DeepNavy.copy(alpha=.96f)))))
            Column(Modifier.fillMaxSize().padding(start=25.dp, top=28.dp, end=25.dp, bottom=24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Brand(inverse = true)
                Column {
                    Text("See the bigger picture.", fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(7.dp))
                    Text("Clarity for leaders. Momentum for organisations.", fontSize=13.sp, color=Color.White.copy(alpha=.87f))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal=25.dp, vertical=23.dp)) {
            Text(if (isRegister) "Create your account" else "Welcome to Novaris", fontSize=24.sp, fontWeight=FontWeight.Bold, color=DeepNavy)
            Spacer(Modifier.height(5.dp))
            Text(if (isRegister) "Join to access all eight complimentary assessments." else "Sign in to explore your leadership and business insights.", fontSize=13.sp, lineHeight=19.sp, color=Muted)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFFEDEFEA)).padding(4.dp)) {
                AuthTab("Sign in", !isRegister, Modifier.weight(1f)) { isRegister=false; error="" }
                AuthTab("Sign up", isRegister, Modifier.weight(1f)) { isRegister=true; error="" }
            }
            Spacer(Modifier.height(19.dp))
            if (isRegister) {
                TextFieldLabel("Full name")
                NovarisTextField(name, {name=it}, "Your name", KeyboardType.Text)
                Spacer(Modifier.height(12.dp))
            }
            TextFieldLabel("Email address")
            NovarisTextField(email, {email=it}, "you@company.com", KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            if (isRegister) {
                TextFieldLabel("Organisation (optional)")
                NovarisTextField(company, {company=it}, "Company name", KeyboardType.Text)
                Spacer(Modifier.height(12.dp))
            }
            TextFieldLabel("Password")
            OutlinedTextField(
                value=password, onValueChange={password=it}, singleLine=true,
                placeholder={Text("At least 8 characters", color=Muted, fontSize=13.sp)},
                visualTransformation=PasswordVisualTransformation(),
                modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp),
                colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Teal, unfocusedBorderColor=Rule, focusedContainerColor=Color.White, unfocusedContainerColor=Color.White)
            )
            if (error.isNotBlank()) {
                Spacer(Modifier.height(11.dp))
                Text(error, color=Color(0xFFB33335), fontSize=12.sp, lineHeight=17.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick={
                    when {
                        isRegister && name.trim().length < 2 -> error="Please enter your full name."
                        !email.contains('@') || !email.substringAfter('@').contains('.') -> error="Enter a valid email address."
                        password.length < 8 -> error="Use a password with at least 8 characters."
                        else -> {
                            loading=true; error=""
                            scope.launch {
                                try {
                                    val a = if (isRegister) service.register(name.trim(), email.trim(), password, company.trim()) else service.signIn(email.trim(), password)
                                    password=""; onSuccess(a)
                                } catch (e: Exception) { error=e.message ?: "Something went wrong." }
                                finally { loading=false }
                            }
                        }
                    }
                }, enabled=!loading, modifier=Modifier.fillMaxWidth().height(54.dp), shape=RoundedCornerShape(13.dp),
                colors=ButtonDefaults.buttonColors(containerColor=DeepNavy)
            ) { if (loading) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth=2.dp, color=Color.White) else {
                Text(if (isRegister) "Create account" else "Sign in", fontWeight=FontWeight.Bold)
                Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(17.dp))
            } }
            Spacer(Modifier.height(14.dp))
            TextButton(onClick=onGuest, modifier=Modifier.fillMaxWidth()) { Text("Explore as guest", color=Teal, fontWeight=FontWeight.SemiBold) }
            Spacer(Modifier.height(6.dp))
            Text("All assessments are free. No payments. Full reports go privately to Novaris; you see a short executive summary.", color=Muted, fontSize=11.sp, lineHeight=16.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun AuthTab(title: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(if(selected) Color.White else Color.Transparent).clickable(onClick=onClick).padding(vertical=11.dp), contentAlignment=Alignment.Center) {
        Text(title, fontWeight=if(selected) FontWeight.Bold else FontWeight.Medium, fontSize=13.sp, color=if(selected) DeepNavy else Muted)
    }
}
@Composable private fun TextFieldLabel(label: String) { Text(label, color=Ink, fontSize=12.sp, fontWeight=FontWeight.SemiBold, modifier=Modifier.padding(bottom=6.dp)) }
@Composable private fun NovarisTextField(value: String, setValue: (String)->Unit, hint: String, keyboard: KeyboardType) {
    OutlinedTextField(
        value=value, onValueChange=setValue, singleLine=true,
        placeholder={Text(hint, fontSize=13.sp, color=Muted)},
        keyboardOptions=KeyboardOptions(keyboardType=keyboard),
        modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp),
        colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Teal, unfocusedBorderColor=Rule, focusedContainerColor=Color.White, unfocusedContainerColor=Color.White)
    )
}

@Composable
private fun ExploreScreen(account: Account?, onProfile: () -> Unit, onOpen: (Assessment) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filtered = ASSESSMENTS.filter { test ->
        (filter == "All" || test.group == filter) && (query.isBlank() || listOf(test.title, test.subtitle, test.description).any { it.contains(query, ignoreCase=true) })
    }
    LazyColumn(modifier=Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=30.dp), verticalArrangement=Arrangement.spacedBy(0.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start=20.dp, end=20.dp, top=14.dp, bottom=18.dp), verticalAlignment=Alignment.CenterVertically) {
                Brand(Modifier.weight(1f))
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White).border(1.dp, Rule, CircleShape).clickable(onClick=onProfile), contentAlignment=Alignment.Center) {
                    if (account == null) Icon(Icons.Default.Person, null, tint=DeepNavy) else Text(account.name.take(1).uppercase(), color=DeepNavy, fontWeight=FontWeight.Bold)
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal=18.dp).height(216.dp).clip(RoundedCornerShape(24.dp))) {
                Image(painterResource(R.drawable.photo_factory), "Indian business owners discussing operations", Modifier.fillMaxSize(), contentScale=ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(DeepNavy.copy(alpha=.99f), DeepNavy.copy(alpha=.93f), DeepNavy.copy(alpha=.25f)))))
                Column(Modifier.fillMaxHeight().fillMaxWidth(.76f).padding(20.dp), verticalArrangement=Arrangement.SpaceBetween) {
                    Column {
                        Text("THE NOVARIS EXPERIENCE", color=Color(0xFF9AE8D5), letterSpacing=1.5.sp, fontSize=9.sp, fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(11.dp))
                        Text("Better insight.\nStronger decisions.", fontSize=25.sp, lineHeight=30.sp, fontWeight=FontWeight.Bold, color=Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Understand your people, teams and business with practical diagnostics.", fontSize=11.sp, lineHeight=16.sp, color=Color.White.copy(alpha=.9f))
                    }
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha=.17f)).padding(horizontal=11.dp, vertical=7.dp)) {
                        Text("8 complimentary assessments", color=Color.White, fontSize=10.sp, fontWeight=FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal=21.dp, vertical=21.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                Metric("08", "Diagnostics")
                Box(Modifier.height(38.dp).width(1.dp).background(Rule))
                Metric("FREE", "No checkout")
                Box(Modifier.height(38.dp).width(1.dp).background(Rule))
                Metric("100%", "Action-focused")
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal=20.dp)) {
                Text("Discover your next insight", fontWeight=FontWeight.Bold, fontSize=22.sp, color=DeepNavy)
                Spacer(Modifier.height(5.dp))
                Text("Choose a diagnostic. Your results will guide the conversation.", color=Muted, fontSize=12.sp)
                Spacer(Modifier.height(15.dp))
                OutlinedTextField(
                    value=query, onValueChange={query=it}, singleLine=true,
                    leadingIcon={ Icon(Icons.Default.Search, contentDescription=null, tint=Muted) },
                    placeholder={Text("Search assessments", color=Muted, fontSize=13.sp)},
                    modifier=Modifier.fillMaxWidth().height(54.dp), shape=RoundedCornerShape(15.dp),
                    colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Teal, unfocusedBorderColor=Rule, focusedContainerColor=Color.White, unfocusedContainerColor=Color.White)
                )
            }
        }
        item {
            androidx.compose.foundation.lazy.LazyRow(contentPadding=PaddingValues(horizontal=20.dp, vertical=16.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Organisation", "People", "Teams", "Personal")) { item ->
                    val selected = filter == item
                    Box(Modifier.clip(RoundedCornerShape(50.dp)).background(if(selected) DeepNavy else Color.White).border(if(selected) 0.dp else 1.dp, Rule, RoundedCornerShape(50.dp)).clickable{filter=item}.padding(horizontal=14.dp, vertical=9.dp)) {
                        Text(item, color=if(selected) Color.White else Ink, fontSize=12.sp, fontWeight=if(selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=4.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                Text("Explore assessments", fontSize=15.sp, fontWeight=FontWeight.Bold, color=DeepNavy)
                Text("${filtered.size} available", fontSize=11.sp, color=Muted)
            }
            Spacer(Modifier.height(9.dp))
        }
        if (filtered.isEmpty()) item { Text("No assessments match that search.", Modifier.padding(26.dp), color=Muted) }
        items(filtered, key={it.key}) { assessment ->
            Box(Modifier.padding(horizontal=18.dp, vertical=6.dp)) { AssessmentCard(assessment, onClick={onOpen(assessment)}) }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(start=22.dp, end=22.dp, top=21.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, Modifier.size(18.dp), tint=Teal)
                    Spacer(Modifier.width(8.dp))
                    Text("Your assessment, handled with care", color=DeepNavy, fontWeight=FontWeight.SemiBold, fontSize=13.sp)
                }
                Spacer(Modifier.height(7.dp))
                Text("Your screen shows only an executive summary. Complete original PDF reports are delivered privately to Novaris. Team Latitude is anonymous by design.", fontSize=11.sp, lineHeight=17.sp, color=Muted)
            }
        }
    }
}

@Composable private fun Metric(value: String, caption: String) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value, fontWeight=FontWeight.Black, fontSize=16.sp, color=DeepNavy)
        Text(caption, color=Muted, fontSize=10.sp)
    }
}

@Composable private fun AssessmentCard(test: Assessment, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Color.White).clickable(onClick=onClick).padding(15.dp), verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).height(92.dp).clip(RoundedCornerShape(9.dp)).background(test.accent))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(test.group.uppercase(), color=test.accent, fontSize=9.sp, fontWeight=FontWeight.ExtraBold, letterSpacing=.9.sp)
                if (test.anonymous) Text("ANONYMOUS", fontSize=8.sp, color=Teal, fontWeight=FontWeight.Bold, modifier=Modifier.background(Mint, RoundedCornerShape(4.dp)).padding(horizontal=5.dp, vertical=2.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(test.title, fontWeight=FontWeight.Bold, fontSize=16.sp, maxLines=1, overflow=TextOverflow.Ellipsis, color=DeepNavy)
            Spacer(Modifier.height(3.dp))
            Text(test.subtitle, fontSize=11.sp, fontWeight=FontWeight.Medium, color=Muted)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.Timelapse, null, Modifier.size(12.dp), tint=Muted)
                Text(test.minutes, fontSize=10.sp, color=Muted)
                Text(" · ", fontSize=10.sp, color=Muted)
                Text("Free", fontSize=10.sp, color=Teal, fontWeight=FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.width(7.dp))
        Box(Modifier.size(35.dp).clip(CircleShape).background(Mint), contentAlignment=Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(17.dp), tint=Teal)
        }
    }
}

@Composable
private fun JourneyScreen(recent: List<String>, onOpen: (Assessment) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(horizontal=20.dp, vertical=24.dp)) {
        item {
            Text("Your journey", fontSize=27.sp, fontWeight=FontWeight.Bold, color=DeepNavy)
            Spacer(Modifier.height(8.dp))
            Text("Pick up where you last explored. Your responses and reports aren't stored in this app.", color=Muted, fontSize=13.sp, lineHeight=20.sp)
            Spacer(Modifier.height(23.dp))
            Text("Recently opened", color=DeepNavy, fontSize=16.sp, fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }
        if (recent.isEmpty()) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(25.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.MenuBook, null, Modifier.size(43.dp), tint=Teal)
                Spacer(Modifier.height(10.dp))
                Text("Your discovery starts here", color=DeepNavy, fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Once you open an assessment, it'll appear here for quick access.", color=Muted, textAlign=TextAlign.Center, fontSize=12.sp)
            }
        } else items(recent) { key ->
            val test = ASSESSMENTS.firstOrNull { it.key == key }
            if (test != null) {
                Box(Modifier.padding(bottom=11.dp)) { AssessmentCard(test) { onOpen(test) } }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Text("About your results", fontSize=15.sp, fontWeight=FontWeight.Bold, color=DeepNavy)
            Spacer(Modifier.height(8.dp))
            Text("The Novaris website handles assessment sessions, full PDF generation and private email delivery. This screen shows recently opened tests, not completion claims or private test scores.", fontSize=12.sp, lineHeight=19.sp, color=Muted)
        }
    }
}

@Composable
private fun ProfileScreen(account: Account?, onAuth: ()->Unit, onSignOut: ()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(23.dp)) {
        Text("Your profile", fontSize=27.sp, fontWeight=FontWeight.Bold, color=DeepNavy)
        Spacer(Modifier.height(21.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DeepNavy).padding(22.dp)) {
            Brand(inverse=true)
            Spacer(Modifier.height(28.dp))
            Text(if (account != null) "Welcome, ${account.name}" else "Explore with Novaris", fontWeight=FontWeight.Bold, color=Color.White, fontSize=23.sp)
            Spacer(Modifier.height(5.dp))
            Text(if (account != null) account.email else "You're browsing as a guest.", color=Color.White.copy(alpha=.77f), fontSize=12.sp)
            if (account?.company?.isNotBlank()==true) {
                Spacer(Modifier.height(5.dp)); Text(account.company, color=Color.White.copy(alpha=.8f), fontSize=12.sp)
            }
        }
        Spacer(Modifier.height(23.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).padding(17.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, null, tint=Teal)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Private original reports", fontWeight=FontWeight.Bold, color=DeepNavy, fontSize=13.sp)
                Text("Sent to lead@novaristransformationlab.com", color=Muted, fontSize=11.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color.White).padding(17.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, tint=Teal)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Eight assessments. Always free.", fontWeight=FontWeight.Bold, color=DeepNavy, fontSize=13.sp)
                Text("No checkout, subscriptions or payment information.", color=Muted, fontSize=11.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick=if (account == null) onAuth else onSignOut, modifier=Modifier.fillMaxWidth().height(52.dp), shape=RoundedCornerShape(13.dp), colors=ButtonDefaults.buttonColors(containerColor=DeepNavy)) {
            Text(if (account == null) "Sign in or create account" else "Sign out", fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("Novaris Transformation Lab · Android edition 1.0.0", color=Muted, fontSize=11.sp, textAlign=TextAlign.Center, modifier=Modifier.fillMaxWidth())
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AssessmentWebScreen(test: Assessment, account: Account?, onClose: () -> Unit) {
    val context = LocalContext.current
    var webView by remember(test.key) { mutableStateOf<WebView?>(null) }
    var progress by remember(test.key) { mutableIntStateOf(0) }
    var failed by remember(test.key) { mutableStateOf(false) }
    var errorText by remember(test.key) { mutableStateOf("") }
    BackHandler {
        val web = webView
        if (web != null && web.canGoBack()) web.goBack() else onClose()
    }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(61.dp).padding(horizontal=14.dp), verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={ val web=webView; if(web?.canGoBack()==true) web.goBack() else onClose() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint=DeepNavy)
            }
            Column(Modifier.weight(1f)) {
                Text(test.title, fontSize=15.sp, fontWeight=FontWeight.Bold, color=DeepNavy, maxLines=1)
                Text(if (test.anonymous) "Anonymous diagnostic" else "Secure assessment", fontSize=10.sp, color=Muted)
            }
            IconButton(onClick={ failed=false; webView?.reload() }) { Icon(Icons.Default.Refresh, "Refresh", tint=Teal) }
        }
        if (progress in 0..99) LinearProgressIndicator(progress={ progress/100f }, modifier=Modifier.fillMaxWidth().height(3.dp), color=Teal, trackColor=Mint)
        Box(Modifier.weight(1f)) {
            key(test.key) {
                AndroidView(
                    factory={ ctx ->
                        WebView(ctx).apply {
                            webView=this
                            setBackgroundColor(android.graphics.Color.WHITE)
                            settings.javaScriptEnabled = true // Original authored assessments need JS for scoring / original PDF generation.
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            webChromeClient = object: WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) { progress=newProgress }
                            }
                            webViewClient = object: WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url=request?.url ?: return false
                                    val host=url.host?.lowercase() ?: ""
                                    if (url.scheme=="https" && (host=="novaristransformationlab.com" || host=="www.novaristransformationlab.com")) return false
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (_: Exception) { }
                                    return true
                                }
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { failed=false; progress=8 }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!test.anonymous && account != null && url?.contains("start-test.php") == true) {
                                        // Prefill only visible registration fields. Participant may edit before starting.
                                        val js="(function(){var f=document.querySelector('form');if(!f)return;"+
                                            "var entries={name:${JSONObject.quote(account.name)},email:${JSONObject.quote(account.email)},company:${JSONObject.quote(account.company)}};"+
                                            "Object.keys(entries).forEach(function(k){var x=f.querySelector('[name=\\\"'+k+'\\\"]');if(x&&!x.value){x.value=entries[k];x.dispatchEvent(new Event('input',{bubbles:true}));}});})();"
                                        view?.evaluateJavascript(js,null)
                                    }
                                }
                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    if (request?.isForMainFrame==true) { failed=true; errorText=error?.description?.toString() ?: "Page failed to load." }
                                }
                            }
                            loadUrl(BuildConfig.BASE_URL + "start-test.php?test=" + Uri.encode(test.key))
                        }
                    },
                    modifier=Modifier.fillMaxSize(),
                    onRelease={ web ->
                        web.stopLoading(); web.webChromeClient=null; web.webViewClient=WebViewClient()
                        web.destroy(); if (webView===web) webView=null
                    }
                )
            }
            if (failed) {
                Column(Modifier.fillMaxSize().background(Color.White).padding(32.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
                    Icon(Icons.Default.Info, null, tint=Teal, modifier=Modifier.size(46.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Unable to open assessment", fontWeight=FontWeight.Bold, color=DeepNavy, fontSize=19.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("$errorText\n\nCheck your connection and make sure the Novaris website is deployed.", color=Muted, fontSize=12.sp, lineHeight=19.sp, textAlign=TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    Button(onClick={failed=false; webView?.reload()}, colors=ButtonDefaults.buttonColors(containerColor=Teal)) { Text("Try again") }
                }
            }
        }
    }
}
