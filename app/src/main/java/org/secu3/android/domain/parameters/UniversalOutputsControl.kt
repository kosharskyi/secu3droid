package org.secu3.android.domain.parameters

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import javax.inject.Inject

/** Immutable presentation values for one configured output. */
data class UniversalOutputValues(
    val enabled: Boolean, val condition1: Int, val condition2: Int,
    val condition1Inversion: Boolean, val condition2Inversion: Boolean,
    val inversion: Boolean, val logicFunction: Int,
    val onThreshold1: Float, val offThreshold1: Float,
    val onThreshold2: Float, val offThreshold2: Float,
)

data class UniversalOutputsValues(val outputs: List<UniversalOutputValues>, val logicFunction12: Int) {
    val pairFunctionAvailable: Boolean get() = outputs.size >= 2 && outputs[0].enabled && outputs[1].enabled
}

/** Owns Universal Outputs packet edits and sends immutable snapshots through ParameterSession. */
@ViewModelScoped
class UniversalOutputsControl internal constructor(
    private val conditions: UniversalOutputConditions,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject constructor(conditions: UniversalOutputConditions, parameterSession: ParameterSession) : this(conditions, parameterSession::sendPacket)

    private var packet: UniOutParamPacket? = null
    private val mutableValues = MutableStateFlow<UniversalOutputsValues?>(null)
    val values: StateFlow<UniversalOutputsValues?> = mutableValues.asStateFlow()

    fun reset() { packet = null; mutableValues.value = null }
    fun receive(received: UniOutParamPacket) { packet = received; publish() }

    fun setOutputEnabled(output: Int, enabled: Boolean) = edit(output in 1..6) { p -> setEnabled(p, output, enabled) }
    fun selectCondition(output: Int, slot: Int, code: Int): Boolean {
        val p = packet ?: return false
        if (output !in 1..6 || slot !in 1..2 || !isEnabled(p, output) || !conditions.availableInSlot(code, slot)) return false
        if (slot == 2 && logic(p, output) == 15) return false
        val spec = conditions.get(code) ?: return false
        return edit { current ->
            setCondition(current, output, slot, code)
            setThreshold(current, output, slot, true, spec.onDefault)
            setThreshold(current, output, slot, false, spec.offDefault)
        }
    }
    fun setConditionInverted(output: Int, slot: Int, inverted: Boolean): Boolean = edit(outputAllowed(output, slot, second = true)) { setInverted(it, output, slot, inverted) }
    fun setResultInverted(output: Int, inverted: Boolean): Boolean = edit(outputAllowed(output)) { setResultInverted(it, output, inverted) }
    fun selectLogicFunction(output: Int, code: Int): Boolean = edit(outputAllowed(output) && code in LOGIC_CODES) { setLogic(it, output, code) }
    fun editOnThreshold(output: Int, slot: Int, expectedCondition: Int, value: Float): Boolean = editThreshold(output, slot, expectedCondition, value, true)
    fun editOffThreshold(output: Int, slot: Int, expectedCondition: Int, value: Float): Boolean = editThreshold(output, slot, expectedCondition, value, false)
    fun selectPairFunction(code: Int): Boolean {
        val p = packet ?: return false
        if (code !in LOGIC_CODES || mutableValues.value?.pairFunctionAvailable != true) return false
        return edit { it.logicFunction_1_2 = code }
    }

    private fun editThreshold(output: Int, slot: Int, expected: Int, value: Float, on: Boolean): Boolean {
        val p = packet ?: return false
        if (output !in 1..6 || slot !in 1..2 || !isEnabled(p, output)) return false
        if (slot == 2 && logic(p, output) == 15) return false
        if (condition(p, output, slot) != expected) return false
        val format = conditions.get(expected) ?: return false
        if (!value.isFinite() || value !in format.min..format.max) return false
        return edit { setThreshold(it, output, slot, on, value) }
    }

    private fun outputAllowed(output: Int, slot: Int = 1, second: Boolean = false): Boolean {
        val p = packet ?: return false
        return output in 1..6 && slot in 1..2 && isEnabled(p, output) && (!second || slot == 1 || logic(p, output) != 15)
    }

    private inline fun edit(valid: Boolean = true, change: (UniOutParamPacket) -> Unit): Boolean {
        if (!valid) return false
        val current = packet ?: return false
        val before = current.checkpointRawFields()
        val admitted = try {
            change(current)
            val bytes = current.pack().copyOf()
            sendPacket(object : OutputPacket { override fun pack(): IntArray = bytes.copyOf() })
        } catch (_: IllegalArgumentException) { false }
        if (!admitted) { current.restoreRawFields(before); return false }
        publish()
        return true
    }

    private fun publish() {
        val p = packet ?: return
        mutableValues.value = UniversalOutputsValues((1..6).map { readOutput(p, it) }, p.logicFunction_1_2)
    }

    private fun readOutput(p: UniOutParamPacket, output: Int): UniversalOutputValues = when (output) {
        1 -> UniversalOutputValues(p.output1Use, p.output1Condition1, p.output1Condition2, p.output1Cond1Inversion, p.output1Cond2Inversion, p.output1Inversion, p.output1LogicFunc, p.output1OnThrd1, p.output1OffThrd1, p.output1OnThrd2, p.output1OffThrd2)
        2 -> UniversalOutputValues(p.output2Use, p.output2Condition1, p.output2Condition2, p.output2Cond1Inversion, p.output2Cond2Inversion, p.output2Inversion, p.output2LogicFunc, p.output2OnThrd1, p.output2OffThrd1, p.output2OnThrd2, p.output2OffThrd2)
        3 -> UniversalOutputValues(p.output3Use, p.output3Condition1, p.output3Condition2, p.output3Cond1Inversion, p.output3Cond2Inversion, p.output3Inversion, p.output3LogicFunc, p.output3OnThrd1, p.output3OffThrd1, p.output3OnThrd2, p.output3OffThrd2)
        4 -> UniversalOutputValues(p.output4Use, p.output4Condition1, p.output4Condition2, p.output4Cond1Inversion, p.output4Cond2Inversion, p.output4Inversion, p.output4LogicFunc, p.output4OnThrd1, p.output4OffThrd1, p.output4OnThrd2, p.output4OffThrd2)
        5 -> UniversalOutputValues(p.output5Use, p.output5Condition1, p.output5Condition2, p.output5Cond1Inversion, p.output5Cond2Inversion, p.output5Inversion, p.output5LogicFunc, p.output5OnThrd1, p.output5OffThrd1, p.output5OnThrd2, p.output5OffThrd2)
        6 -> UniversalOutputValues(p.output6Use, p.output6Condition1, p.output6Condition2, p.output6Cond1Inversion, p.output6Cond2Inversion, p.output6Inversion, p.output6LogicFunc, p.output6OnThrd1, p.output6OffThrd1, p.output6OnThrd2, p.output6OffThrd2)
        else -> error("Output index must be between 1 and 6")
    }

    private fun isEnabled(p: UniOutParamPacket, output: Int): Boolean = when (output) {
        1 -> p.output1Use
        2 -> p.output2Use
        3 -> p.output3Use
        4 -> p.output4Use
        5 -> p.output5Use
        6 -> p.output6Use
        else -> false
    }
    private fun condition(p: UniOutParamPacket, output: Int, slot: Int): Int = when (output) {
        1 -> if (slot == 1) p.output1Condition1 else p.output1Condition2
        2 -> if (slot == 1) p.output2Condition1 else p.output2Condition2
        3 -> if (slot == 1) p.output3Condition1 else p.output3Condition2
        4 -> if (slot == 1) p.output4Condition1 else p.output4Condition2
        5 -> if (slot == 1) p.output5Condition1 else p.output5Condition2
        6 -> if (slot == 1) p.output6Condition1 else p.output6Condition2
        else -> -1
    }
    private fun logic(p: UniOutParamPacket, output: Int): Int = when (output) {
        1 -> p.output1LogicFunc
        2 -> p.output2LogicFunc
        3 -> p.output3LogicFunc
        4 -> p.output4LogicFunc
        5 -> p.output5LogicFunc
        6 -> p.output6LogicFunc
        else -> -1
    }
    private fun setEnabled(p: UniOutParamPacket, output: Int, v: Boolean) { when (output) {
        1 -> p.output1Use = v
        2 -> p.output2Use = v
        3 -> p.output3Use = v
        4 -> p.output4Use = v
        5 -> p.output5Use = v
        6 -> p.output6Use = v
    } }
    private fun setCondition(p: UniOutParamPacket, output: Int, slot: Int, v: Int) { when (output) {
        1 -> if (slot == 1) p.output1Condition1 = v else p.output1Condition2 = v
        2 -> if (slot == 1) p.output2Condition1 = v else p.output2Condition2 = v
        3 -> if (slot == 1) p.output3Condition1 = v else p.output3Condition2 = v
        4 -> if (slot == 1) p.output4Condition1 = v else p.output4Condition2 = v
        5 -> if (slot == 1) p.output5Condition1 = v else p.output5Condition2 = v
        6 -> if (slot == 1) p.output6Condition1 = v else p.output6Condition2 = v
    } }
    private fun setThreshold(p: UniOutParamPacket, output: Int, slot: Int, on: Boolean, v: Float) { when (output) {
        1 -> when (slot) { 1 -> if (on) p.output1OnThrd1 = v else p.output1OffThrd1 = v; 2 -> if (on) p.output1OnThrd2 = v else p.output1OffThrd2 = v }
        2 -> when (slot) { 1 -> if (on) p.output2OnThrd1 = v else p.output2OffThrd1 = v; 2 -> if (on) p.output2OnThrd2 = v else p.output2OffThrd2 = v }
        3 -> when (slot) { 1 -> if (on) p.output3OnThrd1 = v else p.output3OffThrd1 = v; 2 -> if (on) p.output3OnThrd2 = v else p.output3OffThrd2 = v }
        4 -> when (slot) { 1 -> if (on) p.output4OnThrd1 = v else p.output4OffThrd1 = v; 2 -> if (on) p.output4OnThrd2 = v else p.output4OffThrd2 = v }
        5 -> when (slot) { 1 -> if (on) p.output5OnThrd1 = v else p.output5OffThrd1 = v; 2 -> if (on) p.output5OnThrd2 = v else p.output5OffThrd2 = v }
        6 -> when (slot) { 1 -> if (on) p.output6OnThrd1 = v else p.output6OffThrd1 = v; 2 -> if (on) p.output6OnThrd2 = v else p.output6OffThrd2 = v }
    } }
    private fun setInverted(p: UniOutParamPacket, output: Int, slot: Int, v: Boolean) { when (output) {
        1 -> if (slot == 1) p.output1Cond1Inversion = v else p.output1Cond2Inversion = v
        2 -> if (slot == 1) p.output2Cond1Inversion = v else p.output2Cond2Inversion = v
        3 -> if (slot == 1) p.output3Cond1Inversion = v else p.output3Cond2Inversion = v
        4 -> if (slot == 1) p.output4Cond1Inversion = v else p.output4Cond2Inversion = v
        5 -> if (slot == 1) p.output5Cond1Inversion = v else p.output5Cond2Inversion = v
        6 -> if (slot == 1) p.output6Cond1Inversion = v else p.output6Cond2Inversion = v
    } }
    private fun setResultInverted(p: UniOutParamPacket, output: Int, v: Boolean) { when (output) {
        1 -> p.output1Inversion = v
        2 -> p.output2Inversion = v
        3 -> p.output3Inversion = v
        4 -> p.output4Inversion = v
        5 -> p.output5Inversion = v
        6 -> p.output6Inversion = v
    } }
    private fun setLogic(p: UniOutParamPacket, output: Int, v: Int) { when (output) {
        1 -> p.output1LogicFunc = v
        2 -> p.output2LogicFunc = v
        3 -> p.output3LogicFunc = v
        4 -> p.output4LogicFunc = v
        5 -> p.output5LogicFunc = v
        6 -> p.output6LogicFunc = v
    } }

    companion object { private val LOGIC_CODES = setOf(0, 1, 2, 3, 15) }
}
