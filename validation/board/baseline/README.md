# Selected physical validation, 2026-10-26

Local publication staging; not uploaded. These records cover three exact successful runs, not the full contribution. The current-source native driver is 90d5e2c3; GCD CAD was generated at 0dbcfcad with exact RTL/header/constraint/build-script equivalence checked before board entry.

- GCD: 64 and 48 produce 16, PASS after 12 target cycles. The qualified controller completed all 17 checks, including JTAG programming, clock measurement and pre/post device/PLM/link health.
- SDK boundary probe: four 4096-byte pages around two adjacent 512 MiB DDR allocations matched in both DMA directions; host mirrors were overwritten between directions.
- Actual current runtime: one 128-byte write at 512 MiB minus 32 bytes was split into 32 and 96 bytes; both DMA readback checks passed. This fixture uses the actual runtime/SDK/allocator/QDMA, but supplies base-class construction and does not execute a target program or bridge stream engine.

Summary hashes identify the tested driver, register map, PDI, VBIN and host module/daemon combination. Binaries, device images, vendor reports, host configurations and raw system logs are omitted. Native/probe output is complete except replacement of local workspace/home path prefixes; log-provenance.json records raw and transformed SHA256 values. Historical evidence remains unchanged locally.

These successful normal operations do not establish physical MMIO fault containment or the root cause of the earlier PCIe loss. Optional stream engines, Rocket, Linux, AutoCounter, AutoILA and Manager require their own matching evidence before publication claims are extended.
