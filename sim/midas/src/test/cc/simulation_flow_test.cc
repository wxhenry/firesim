// See LICENSE for license details.

#include "bridges/clock.h"
#include "bridges/master.h"
#include "core/bridge_driver.h"
#include "core/config.h"
#include "core/simulation.h"

#include <iostream>
#include <stdexcept>

namespace {
const TargetConfig config{};

class RegisterDevice : public simif_t {
public:
  RegisterDevice() : simif_t(config) {}
  uint32_t read(size_t address) override {
    if (address == 4)
      return 0x46697265;
    if (address == 0 || address == 16 || address == 28)
      return 1;
    return 0;
  }
  void write(size_t, uint32_t) override {}
};

class FinalizingBridge : public bridge_driver_t {
public:
  static char KIND;
  FinalizingBridge(simif_t &device, bool terminates, int code)
      : bridge_driver_t(device, &KIND), terminates(terminates), code(code) {}
  void finish() override { finished = true; }
  bool terminate() override { return finished && terminates; }
  int exit_code() override { return finished ? code : 0; }
  bool finished = false;

private:
  bool terminates;
  int code;
};
char FinalizingBridge::KIND;

class Simulation : public simulation_t {
public:
  Simulation(widget_registry_t &registry, int code, bool timeout)
      : simulation_t(registry, {}), code(code), timeout(timeout) {}
  int simulation_run() override { return code; }
  bool simulation_timed_out() override { return timeout; }

private:
  int code;
  bool timeout;
};
} // namespace

int main(int argc, char **argv) {
  try {
    if (argc != 2)
      throw std::runtime_error("expected one case name");
    const std::string name = argv[1];
    int run_code = 0, first_code = 0, second_code = 0, expected = 0;
    bool terminates = true, timeout = false;
    if (name == "late-failure") {
      first_code = expected = 123;
    } else if (name == "early-failure") {
      run_code = expected = 17;
      first_code = 123;
    } else if (name == "multiple-failures") {
      first_code = expected = 123;
      second_code = 124;
    } else if (name == "success-before-failure") {
      second_code = expected = 124;
    } else if (name == "nonterminating") {
      terminates = false;
      first_code = 123;
    } else if (name == "timeout") {
      timeout = true;
      expected = EXIT_FAILURE;
    } else if (name != "success") {
      throw std::runtime_error("unknown case: " + name);
    }
    RegisterDevice device;
    widget_registry_t registry;
    registry.add_widget(
        new clockmodule_t(device, {16, 20, 24, 28, 32, 36}, 0, {}));
    registry.add_widget(new master_t(device, {0, 4, 8}, 0, {}));
    auto *first = new FinalizingBridge(device, terminates, first_code);
    auto *second = new FinalizingBridge(device, true, second_code);
    registry.add_widget(first);
    registry.add_widget(second);
    Simulation simulation(registry, run_code, timeout);
    const int actual = simulation.execute_simulation_flow();
    if (!first->finished || !second->finished)
      throw std::runtime_error("not all bridges were finalized");
    if (actual != expected)
      throw std::runtime_error("expected exit code " +
                               std::to_string(expected) + ", got " +
                               std::to_string(actual));
    return 0;
  } catch (const std::exception &error) {
    std::cerr << error.what() << std::endl;
    return 1;
  }
}
