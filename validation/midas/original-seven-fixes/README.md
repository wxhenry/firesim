# Selected MIDAS validation logs

Prepared locally for the seven proposed review units associated with FireSim issue #1901.
No GitHub publication or hardware access was performed by this collector.

These are selected final runs only. Fixture-development runs are excluded.
Local machine path prefixes were replaced by placeholders; log text is otherwise
preserved. index.json records both original and sanitized file SHA256 hashes.
Source fingerprints, exact commits, dependency versions, commands, stdout/stderr
and test results are included. The evidence indexes also identify locally archived
generated artifacts; this package includes logs, not the generated RTL or binaries.

Results: 71 distinct fixed checks passed across the seven review units. Of the 55
new checks compared against original production code, 35 failed and 20 controls
passed on the original. The additional 16 existing Scala checks ran on the fixed
branch only. The 8 BankConflict traffic checks were also repeated successfully
after the default-mask change and are counted once in the total.

The final combined tip 767c95e659af10dc9c3f25541618c9b40450dc4c was then
retested: 21/21 C++ cases and 50/50 Scala/RTL cases across ten suites passed.
Those final-stack commands, source fingerprints, raw logs and generated-output
evidence index are included separately. The source began clean and its recorded
fingerprints were checked again after completion. These repeat runs are not
counted as additional distinct tests.

These software/compiler/RTL-simulation checks do not constitute FPGA acceptance
of the extracted V80 platform. See each PR description for its validation limits.
