package app.lawnchair.baseline

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a comprehensive startup baseline profile for AutoCat.
 *
 * This profile includes critical user journeys:
 * - Cold app startup
 * - Opening app drawer
 * - Scrolling through apps
 * - Opening settings
 * - Navigating preference screens
 *
 * Run the generator with:
 * ```
 * ./gradlew :baseline-profile:generateBaselineProfile
 * ```
 *
 * After running, verify improvements with [StartupBenchmarks].
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    @RequiresApi(Build.VERSION_CODES.P)
    val rule = BaselineProfileRule()

    @Test
    @RequiresApi(Build.VERSION_CODES.P)
    fun generateStartupProfile() {
        rule.collect(Constants.PACKAGE_NAME) {
            // Start from home screen
            pressHome()
            
            // Launch AutoCat (cold start)
            startActivityAndWait()
            
            // Wait for launcher to settle
            device.waitForIdle()
            Thread.sleep(500)
            
            // Critical Journey 1: Open App Drawer
            // This is the most common user action after launch
            val appsButton = device.findObject(By.res("apps_view"))
            if (appsButton.exists) {
                appsButton.click()
                device.waitForIdle()
                Thread.sleep(300)
                
                // Scroll through app list to trigger loading
                val appsList = device.findObject(By.res("apps_list_view"))
                if (appsList.exists) {
                    appsList.scroll(Direction.DOWN, 0.5f)
                    device.waitForIdle()
                    Thread.sleep(200)
                    
                    appsList.scroll(Direction.UP, 0.5f)
                    device.waitForIdle()
                    Thread.sleep(200)
                }
                
                // Return to home
                device.pressBack()
                device.waitForIdle()
            }
            
            // Critical Journey 2: Open Settings
            // Access settings from home screen
            val settingsButton = device.findObject(By.desc("Settings"))
                ?: device.findObject(By.res("settings_button"))
            if (settingsButton?.exists == true) {
                settingsButton.click()
                device.waitForIdle()
                Thread.sleep(300)
                
                // Navigate to General Preferences
                val generalPref = device.findObject(By.textContains("General"))
                    ?: device.findObject(By.descContains("General"))
                if (generalPref?.exists == true) {
                    generalPref.click()
                    device.waitForIdle()
                    Thread.sleep(200)
                    
                    // Scroll through preferences
                    val preferencesList = device.findObject(By.res("preference_list"))
                    preferencesList?.scroll(Direction.DOWN, 0.3f)
                    device.waitForIdle()
                    
                    // Navigate back
                    device.pressBack()
                    device.waitForIdle()
                }
                
                // Navigate to App Drawer Preferences
                val drawerPref = device.findObject(By.textContains("App Drawer"))
                    ?: device.findObject(By.descContains("Drawer"))
                if (drawerPref?.exists == true) {
                    drawerPref.click()
                    device.waitForIdle()
                    Thread.sleep(200)
                    
                    device.pressBack()
                    device.waitForIdle()
                }
                
                // Return to home
                device.pressBack()
                device.waitForIdle()
            }
            
            // Critical Journey 3: Quick Settings Panel
            // Test overview/recents if enabled
            try {
                device.pressRecentApps()
                device.waitForIdle()
                Thread.sleep(300)
                device.pressBack()
                device.waitForIdle()
            } catch (e: Exception) {
                // Recents might not be available
            }
            
            // Final settle time
            device.waitForIdle()
            Thread.sleep(500)
        }
    }
    
    @Test
    @RequiresApi(Build.VERSION_CODES.P)
    fun generateAppDrawerProfile() {
        rule.collect(Constants.PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
            
            // Focus on app drawer performance
            repeat(3) { iteration ->
                // Open drawer
                val appsButton = device.findObject(By.res("apps_view"))
                if (appsButton.exists) {
                    appsButton.click()
                    device.waitForIdle()
                    Thread.sleep(200)
                    
                    // Scroll extensively
                    val appsList = device.findObject(By.res("apps_list_view"))
                    if (appsList.exists) {
                        repeat(2) {
                            appsList.scroll(Direction.DOWN, 0.7f)
                            device.waitForIdle()
                        }
                        repeat(2) {
                            appsList.scroll(Direction.UP, 0.7f)
                            device.waitForIdle()
                        }
                    }
                    
                    // Close drawer
                    device.pressBack()
                    device.waitForIdle()
                    Thread.sleep(100)
                }
            }
        }
    }
}
