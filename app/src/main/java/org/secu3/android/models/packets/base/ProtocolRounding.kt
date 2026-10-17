package org.secu3.android.models.packets.base

/** Matches Manager MathHelpers::Round, including negative half-count ties. */
internal fun Float.protocolRoundToInt(): Int {
    require(isFinite()) { "Non-finite protocol value" }
    return (this + .5f - if (this < 0f) 1f else 0f).toInt()
}
