package com.example.lostfound

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.example.lostfound.service.SessionManager
import com.example.lostfound.ui.login.LoginActivity
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import org.hamcrest.Matchers.`is` as hamcrestIs
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.lostfound.ui.main.MainActivity

@RunWith(AndroidJUnit4::class)
class CampusFoundFlowTest {

    @get:Rule
    val loginRule = ActivityTestRule(LoginActivity::class.java, true, false)

    @Before
    fun clearSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SessionManager(context).clearSession()
    }

    @Test
    fun loginScreen_showsWelcomeAndButtons() {
        loginRule.launchActivity(null)
        onView(withText("Welcome back")).check(matches(isDisplayed()))
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
        onView(withId(R.id.registerButton)).check(matches(isDisplayed()))
        onView(withId(R.id.darkModeButton)).check(matches(isDisplayed()))
    }

    @Test
    fun login_withValidCredentials_navigatesToHome() {
        seedTestUser()

        loginRule.launchActivity(null)

        onView(withId(R.id.emailInput)).perform(replaceText("test@university.edu"), closeSoftKeyboard())
        onView(withId(R.id.passwordInput)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        waitForMainActivity()
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))
        onView(withId(R.id.swipeRefresh)).check(matches(isDisplayed()))
    }

    @Test
    fun login_withPhoneNumber_navigatesToHome() {
        seedTestUser()

        loginRule.launchActivity(null)

        onView(withId(R.id.emailInput)).perform(typeText("012345678"), closeSoftKeyboard())
        onView(withId(R.id.passwordInput)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())

        waitForMainActivity()
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))
    }

    @Test
    fun login_registerDialog_opensAndCloses() {
        loginRule.launchActivity(null)

        onView(withId(R.id.registerButton)).perform(click())
        onView(withText(R.string.create_account)).check(matches(isDisplayed()))
        onView(withId(R.id.registerUsernameInput)).check(matches(isDisplayed()))
        onView(withId(R.id.closeRegisterButton)).perform(click())
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }

    @Test
    fun login_darkModeButton_isClickable() {
        loginRule.launchActivity(null)
        onView(withId(R.id.darkModeButton)).perform(click())
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_loadsItemsFromApi() {
        launchLoggedInMain()
        waitForApi()

        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))
        onView(withId(R.id.loadingProgress)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.swipeRefresh)).check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_searchWorks() {
        launchLoggedInMain()
        waitForApi()

        onView(withId(R.id.searchInput)).perform(typeText("wallet"), closeSoftKeyboard())
        Thread.sleep(800)
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavigation_switchesTabs() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.postItemFragment)).perform(click())
        onView(withText("Report Item")).check(matches(isDisplayed()))
        onView(withId(R.id.titleInput)).check(matches(isDisplayed()))

        onView(withId(R.id.profileFragment)).perform(click())
        onView(withId(R.id.userNameText)).check(matches(isDisplayed()))
        onView(withId(R.id.editProfileButton)).check(matches(isDisplayed()))
        onView(withId(R.id.darkModeButton)).check(matches(isDisplayed()))

        onView(withId(R.id.homeFragment)).perform(click())
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))
    }

    @Test
    fun postItemScreen_formFieldsVisible() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.postItemFragment)).perform(click())

        onView(withId(R.id.titleInput)).check(matches(isDisplayed()))
        onView(withId(R.id.lostOption)).check(matches(isDisplayed()))
        onView(withId(R.id.foundOption)).check(matches(isDisplayed()))
        onView(withId(R.id.submitButton)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.contactInput)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.useLocationButton)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun postItem_validationShowsErrorForEmptyForm() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.postItemFragment)).perform(click())
        onView(withId(R.id.submitButton)).perform(scrollTo(), click())

        onView(withId(R.id.formErrorText)).check(matches(isDisplayed()))
    }

    @Test
    fun profileScreen_showsUserInfoAndLogoutButton() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.profileFragment)).perform(click())

        onView(withId(R.id.userNameText)).check(matches(isDisplayed()))
        onView(withId(R.id.studentIdText)).check(matches(isDisplayed()))
        onView(withId(R.id.totalPostsText)).check(matches(isDisplayed()))
        onView(withId(R.id.logoutButton)).check(matches(isDisplayed()))
        onView(withId(R.id.editProfileButton)).check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_filterChip_canBeSelected() {
        launchLoggedInMain()
        waitForApi()

        onView(withTagValue(hamcrestIs("filter_chip_Electronics"))).perform(click())
        onView(withTagValue(hamcrestIs("filter_chip_Electronics"))).check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_fab_navigatesToReport() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.addItemFab)).perform(click())
        onView(withId(R.id.titleInput)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.submitButton)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_itemClick_opensDetail() {
        launchLoggedInMain()
        waitForApi()

        onView(withId(R.id.itemsRecyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
        waitForDetailActivity()
        onView(withId(R.id.detailTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.backButton)).perform(click())
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))
    }

    @Test
    fun profile_editProfileDialog_opens() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.profileFragment)).perform(click())
        onView(withId(R.id.editProfileButton)).perform(click())
        onView(withId(R.id.editNameInput)).check(matches(isDisplayed()))
        onView(withText(android.R.string.cancel)).perform(click())
        onView(withId(R.id.logoutButton)).check(matches(isDisplayed()))
    }

    @Test
    fun profile_logout_returnsToLogin() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.profileFragment)).perform(click())
        onView(withId(R.id.logoutButton)).perform(click())
        onView(withText("Are you sure you want to logout?")).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        waitForLoginActivity()
        onView(withText("Welcome back")).check(matches(isDisplayed()))
    }

    @Test
    fun postItem_lostFoundToggle_works() {
        launchLoggedInMain()
        Thread.sleep(1500)

        onView(withId(R.id.postItemFragment)).perform(click())
        onView(withId(R.id.foundOption)).perform(click())
        onView(withId(R.id.lostOption)).perform(click())
        onView(withId(R.id.lostOption)).check(matches(isDisplayed()))
    }

    private fun seedTestUser() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SessionManager(context).registerUser(
            "Test Student",
            "test@university.edu",
            "012345678",
            "password123"
        )
    }

    private fun launchLoggedInMain() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SessionManager(context).registerUser(
            "Flow Tester",
            "flow@university.edu",
            "098765432",
            "password123"
        )
        SessionManager(context).saveSession(
            email = "flow@university.edu",
            name = "Flow Tester",
            phone = "85598765432",
            studentId = "",
            rememberMe = true
        )

        loginRule.launchActivity(null)
        waitForMainActivity()
    }

    private fun waitForApi() {
        Thread.sleep(6000)
    }

    private fun waitForMainActivity(timeoutMs: Long = 15_000) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var found = false
            instrumentation.runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                found = resumed.any { it.javaClass == MainActivity::class.java }
            }
            if (found) {
                Thread.sleep(500)
                return
            }
            Thread.sleep(200)
        }
        throw AssertionError("MainActivity did not resume within ${timeoutMs}ms")
    }

    private fun waitForDetailActivity(timeoutMs: Long = 10_000) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var found = false
            instrumentation.runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                found = resumed.any {
                    it.javaClass.name.endsWith("ItemDetailActivity")
                }
            }
            if (found) return
            Thread.sleep(200)
        }
        throw AssertionError("ItemDetailActivity did not resume")
    }

    private fun waitForLoginActivity(timeoutMs: Long = 10_000) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            var found = false
            instrumentation.runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                found = resumed.any { it.javaClass == LoginActivity::class.java }
            }
            if (found) return
            Thread.sleep(200)
        }
        throw AssertionError("LoginActivity did not resume after logout")
    }
}
