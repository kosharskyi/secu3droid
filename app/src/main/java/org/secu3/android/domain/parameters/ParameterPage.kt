/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.parameters

/** ECU parameter pages, independent of Android resource identifiers. */
enum class ParameterPage {
    STARTER,
    ANGLES,
    IDLING,
    TEMPERATURE,
    FUEL_CUTOFF,
    CKPS,
    ADC_ERRORS,
    KNOCK,
    LAMBDA_CONTROL,
    ACCELERATION,
    LTFT,
    FUNCTIONS,
    MISCELLANEOUS,
    SECURITY,
    CHOKE_CONTROL,
    GAS_DOSE,
    EL_THROTTLE,
    FUEL_INJECTION,
    UNIVERSAL_OUTPUTS,
}
