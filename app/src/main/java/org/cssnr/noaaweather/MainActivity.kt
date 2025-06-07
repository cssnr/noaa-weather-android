package org.cssnr.noaaweather

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import org.cssnr.noaaweather.databinding.ActivityMainBinding
import org.cssnr.noaaweather.widget.WidgetProvider
import org.cssnr.noaaweather.work.APP_WORKER_CONSTRAINTS
import org.cssnr.noaaweather.work.AppWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
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
        Log.d(LOG_TAG, "MainActivity: onCreate")

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        //binding.appBarMain.fab.setOnClickListener { view ->
        //    Log.d(LOG_TAG, "binding.appBarMain.fab.setOnClickListener")
        //    //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //    //    .setAction("Action", null)
        //    //    .setAnchorView(R.id.fab).show()
        //    val newFragment = AddDialogFragment()
        //    newFragment.show(supportFragmentManager, "AddDialogFragment")
        //}

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_item_home, R.id.nav_item_stations, R.id.nav_item_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val bottomNav = findViewById<View?>(R.id.bottom_nav) as BottomNavigationView
        setupWithNavController(bottomNav, navController)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            false
        //drawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT)

        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")

        val headerView = binding.navView.getHeaderView(0)
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d(LOG_TAG, "formattedVersion: $formattedVersion")
        versionTextView.text = formattedVersion

        // The setNavigationItemSelectedListener is optional for manual processing
        //navView.setNavigationItemSelectedListener { item ->
        //    Log.d(LOG_TAG, "item: $item")
        //    when (item.itemId) {
        //        R.id.nav_home -> navController.navigate(R.id.nav_home)
        //        R.id.nav_gallery -> navController.navigate(R.id.nav_gallery)
        //        R.id.nav_slideshow -> navController.navigate(R.id.nav_slideshow)
        //    }
        //    binding.drawerLayout.closeDrawer(GravityCompat.START)
        //    true
        //}

        // TODO: Ghetto manual fix for selecting items on sub item navigation...
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(LOG_TAG, "NAV CONTROLLER - destination: ${destination.label}")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (destination.id) {
                R.id.nav_item_settings_widget,
                R.id.nav_item_settings_debug -> {
                    Log.d(LOG_TAG, "nav_item_settings_XXX")
                    bottomNav.menu.findItem(R.id.nav_item_settings).isChecked = true
                    //navView.setCheckedItem(R.id.nav_item_settings)
                    val menu = navView.menu
                    for (i in 0 until menu.size) {
                        val item = menu[i]
                        item.isChecked = item.itemId == R.id.nav_item_settings
                    }
                }
            }
        }

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

        if (!preferences.contains("first_run_shown")) {
            Log.i(LOG_TAG, "FIRST RUN DETECTED")
            preferences.edit {
                putBoolean("first_run_shown", true)
            }
            val bundle = bundleOf("add_station" to true)
            //navController.navigate(R.id.nav_item_stations, bundle)
            navController.navigate(
                R.id.nav_item_stations, bundle, NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestinationId, true)
                    .build()
            )
            //navController.navigate(
            //    R.id.nav_item_stations, bundle, NavOptions.Builder()
            //        .setPopUpTo(R.id.nav_item_home, true)
            //        .build()
            //)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "onOptionsItemSelected: $item")
        return when (item.itemId) {
            R.id.option_add_station -> {
                Log.d(LOG_TAG, "ADD STATION")
                val bundle = bundleOf("add_station" to true)
                //navController.navigate(R.id.nav_item_stations, bundle)
                // TODO: YET ANOTHER GHETTO Navigation Hack...
                val dest = when (navController.currentDestination?.id!!) {
                    R.id.nav_item_settings_widget,
                    R.id.nav_item_settings_debug -> {
                        Log.d(LOG_TAG, "dest: nav_item_settings")
                        R.id.nav_item_settings
                    }

                    else -> navController.currentDestination?.id!!
                }
                Log.d(LOG_TAG, "dest: $dest")
                navController.navigate(
                    R.id.nav_item_stations, bundle, NavOptions.Builder()
                        .setPopUpTo(dest, true)
                        .build()
                )
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
            //else -> super.onOptionsItemSelected(item)
            else -> NavigationUI.onNavDestinationSelected(item, navController)
                    || super.onOptionsItemSelected(item)
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

    //override fun onStart() {
    //    super.onStart()
    //    Log.d(LOG_TAG, "MainActivity - onStart")
    //}

    //override fun onResume() {
    //    super.onResume()
    //    Log.d(LOG_TAG, "MainActivity - onResume")
    //}

    //override fun onPause() {
    //    super.onPause()
    //    Log.d(LOG_TAG, "MainActivity - onPause")
    //}

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "MainActivity - onStop")
        this.updateWidget()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "ON DESTROY")
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun Context.updateWidget() {
        Log.d("updateWidget", "Context.updateWidget")

        //val appWidgetManager = AppWidgetManager.getInstance(this)
        //val componentName = ComponentName(this, WidgetProvider::class.java)
        //val ids = appWidgetManager.getAppWidgetIds(componentName)
        //appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
        //WidgetProvider().onUpdate(this, appWidgetManager, ids)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, WidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        WidgetProvider().onUpdate(this, appWidgetManager, ids)
    }
}
