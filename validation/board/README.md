# Current SLASH V80 physical validation

Selected complete native stdout/stderr, acceptance records and actual ILA CSVs. Path prefixes alone are replaced; provenance binds original and sanitized bytes. No source, generated HDL, vendor images/checkpoints, host configuration or system logs are included.

GCD, Rocket ISA/Linux, three target-to-host Printf stream modes and AutoCounter passed the matching physical checks. The zero-DDR MMIO native driver includes the constructor repair; other workload natives retain the earlier frozen source. Original failure records are retained separately. Complete native ISA logs and the aggregate controller result are included. Printf outputs and CPU references retain all synthesized lines; acceptance compares the multiset of SYNTHESIZED_PRINT lines, without claiming an ordering across independent print streams.

Manager actually launched/completed the ISA add through unchanged tasks/Fabric/SSH. Its collected uartlog contains successful qualified-run JSON; native output is retained separately. The original outer classifier incorrectly required native PASS text in uartlog. HW_CFG_SUMMARY is generated locally; an extra remote-copy configuration caused rsync 23. This is not a claim of warning-free collection or live native console forwarding.

AutoILA used a recorded private HSDP validation extension. Two 1024-point captures show initial (64,48) and final (16,16). Public launcher automatic capture is not implemented. The first audit stopped on a circular PLM log false rejection. The corrected decoder uses captured RTCA ring metadata; the continuation verified unchanged ring/metadata/SSIT/status evidence and reused the existing programmed image. It did not reprogram.

The Printf results qualify target-to-host output only. Host-to-target input has CPU regression evidence, not matching physical stream qualification; no guest NIC, additional memory-model, multi-card or static-v3 isolation claim is made. Software deadlines do not establish fault containment.
