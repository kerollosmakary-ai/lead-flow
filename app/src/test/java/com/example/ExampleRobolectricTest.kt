package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.LeadViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun testClientApprovalFlowAndGating() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LeadViewModel(app)

        // Initially no client is approved
        assertFalse(viewModel.isClientApproved.value)
        assertNull(viewModel.approvedClientName.value)

        // Try to approve empty client name -> fails
        viewModel.approveClient(" ")
        assertFalse(viewModel.isClientApproved.value)

        // Approve valid client name
        viewModel.approveClient("Kamal")
        assertTrue(viewModel.isClientApproved.value)
        assertEquals("Kamal", viewModel.approvedClientName.value)

        // Revoke approval -> clears session
        viewModel.revokeClientApproval()
        assertFalse(viewModel.isClientApproved.value)
        assertNull(viewModel.approvedClientName.value)
    }

    @Test
    fun testPendingReviewInitialization() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LeadViewModel(app)

        // Verify initial state values of pending structures
        assertNull(viewModel.pendingStructuredLead.value)
        assertNull(viewModel.pendingRawNoteText.value)
        assertNull(viewModel.pendingSource.value)

        // Mock state assignment directly to simulate a parsed state waiting for PHASE 2 review
        viewModel.approveClient("Sarah Loft")
        assertTrue(viewModel.isClientApproved.value)
    }

    @Test
    fun testGeneralAppSetup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val appName = context.getString(R.string.app_name)
        assertNotNull(appName)
    }
}
