## Hold controls must cancel and flush independently of their visual layout

Choke and gas-dose manual controls issue repeated ECU step commands while pressed and periodic flush callbacks while held. Keep their pointer gesture jobs, release/cancel callbacks, and `isActive` gates together; moving them into shared page or section surfaces must not replace the gesture handler with a click control.

## Security credentials are write-only and may only be applied when valid

Bluetooth name and password fields are edit buffers, not values read back from the ECU. Keep password masking, the write-only hint, and the apply gate (`useBt`, nonempty name, password length 4–8) intact. IButton keys remain editable only when the immobilizer is enabled and a complete 12-character invalid key is an error.

## Unknown ECU selector codes remain visible

Selectors with numeric protocol values must preserve their `Code …` or localized unknown-value fallback when the ECU reports a code outside the current option list. Do not substitute the first valid option or make the unsupported value appear selected in the option dialog.

## Fuel injection availability gates are independent of field styling

The injector-availability state hides the whole editor, while individual controls can remain disabled based on firmware variant or another parameter (for example, timing when map timing is active, squirts when no options exist, and Lambda fields when the injector feature is unavailable). Shared field styling must carry each existing enabled condition through unchanged.

## Universal output function code 15 represents the fifth displayed option

The packet's `15` logic-function code maps to index 4 in the displayed option list. The paired-output setter and each output setter translate the fifth choice back to `15`; other selections retain their displayed index. Unknown codes stay visible as unknown values.

## Reserve-parameter and firmware-CRC flags are read-only in the live manager

The desktop PMParamsController disables both via EnableUseResParCheck(false). Editable versions belong to its offline firmware/EEPROM workflows; the Android live Security screen must not expose writes just because the offline desktop form has checkboxes.

## Firmware options govern dependent parameter controls

ParameterCapabilities maps ECU flags using the live desktop controller rules. Its permissive default supports loading and standalone previews; connected routes pass actual firmware options. Local predicates still apply (load-grid/source, missing teeth, injection table mode), and unsupported incoming values remain visible without selecting a replacement. Do not apply desktop-only user functionality preferences as ECU capabilities.

## Miscellaneous VSS displays a rounded integer without quantizing the packet

The packet retains the ECU's reciprocal VSS Float so unrelated edits preserve the incoming word. The state and integer editor display a rounded pulses-per-kilometer value, while an explicit editor commit stores the selected integer as a Float.

## Miscellaneous hop angles display rounded integers while retaining fractions

The packet keeps one-thirty-second degree Float values for start angle and duration; state and integer editor initial values round only for presentation. An explicit editor commit stores the selected whole degree as a Float.
