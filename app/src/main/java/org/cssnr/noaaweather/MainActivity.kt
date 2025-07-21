package org.cssnr.noaaweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.databinding.ActivityMainBinding
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.widget.WidgetProvider
import org.cssnr.noaaweather.work.APP_WORKER_CONSTRAINTS
import org.cssnr.noaaweather.work.AppWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        Log.d("SharedPreferences", "OnSharedPreferenceChangeListener: $key")
        //if (key == "enable_debug_logs") {
        //    val value = prefs.getBoolean(key, false)
        //    Log.i("SharedPreferences", "isLoggingEnabled: $value")
        //    fileLoggingTree.isLoggingEnabled = value
        //}
    }

    companion object {
        const val LOG_TAG = "NOAAWeather"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "onCreate: savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHostFragment
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        //// Start Destination
        //if (savedInstanceState == null) {
        //    val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        //    //val startPreference = preferences.getString("start_destination", null)
        //    //Log.d("Main[onCreate]", "startPreference: $startPreference")
        //    val startDestination = R.id.nav_home
        //    navGraph.setStartDestination(startDestination)
        //    navController.graph = navGraph
        //}

        // Bottom Navigation
        val bottomNav = binding.appBarMain.contentMain.bottomNav
        bottomNav.setupWithNavController(navController)

        // Navigation Drawer
        binding.navView.setupWithNavController(navController)

        // App Bar Configuration
        setSupportActionBar(binding.appBarMain.contentMain.toolbar)
        val topLevelItems =
            setOf(R.id.nav_item_home, R.id.nav_item_stations, R.id.nav_item_settings)
        appBarConfiguration = AppBarConfiguration(topLevelItems, binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Destinations w/ a Parent Item
        val destinationToBottomNavItem = mapOf(
            R.id.nav_item_settings_widget to R.id.nav_item_settings,
            R.id.nav_item_settings_debug to R.id.nav_item_settings,
        )
        // Destination w/ No Parent
        val hiddenDestinations = setOf<Int>(
            //R.id.nav_item_setup,
        )
        // Implement Navigation Hacks Because.......Android?
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("addOnDestinationChangedListener", "destination: ${destination.label}")
            binding.drawerLayout.closeDrawer(GravityCompat.START)

            val destinationId = destination.id

            if (destinationId in hiddenDestinations) {
                Log.d("addOnDestinationChangedListener", "Set bottomNav to Hidden Item")
                bottomNav.menu.findItem(R.id.nav_hidden).isChecked = true
                return@addOnDestinationChangedListener
            }

            val matchedItem = destinationToBottomNavItem[destinationId]
            if (matchedItem != null) {
                Log.d("addOnDestinationChangedListener", "matched nav item: $matchedItem")
                bottomNav.menu.findItem(matchedItem).isChecked = true
                val menu = binding.navView.menu
                for (i in 0 until menu.size) {
                    val item = menu[i]
                    item.isChecked = item.itemId == matchedItem
                }
            }
        }

        //// Handle Custom Navigation Items
        //val navLinks = mapOf(
        //    R.id.nav_item_tiktok to getString(R.string.tiktok_url),
        //    R.id.nav_itewm_youtube to getString(R.string.youtube_url),
        //    R.id.nav_item_website to getString(R.string.website_url),
        //)
        //binding.navView.setNavigationItemSelectedListener { menuItem ->
        //    binding.drawerLayout.closeDrawers()
        //    val path = navLinks[menuItem.itemId]
        //    if (path != null) {
        //        Log.d("Drawer", "path: $path")
        //        val intent = Intent(Intent.ACTION_VIEW, path.toUri())
        //        startActivity(intent)
        //        true
        //    } else {
        //        val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
        //        Log.d("Drawer", "handled: $handled")
        //        handled
        //    }
        //}

        // Set Debug Preferences
        Log.d(LOG_TAG, "Set Debug Preferences")
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "DEBUG BUILD DETECTED!")
            if (!preferences.contains("enable_debug_logs")) {
                Log.i(LOG_TAG, "ENABLING DEBUG LOGGING...")
                preferences.edit {
                    putBoolean("enable_debug_logs", true)
                }
            }
        }

        // Set Default Preferences
        Log.d(LOG_TAG, "Set Default Preferences")
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_widget, false)

        // Initialize Shared Preferences Listener
        Log.d(LOG_TAG, "Initialize Shared Preferences Listener")
        preferences.registerOnSharedPreferenceChangeListener(listener)

        // Update UI
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            false
        //drawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT)

        // NOTE: Testing hard coded bottom nav color and navigation bar color...
        window.navigationBarColor = ContextCompat.getColor(this, R.color.bottom_nav_color)

        val headerView = binding.navView.getHeaderView(0)
        ViewCompat.setOnApplyWindowInsetsListener(headerView) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(headerView)

        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d(LOG_TAG, "formattedVersion: $formattedVersion")
        versionTextView.text = formattedVersion

        // Setup Work Manager
        Log.d(LOG_TAG, "Setup Work Manager")
        val workInterval = preferences.getString("work_interval", null) ?: "60"
        Log.d(LOG_TAG, "workInterval: $workInterval")
        if (workInterval != "0") {
            val workRequest =
                PeriodicWorkRequestBuilder<AppWorker>(workInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(APP_WORKER_CONSTRAINTS)
                    .build()
            Log.d(LOG_TAG, "workRequest: $workRequest")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "app_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        // Setup Notifications
        val channelId = "default_channel_id"
        val name = "Default Channel"
        val descriptionText = "General Notifications Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(channelId, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

        // Handle Intent (this is the only thing handled for now)
        //if (!preferences.contains("first_run_shown")) {
        //    Log.i(LOG_TAG, "FIRST RUN DETECTED")
        //    preferences.edit { putBoolean("first_run_shown", true) }
        //    val bundle = bundleOf("add_station" to true)
        //    navController.navigate(
        //        R.id.nav_item_stations, bundle, NavOptions.Builder()
        //            .build()
        //    )
        //}
        Log.d(LOG_TAG, "MainActivity: savedInstanceState: $savedInstanceState")
        if (savedInstanceState == null) {
            Log.d(LOG_TAG, "MainActivity: lifecycleScope.launch")
            lifecycleScope.launch {
                val dao = StationDatabase.Companion.getInstance(applicationContext).stationDao()
                val station = withContext(Dispatchers.IO) { dao.getActive() }
                Log.d(LOG_TAG, "MainActivity: station: $station")
                if (station == null) {
                    //navController.navigate(R.id.nav_item_stations, bundleOf("add_station" to true))

                    //val bundle = bundleOf("add_station" to true)
                    //navController.navigate(
                    //    R.id.nav_item_stations, bundle, NavOptions.Builder()
                    //        .build()
                    //)

                    Log.i(LOG_TAG, "navController.previousBackStackEntry: add_station: true")
                    //navController.currentBackStackEntry?.savedStateHandle?.set("add_station", true)
                    navController.previousBackStackEntry?.savedStateHandle?.set("add_station", true)

                    //binding.appBarMain.contentMain.bottomNav.selectedItemId = R.id.nav_item_stations
                    val menuItem = binding.navView.menu.findItem(R.id.nav_item_stations)
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                }
            }
        }

        //// Only Handel Intent Once Here after App Start
        //if (savedInstanceState?.getBoolean("intentHandled") != true) {
        //    Log.i(LOG_TAG, "TRIGGER NEW INTENT FROM ONCREATE")
        //    onNewIntent(intent)
        //}
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "onOptionsItemSelected: $item")
        return when (item.itemId) {
            R.id.option_add_station -> {
                Log.d(LOG_TAG, "ADD STATION")
                val bundle = bundleOf("add_station" to true)
                navController.navigate(
                    R.id.nav_item_stations, bundle, NavOptions.Builder()
                        .setPopUpTo(navController.graph.startDestinationId, false)
                        .build()
                )

                //navController.navigate(R.id.nav_item_stations, bundle)
                // TODO: YET ANOTHER GHETTO Navigation Hack...
                //val dest = when (navController.currentDestination?.id!!) {
                //    R.id.nav_item_settings_widget,
                //    R.id.nav_item_settings_debug -> {
                //        Log.d(LOG_TAG, "dest: nav_item_settings")
                //        R.id.nav_item_settings
                //    }
                //
                //    else -> navController.currentDestination?.id!!
                //}
                //Log.d(LOG_TAG, "dest: $dest")
                //navController.navigate(
                //    R.id.nav_item_stations, bundle, NavOptions.Builder()
                //        .setPopUpTo(dest, true)
                //        .build()
                //)
                true
            }

            R.id.option_github -> {
                Log.d(LOG_TAG, "GITHUB")
                val url = getString(R.string.github_url)
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
                true
            }

            R.id.option_developer -> {
                Log.d(LOG_TAG, "DEVELOPER")
                val url = getString(R.string.website_url)
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
                true
            }

            else -> {
                // TODO: Title is null on Menu and not destinations, so this avoids warnings...
                if (item.title != null) {
                    NavigationUI.onNavDestinationSelected(item, navController) ||
                            super.onOptionsItemSelected(item)
                } else {
                    super.onOptionsItemSelected(item)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        // THe navController has been set as a private lateinit var
        //val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //override fun onNewIntent(intent: Intent) {
    //    super.onNewIntent(intent)
    //    Log.d("onNewIntent", "intent: $intent")
    //    val data = intent.data
    //    val action = intent.action
    //    Log.d("onNewIntent", "${action}: $data")
    //}
    //
    //override fun onSaveInstanceState(outState: Bundle) {
    //    super.onSaveInstanceState(outState)
    //    outState.putBoolean("intentHandled", true)
    //}

    //override fun onStart() {
    //    super.onStart()
    //    Log.d(LOG_TAG, "MainActivity - onStart")
    //}

    //override fun onResume() {
    //    super.onResume()
    //    Log.d(LOG_TAG, "MainActivity - onResume")
    //}

    //override fun onPause() {
    //    Log.d(LOG_TAG, "MainActivity - onPause")
    //    super.onPause()
    //}

    override fun onStop() {
        Log.d(LOG_TAG, "MainActivity - onStop")
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, WidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        WidgetProvider().onUpdate(this, appWidgetManager, ids)
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "ON DESTROY")
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
        super.onDestroy()
    }
}
