# Final publication checks and source mapping

The branch-assembly record maps exact tested boundaries to published commits. Final deltas are two Scala whitespace alignments, unused imports, an unused test lambda identifier, RST layout and the guide paragraph pointing to current physical results. There is no native or FPGA logic change. The affected compiler/synchronous-RAM/MMIO regression was rerun: all 11 cases in three suites pass. These overlap previous suite counts and are not added as distinct cases.

The final r3 record passes Scala production/test scalafix, Scala formatting, RST formatting, Sphinx HTML build and affected RTL regressions. Manager black formatting passed in the initial style check, and the final independent Manager mypy check passes.

Earlier failed formatting/rule checks are retained. The initial documentation build stopped on a GitHub URL for an unpublished local branch. The successful local build sets the repository-supported SKIP_URL_CHECK=1; it checks document rendering, not remote URL existence. Four Sphinx warnings refer to unchanged existing pages. Text transcript whitespace/terminal line endings are intentionally preserved; the evidence directory disables whitespace lint only for these log files.
