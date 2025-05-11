package com.momentum.habittracker.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.momentum.habittracker.R
import com.momentum.habittracker.databinding.ActivityMainBinding
import com.momentum.habittracker.utils.ThemeUtils

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeUtils.applyTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)

        // Initialize Navigation
        navController = findNavController(R.id.nav_host_fragment)

        // Setup AppBar with NavController
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(
                R.id.habitsListFragment,
                R.id.calendarFragment,
                R.id.statisticsFragment
            ),
            fallbackOnNavigateUpListener = ::onSupportNavigateUp
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Connect BottomNavigationView with NavController
        binding.bottomNavigation.apply {
            setupWithNavController(navController)
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_habits -> {
                        navController.navigate(R.id.habitsListFragment)
                        true
                    }
                    R.id.navigation_calendar -> {
                        navController.navigate(R.id.calendarFragment)
                        true
                    }
                    R.id.navigation_stats -> {
                        navController.navigate(R.id.statisticsFragment)
                        true
                    }
                    else -> false
                }
            }
        }

        // Setup FAB
        binding.fabAddHabit.apply {
            show()
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AddEditHabitActivity::class.java))
            }
        }

        // Show FAB only on HabitsListFragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.fabAddHabit.visibility = when(destination.id) {
                R.id.habitsListFragment -> View.VISIBLE
                else -> View.GONE
            }

            // Update toolbar title based on destination
            supportActionBar?.title = when(destination.id) {
                R.id.habitsListFragment -> "Habits"
                R.id.calendarFragment -> "Calendar"
                R.id.statisticsFragment -> "Statistics"
                else -> ""
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}