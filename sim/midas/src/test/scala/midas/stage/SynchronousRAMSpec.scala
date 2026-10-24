// See LICENSE for license details.

package midas.stage

import java.nio.file.{Files, Paths}
import java.io.PrintWriter

import chisel3._
import chisel3.util._
import chisel3.experimental.annotate
import firrtl.options.TargetDirAnnotation
import firrtl.stage.FirrtlCircuitAnnotation
import midas.{GenerateMultiCycleRamModels, OutputDir, TargetTransforms}
import midas.stage.phases.ConfigParametersAnnotation
import midas.targetutils.{FirrtlMemModelAnnotation, MemModelAnnotation}
import org.scalatest.flatspec.AnyFlatSpec
import scala.sys.process._

/** Exercise module-wide RAM extraction. Chisel/dedup can produce per-instance targets, while LabelSRAMModels consumes
  * module-local memory annotations.
  */
class LocalizeRAMRegressionAnnotations extends firrtl.Transform {
  def inputForm  = firrtl.LowForm
  def outputForm = firrtl.LowForm
  def execute(state: firrtl.CircuitState): firrtl.CircuitState =
    state.copy(annotations = state.annotations.map {
      case FirrtlMemModelAnnotation(target) => FirrtlMemModelAnnotation(target.pathlessTarget)
      case other                            => other
    }.distinct)
}

class SynchronousRAMInner(writeFirst: Boolean, salt: Int) extends Module {
  val io    = IO(new Bundle {
    val cycles    = Output(UInt(32.W))
    val checks    = Output(UInt(32.W))
    val errors    = Output(UInt(32.W))
    val signature = Output(UInt(32.W))
  })
  val cycle = RegInit(0.U(32.W))
  cycle := cycle + 1.U
  val reference     = Reg(Vec(16, UInt(16.W)))
  val memory        = SyncReadMem(16, UInt(16.W), if (writeFirst) SyncReadMem.WriteFirst else SyncReadMem.ReadFirst)
  annotate(MemModelAnnotation(memory))
  val readAddress   = (cycle * 5.U)(3, 0)
  val writeAddress  = Mux(cycle >= 20.U && cycle % 5.U === 0.U, readAddress, cycle(3, 0))
  val readEnable    = cycle >= 20.U && cycle                  % 3.U =/= 0.U
  val writeEnable   = cycle < 16.U || (cycle >= 20.U && cycle % 4.U =/= 0.U)
  val data          = (cycle * 17.U + salt.U)(15, 0)
  when(writeEnable) {
    memory.write(writeAddress, data)
    reference(writeAddress) := data
  }
  val actual        = memory.read(readAddress, readEnable)
  val expectedValue =
    if (writeFirst) Mux(writeEnable && writeAddress === readAddress, data, reference(readAddress))
    else reference(readAddress)
  val expected      = RegEnable(expectedValue, readEnable)
  val valid         = RegNext(readEnable, false.B)
  val checks        = RegInit(0.U(32.W))
  val errors        = RegInit(0.U(32.W))
  val signature     = RegInit(0.U(32.W))
  when(valid) {
    checks    := checks + 1.U
    when(actual =/= expected) { errors := errors + 1.U }
    signature := signature + actual
  }
  io.cycles := cycle
  io.checks    := checks
  io.errors    := errors
  io.signature := signature
}

class SynchronousRAMTarget(writeFirst: Boolean) extends Module {
  val io     = IO(new Bundle {
    val cycles    = Output(UInt(32.W))
    val checks    = Output(UInt(32.W))
    val errors    = Output(UInt(32.W))
    val signature = Output(UInt(32.W))
  })
  val models = (0 until 2).map { i =>
    val model = Module(new SynchronousRAMInner(writeFirst, 100))
    model
  }
  io.cycles := models.head.io.cycles
  io.checks    := models.map(_.io.checks).reduce(_ + _)
  io.errors    := models.map(_.io.errors).reduce(_ + _)
  io.signature := models.map(_.io.signature).reduce(_ + _)
}

class SynchronousRAMSpec extends AnyFlatSpec {
  behavior.of("Golden Gate synchronous RAM models")

