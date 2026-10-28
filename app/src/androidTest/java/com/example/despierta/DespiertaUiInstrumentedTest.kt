package com.example.despierta

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingPolicies
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class DespiertaUiInstrumentedTest {
    @get:Rule(order = 0)
    val notificationPermission: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launchWithoutLiveClockTicker() {
        // Experimento acotado: el emulador de CI tarda más de 12 s en mostrar la actividad.
        // Un límite de 120 s distingue lentitud (pasará) de un bloqueo permanente (seguirá fallando).
        IdlingPolicies.setIdlingResourceTimeout(120, TimeUnit.SECONDS)
        IdlingPolicies.setMasterPolicyTimeout(150, TimeUnit.SECONDS)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_DISABLE_LIVE_CLOCK_FOR_TESTS, true)
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun closeActivity() {
        scenario.close()
    }

    @Test
    fun versionAndPrimaryNavigationAreVisible() {
        composeRule.onNodeWithText("DESPIERTA").assertIsDisplayed()
        composeRule.onNodeWithTag("live_clock").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_duration_card").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_shifts").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_settings").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_shifts").performClick()
        composeRule.onNodeWithTag("shift_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    @Test
    fun sleepScreenShowsScheduleGoalAndEditsWithWheels() {
        composeRule.onNodeWithTag("nav_sleep").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("sleep_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_bedtime_card").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_wake_card").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_duration_summary").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_goal_progress").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_days").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("sleep_wake_card").performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithTag("sleep_time_editor").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_bedtime_card").performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithTag("sleep_wake_card").assertIsNotSelected()
    }

    @Test
    fun settingsAreOrganizedInCategories() {
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_appearance").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_page_appearance").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_noche").performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithTag("settings_back").performClick()

        composeRule.onNodeWithTag("settings_notifications").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_page_notifications").assertIsDisplayed()
        composeRule.onNodeWithTag("notify_master").assertIsDisplayed()
    }

    @Test
    fun alarmEditorShowsWheelPickerAndManualEntry() {
        composeRule.onNodeWithTag("add_alarm").performClick()

        composeRule.onNodeWithTag("time_wheel_hour").assertIsDisplayed()
        composeRule.onNodeWithTag("time_wheel_minute").assertIsDisplayed()
        composeRule.onNodeWithText("Introducir hora manualmente").performClick()
        composeRule.onNodeWithTag("manual_time_input").assertIsDisplayed()
    }

    @Test
    fun alarmEditorAllowsSnoozeAndVibrationConfiguration() {
        composeRule.onNodeWithTag("add_alarm").performClick()
        // Separa dos fases para que un fallo indique si Compose seguía ocupado al abrir el
        // editor o después de desplazar su contenido.
        composeRule.onNodeWithTag("time_wheel_hour").assertIsDisplayed()

        composeRule.onNodeWithTag("snooze_5").performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag("snooze_15").performClick().assertIsSelected()
        composeRule.onNodeWithTag("snooze_5").assertIsNotSelected()
        composeRule.onNodeWithTag("alarm_vibration")
            .performScrollTo()
            .assertIsOn()
            .performClick()
            .assertIsOff()
        composeRule.onNodeWithTag("preview_tone").performScrollTo().assertIsDisplayed()
    }
}
