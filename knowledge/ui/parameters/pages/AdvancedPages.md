## Hold controls must cancel and flush independently of their visual layout

Choke and gas-dose manual controls use the shared `ManualPositionButton` for repeated ECU step commands and periodic flush callbacks. The screen passes current activity and firmware support as enabled state, and controller cancellation discards pending movement when the page session is invalidated, firmware support disappears, or the screen leaves composition. Choke and Gas Dose packet state and command admission live in their domain controls.

## Security credentials are write-only and keys remain programmable with immobilizer disabled

Bluetooth name and password fields are edit buffers, not values read back from the ECU. Keep password masking, the write-only hint, and the apply gate (`useBt`, nonempty name, password length 4–8) intact. Desktop permits programming valid iButton keys while the immobilizer is off, so key fields stay editable whenever Security is loaded; a complete 12-character invalid key is an error and partial text sends nothing.

## Unknown ECU selector codes remain visible

Selectors with numeric protocol values must preserve their `Code …` or localized unknown-value fallback when the ECU reports a code outside the current option list. Do not substitute the first valid option or make the unsupported value appear selected in the option dialog.

## Carbur output selectors use zero-based wire codes

Carbur's six numbered universal outputs are wire codes `0` through `5`; the disabled choice is the sentinel `15`. Display them as `1` through `6` and `no`, respectively. Keep unknown incoming codes visible and unchanged until the user explicitly chooses an option. Fuel cutoff is gated by fuel-injection capability; ignition cutoff remains available independently.

## Fuel injection availability gates are independent of field styling

The injector-availability state hides the whole editor, while individual controls can remain disabled based on firmware variant or another parameter (for example, timing when map timing is active, squirts when no options exist, and Lambda STOICH when neither injection nor gas-dose support exists). Shared field styling must carry each existing enabled condition through unchanged.

## Lambda channels and STOICH use separate firmware capabilities

Pass the collected `ParameterCapabilities` snapshot into `LambdaControlScreen`. Known fuel-injection, gas-dose, or CARB_AFR firmware enables all eight Lambda channel checkboxes; STOICH is enabled only for fuel-injection or gas-dose firmware. Unknown firmware keeps both controls disabled. CARB_AFR-only support is meaningful for channel selection and must not be filtered out.

## Universal output function code 15 represents the fifth displayed option

The packet's `15` logic-function code maps to index 4 in the displayed option list. The paired-output setter and each output setter translate the fifth choice back to `15`; other selections retain their displayed index. Unknown codes stay visible as unknown values.

## Reserve-parameter and firmware-CRC flags are read-only during live ECU editing

Keep both flags read-only in the live Security screen. Editing them belongs to offline firmware/EEPROM workflows; the live ECU parameter editor must not expose writes for these flags.

## Firmware options govern dependent parameter controls

The shared domain `ParameterCapabilities` snapshot derives support from the current nullable ECU firmware packet. It is permissive for field previews when firmware is unknown, while optional pages stay hidden except Choke in its fixed base slot. Screens add only predicates that depend on their current parameter values (such as missing teeth or load source); unsupported incoming values remain visible without selecting a replacement. Do not decode the same firmware options again in screens or treat user functionality preferences as ECU capabilities.

## Miscellaneous VSS displays a rounded integer without quantizing the packet

The packet retains the ECU's reciprocal VSS Float so unrelated edits preserve the incoming word. The state and integer editor display a rounded pulses-per-kilometer value, while an explicit editor commit stores the selected integer as a Float.

## Miscellaneous hop angles display rounded integers while retaining fractions

The packet keeps one-thirty-second degree Float values for start angle and duration; state and integer editor initial values round only for presentation. An explicit editor commit stores the selected whole degree as a Float.

## Injection timing retains signed sixteenth-degree values while editors show whole degrees

Both injection timing banks and both cranking timing banks are signed 16-bit values scaled by 1/16 degree. Keep their packet values as Floats through unrelated edits; the integer editor rounds only its initial display value, and an explicit edit stores the chosen whole degree.

## Injector pulses-per-liter rounds for display without quantizing the packet

The ECU's unsigned fixed-point physical value remains a Float so unrelated edits preserve its incoming word. The state and integer editor show a rounded value; an explicit whole-number edit stores that value as a Float.

## Angles ZERO modes follow desktop mutual exclusion

The regular ZERO switch disables the OCTANE correction row while active. Enabling either ZERO mode clears the other on the domain command path, and the control rejects stale OCTANE editor callbacks after ZERO becomes active.

## Temperature fan thresholds are coupled by desktop hysteresis

FAN ON corrects FAN OFF to a 0.25 °C gap only while PWM is off. FAN OFF can correct FAN ON in either mode, and enabling or disabling PWM can correct FAN OFF. An automatic correction outside editor bounds rejects the complete command; receiving a legacy pair or editing an unrelated field does not normalize it. Keep FAN OFF editable while PWM is on, matching the desktop.

## Fuel Injection screen values keep timing and pulses-per-liter raw

The domain values retain signed fractional timing/cranking degrees and fixed-point pulses-per-liter as Floats. The UI enum rounds the displayed integer editor value with `roundToInt`; do not move that rounding into packet state, since negative half-degree wire values use Kotlin's existing display rounding behavior.

## Cold acceleration multiplier retains fractional percentage points

Its integer editor rounds only for display. Preserve the decoded Float through unrelated edits, and store an explicit whole-percent selection as a Float so the packet can retain its fixed-point resolution.

## Acceleration dependent fields follow desktop mode predicates

AE_TIME is shown only for non-pump modes and is enabled only for fuel injection with time mode selected. Xtau rows are shown only when the current wallwet model is nonzero and are enabled only for fuel injection. The domain repeats those live predicates when admitting commands; unknown selector codes remain visible until explicit selection.


## Idling rows keep positive and negative P/I meanings explicit

The existing `P0/I0` resource rows are the positive-error coefficients and `P1/I1` are negative-error coefficients. Firmware and desktop wire index 0 is negative, index 1 positive; preserve this explicit semantic mapping in the route rather than changing the packet codec. IAC_ADD also becomes unavailable when the throttle-assist table is selected, and the domain rechecks that live flag for stale editor callbacks. See [IdlingControl](../../../domain/parameters/IdlingControl.md).

## LTFT controls remain enabled in Off mode when firmware supports fuel injection

Desktop enables the full LTFT field group from the firmware FI option, not the selected Off/Petrol/Gas mode. Keep the four mode labels UI-localized and preserve unknown incoming mode codes until a valid explicit choice.
