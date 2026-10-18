## Unknown firmware keeps raw channels available during previews

Null capability values mean the firmware is not known yet and remain permissive so standalone previews do not hide channels or claim that they are unsupported. Once firmware is known, SECU3T hides ADD_I3 and ADD_I5–ADD_I8; ADD_I4 remains visible but is unavailable without TPIC8101, and ADD_I5–ADD_I8 remain visible but unavailable without ATMEGA1284 on a non-SECU3T unit.
