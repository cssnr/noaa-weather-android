package org.cssnr.noaaweather

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.databinding.ActivityMainBinding
import org.cssnr.noaaweather.ui.WidgetProvider
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        const val LOG_TAG = "NOAAWeather"
    }

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

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
        navController = findNavController(R.id.nav_host_fragment_content_main)
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

        // TODO: Improve initialization of the WorkRequest
        val sharedPreferences = this.getSharedPreferences("org.cssnr.noaaweather", MODE_PRIVATE)
        val workInterval = sharedPreferences.getString("work_interval", null) ?: "15"
        Log.i(LOG_TAG, "workInterval: $workInterval")
        Log.i(LOG_TAG, "raw: ${sharedPreferences.getString("work_interval", null)}")
        if (workInterval != "0") {
            val workRequest =
                PeriodicWorkRequestBuilder<AppWorker>(workInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .setRequiresCharging(false)
                            .setRequiresDeviceIdle(false)
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            Log.i(LOG_TAG, "workRequest: $workRequest")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "app_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "onOptionsItemSelected: $item")
        return when (item.itemId) {
            R.id.action_support -> {
                Log.d(LOG_TAG, "ACTION SUPPORT")
                val url = getString(R.string.github_url)
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
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
}

fun Context.updateWidget() {
    Log.d(LOG_TAG, "Context.updateWidget")

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
