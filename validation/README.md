# SLASH V80 contribution verification, 2026-10-26

This separate evidence branch accompanies the five source boundaries on the contributor fork. It is not a source PR.

- [MIDAS](midas/README.md): original-code failures, passing controls and exact first-boundary CPU/RTL logs, as requested in [the maintainer reply](https://github.com/firesim/firesim/issues/1901#issuecomment-6348532515).
- [Current boundaries](current-boundaries/README.md): exact runtime, platform and Manager test stdout/stderr, commands, source fingerprints and result records.
- [Physical trials](board/README.md): complete selected native logs, output/reference streams, acceptance records and actual ILA CSVs, including retained failures and corrected acceptance boundaries.

The source series builds on official main 9db588176c19e7252ed3027b2028bc21095f863a. Individual physical natives use the frozen integrated source described in the source platform VALIDATION.md; the pure-MMIO native includes the subsequent zero-DDR constructor repair. The final source tree preserves those tested production paths, including the audited circular PLM log decoder. Boundary commits are recorded under current-boundaries.

Current physical results cover GCD, three Rocket ISA tests, Linux boot/private-disk I/O/HTIF poweroff, three target-to-host Printf modes, AutoCounter, Manager launch/completion/collection and the AutoILA hardware/debug path. AutoILA capture used a private host extension; the public launcher has no automatic capture interface. Host-to-target streams have CPU evidence, not matching physical stream qualification. No guest NIC, other memory-model, multi-card or static-v3 isolation qualification is claimed. Historical PCIe fault causation and fault containment remain unresolved.

Text-only logs are selected and local paths sanitized; provenance and manifests bind both versions. Vendor images/checkpoints/generated RTL, SDK sources, private host configuration, credentials and raw system logs are excluded. Historical public_upload=false fields describe local collection, not current branch visibility.

[Final checks and published-source mapping](final-checks/README.md) record the lint-only deltas and affected regression rerun.
