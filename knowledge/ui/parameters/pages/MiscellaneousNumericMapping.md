## EVAP slope must keep both model and wire start pairs valid

An EVAP edit keeps the entered start in the packet, but the ECU stores it in 32-RxL counts. Choose the slope with the larger of the typed and rounded wire starts as its reference, then validate the decoded end and minimum delta against both starts. Otherwise a typed pair at the upper range can decode 16 RxL above the 2,000,000 limit after the start rounds upward.