  for {
    writeFirst <- Seq(false, true)
    mode       <- Seq("control", "ram-model")
  } {
    it should s"preserve enabled-read latency and read/write collisions with writeFirst=$writeFirst, $mode" in {
      val parent                 = Files.createDirectories(Paths.get("test_run_dir", suiteName))
      val output                 = Files.createTempDirectory(parent, s"$mode-$writeFirst-").toAbsolutePath
      println(s"Synchronous RAM regression output: $output")
      val parameters             = (new firesim.configs.DefaultF1Config).alterPartial {
        case GenerateMultiCycleRamModels => mode != "control"
        case TargetTransforms            => Seq(firrtl.options.Dependency[LocalizeRAMRegressionAnnotations])
        case OutputDir                   => output.toFile
      }
      val (circuit, annotations) = midas.ElaborateChiselSubCircuit(
        new firesim.lib.testutils.PeekPokeHarness(() => new SynchronousRAMTarget(writeFirst))
      )
      val input                  = annotations ++ Seq(
        FirrtlCircuitAnnotation(circuit),
        ConfigParametersAnnotation(parameters),
        TargetDirAnnotation(output.toString),
        OutputBaseFilenameAnnotation("SynchronousRAM"),
      )
      val result                 = new GoldenGateCompilerPhase().transform(AddDerivedAnnotations.transform(input))
      val rtl                    = result.collect { case firrtl.EmittedVerilogCircuitAnnotation(c) => c.value }.mkString
      if (mode == "ram-model") assert(rtl.contains("module RamModel"), "RAM extraction was not applied")
      val source                 = output.resolve("SynchronousRAM.sv")
      Files.writeString(source, rtl)
      val header                 = result.collectFirst { case GoldenGateOutputFileAnnotation(body, ".const.h") => body }.get
      Files.writeString(output.resolve("SynchronousRAM.const.h"), header)
      def register(name: String): String = ("\\." + name + " = ([0-9]+)").r.findFirstMatchIn(header).get.group(1)
      def port(name:     String): String = ("R\"ESC\\(" + name + "\\)ESC\", peek_poke_t::Port\\{\\.address = ([0-9]+)ULL").r
        .findFirstMatchIn(header)
        .get
        .group(1)
      val constants =
        Seq("INIT_DONE", "STEP", "DONE", "PRECISE_PEEKABLE").map(name => s"#define ADDR_$name ${register(name)}") ++
          Seq(
            "RESET"     -> "reset",
            "CYCLES"    -> "io_cycles",
            "CHECKS"    -> "io_checks",
            "ERRORS"    -> "io_errors",
            "SIGNATURE" -> "io_signature",
          ).map { case (name, target) => s"#define ADDR_$name ${port(target)}" }
      Files.writeString(output.resolve("ram_test_addrs.h"), constants.mkString("\n") + "\n")
      val gate      = output.resolve("AbstractClockGate.v")
      Files.copy(Paths.get("AbstractClockGate.v"), gate)
      def run(command: Seq[String], name: String): Int = {
        Files.writeString(output.resolve(s"$name-command.txt"), command.mkString("\n") + "\n")
        val stdout = new PrintWriter(output.resolve(s"$name.stdout").toFile)
        val stderr = new PrintWriter(output.resolve(s"$name.stderr").toFile)
        try Process(command, output.toFile).!(ProcessLogger(stdout.println(_), stderr.println(_)))
        finally { stdout.close(); stderr.close() }
      }
      val obj       = output.resolve("obj")
      val build     = Seq(
        sys.env.getOrElse("VERILATOR", "verilator"),
        "--cc",
        "--exe",
        "--build",
        "-j",
        "2",
        "--assert",
        "-Wno-fatal",
        "--top-module",
        "FPGATop",
        "-Mdir",
        obj.toString,
        "-CFLAGS",
        s"-I$output",
        source.toString,
        gate.toString,
        Paths.get("midas/src/main/verilog/BUFGCE.v").toAbsolutePath.toString,
        Paths.get("midas/src/test/cc/sync_ram_test.cc").toAbsolutePath.toString,
      )
      assert(run(build, "build") == 0, s"Verilator build failed; see $output")
      assert(
        run(Seq(obj.resolve("VFPGATop").toString), "simulate") == 0,
        s"RAM functional regression failed; see $output",
      )
    }
  }
}
