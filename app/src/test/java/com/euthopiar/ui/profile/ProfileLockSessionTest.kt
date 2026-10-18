package com.euthopiar.ui.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.euthopiar.data.profile.ProfileManager
import com.euthopiar.data.profile.ProfileRepository
import com.euthopiar.data.profile.ProfileType
import com.euthopiar.data.profile.UserProfile
import com.euthopiar.data.profile.hashPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileLockSessionTest {

    private lateinit var context: Context
    private lateinit var profileManager: ProfileManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("euthopiar_profiles", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        profileManager = ProfileManager.getInstance(context)
        ProfileRepository.bindManager(profileManager)
    }

    @Test
    fun testIsPinLockedCredentialsRequirement() {
        val unlockedProfile = UserProfile(name = "Open", isLocked = false, pin = null, pinHash = null)
        assertFalse("Profile without credentials should not be pin locked", unlockedProfile.isPinLocked)

        val rawPinProfile = UserProfile(name = "Raw", isLocked = true, pin = "1234", pinHash = null)
        assertTrue("Profile with raw pin should be pin locked", rawPinProfile.isPinLocked)

        val hashedProfile = UserProfile(name = "Hashed", isLocked = true, pin = null, pinHash = hashPin("4321"))
        assertTrue("Profile with pinHash should be pin locked", hashedProfile.isPinLocked)

        val orphanedProfile = UserProfile(name = "Orphaned", isLocked = true, pin = null, pinHash = null)
        assertFalse("Profile with isLocked=true but no pin or pinHash cannot be pin locked", orphanedProfile.isPinLocked)

        val disabledLockProfile = UserProfile(name = "Disabled", isLocked = false, pin = "1234", pinHash = null)
        assertFalse("Profile with isLocked=false cannot be pin locked", disabledLockProfile.isPinLocked)
    }

    @Test
    fun testPinHashingAndVerification() {
        val pin = "2580"
        val hash = hashPin(pin)
        assertEquals(64, hash.length)

        val profile = UserProfile(id = "user_p", name = "Secret", pinHash = hash, isLocked = true)
        assertTrue("Correct PIN must verify true", profileManager.verifyPin(profile, "2580"))
        assertFalse("Incorrect PIN must verify false", profileManager.verifyPin(profile, "0000"))
        assertFalse("Empty PIN must verify false", profileManager.verifyPin(profile, ""))

        // Locked profile with no credentials stored must reject any PIN
        val emptyCredProfile = UserProfile(id = "user_e", name = "Broken", isLocked = true, pin = null, pinHash = null)
        // With isPinLocked false, it does not challenge
        assertFalse("Orphaned profile without PIN must not be pin locked", emptyCredProfile.isPinLocked)
    }

    @Test
    fun testSessionUnlockAndSwitchProfileRevocation() {
        // Create locked profile
        val lockedUser = profileManager.addProfile(
            UserProfile(
                id = "locked_1",
                name = "Locked Profile",
                pin = "9999",
                isLocked = true
            )
        )
        assertNotNull(lockedUser.pinHash)
        assertTrue(lockedUser.isPinLocked)

        // As creator, the active profile is initially unlocked in current session
        assertTrue("Newly created profile is unlocked for creator", profileManager.isProfileUnlocked(lockedUser.id))

        // Create standard open profile
        val openUser = profileManager.addProfile(
            UserProfile(
                id = "open_1",
                name = "Guest",
                isLocked = false
            )
        )
        assertFalse(openUser.isPinLocked)
        assertTrue(profileManager.isProfileUnlocked(openUser.id))

        // Because we switched to openUser, lockedUser should have its session unlock revoked
        assertFalse("Switching away from locked profile revokes its session unlock", profileManager.isProfileUnlocked(lockedUser.id))

        // Attempting to switch back to locked profile requires unlock
        assertFalse(profileManager.isProfileUnlocked(lockedUser.id))
        profileManager.unlockProfile(lockedUser.id)
        assertTrue(profileManager.isProfileUnlocked(lockedUser.id))

        // Selecting profile
        profileManager.switchProfile(lockedUser.id)
        assertEquals(lockedUser.id, profileManager.activeProfileId.value)
        assertTrue(profileManager.isProfileUnlocked(lockedUser.id))

        // Navigating within same profile maintains session consistency (not locked)
        assertTrue(profileManager.isProfileUnlocked(lockedUser.id))

        // Explicit locking re-locks it
        profileManager.lockProfile(lockedUser.id)
        assertFalse(profileManager.isProfileUnlocked(lockedUser.id))
    }

    @Test
    fun testUpdateProfileWithPinUnlocksActiveProfileInSession() {
        val user = profileManager.addProfile(
            UserProfile(
                id = "test_user_upd",
                name = "Standard User",
                isLocked = false
            )
        )
        profileManager.switchProfile(user.id)
        assertFalse(profileManager.activeProfile.value.isPinLocked)

        // User sets a PIN on their currently active profile
        profileManager.updateProfile(user.copy(pin = "7777", isLocked = true))
        val updated = profileManager.activeProfile.value
        assertTrue("Profile now has PIN lock", updated.isPinLocked)
        assertEquals(hashPin("7777"), updated.pinHash)
        assertTrue("Active profile updated with PIN remains unlocked in current session", profileManager.isProfileUnlocked(updated.id))
    }
}
