package org.secu3.android.ui.parameters

import kotlinx.coroutines.Job
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ParameterPageReadJobsTest {
    @Test
    fun cancellationCallbacksCanRemoveEntriesWhileAllJobsAreCancelled() {
        val jobs = linkedMapOf<Int, Job>()
        val first = Job()
        val second = Job()
        jobs[1] = first
        jobs[2] = second
        first.invokeOnCompletion { if (jobs[1] === first) jobs.remove(1) }
        second.invokeOnCompletion { if (jobs[2] === second) jobs.remove(2) }

        cancelAndClearPageReadJobs(jobs)
        cancelAndClearPageReadJobs(jobs)

        assertTrue(first.isCancelled)
        assertTrue(second.isCancelled)
        assertTrue(jobs.isEmpty())
    }

    @Test
    fun replacementRegisteredByCancellationCallbackSurvivesCleanup() {
        val jobs = linkedMapOf<Int, Job>()
        val replacement = Job()
        val first = Job()
        val second = Job()
        jobs[1] = first
        jobs[2] = second
        first.invokeOnCompletion { jobs[1] = replacement }

        cancelAndClearPageReadJobs(jobs)

        assertTrue(first.isCancelled)
        assertTrue(second.isCancelled)
        assertSame(replacement, jobs[1])
        assertFalse(replacement.isCancelled)
    }
}
