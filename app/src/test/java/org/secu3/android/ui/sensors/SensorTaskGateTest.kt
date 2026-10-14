package org.secu3.android.ui.sensors

import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.utils.Task

class SensorTaskGateTest {
    @Test
    fun onlyResumedRouteDispatchesSettledPageAndResumeReissuesCurrentTask() {
        val tasks = mutableListOf<Task>()
        val gate = SensorTaskGate(initialPage = 9, sendTask = tasks::add)

        gate.selectPage(1)
        assertEquals(emptyList<Task>(), tasks)
        gate.setResumed(true)
        gate.setResumed(true)
        assertEquals(listOf(Task.Secu3ReadRawSensors), tasks)

        gate.selectPage(0)
        assertEquals(listOf(Task.Secu3ReadRawSensors, Task.Secu3ReadSensors), tasks)
        gate.setResumed(false)
        gate.selectPage(1)
        assertEquals(listOf(Task.Secu3ReadRawSensors, Task.Secu3ReadSensors), tasks)
        gate.setResumed(true)
        assertEquals(listOf(Task.Secu3ReadRawSensors, Task.Secu3ReadSensors, Task.Secu3ReadRawSensors), tasks)
    }
}
