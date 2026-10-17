## BL, DE, and TACH encode direction separately from driven state

Each special output uses the lower bit for state and the next higher bit for direction: BL uses state/direction bits 11/12, DE 13/14, and TACH 23/24. The pairs `00`, `10`, and `11` encode Hi-Z, LOW, and HIGH respectively when written direction:state. Boolean output commands always enable direction and set the state; the explicit output-enabled properties clear both bits when disabled. Other output bits remain independent.
