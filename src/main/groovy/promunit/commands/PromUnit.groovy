package promunit.commands

import picocli.CommandLine
import promunit.runner.PromUnitTestRules

@CommandLine.Command(name="promunit", description="Prometheus unit tests to JUnit output")
class PromUnit implements Runnable {

    @CommandLine.Option(names = ["-t", "--tests-dir"], paramLabel = "<path>", description = "Path to unit prometheus test ymls", required = true)
    File testsDir

    @CommandLine.Option(names = ["-r", "--rules-dir"], paramLabel = "<path>", description = "Path to unit prometheus rule ymls", required = true)
    File rulesDir

    @CommandLine.Option(names = ["-o", "--output-dir"], paramLabel = "<path>", description = "Where to output junit test files", required = true)
    File outputDir

    @CommandLine.Option(names = ["-P", "--promtool"], paramLabel = "<path>", description = "Where to find promtool (default on path)", defaultValue = "promtool")
    File promtoolBinary

    @Override
    void run() {
        int rcSum = new PromUnitTestRules(testsDir: testsDir, rulesDir: rulesDir, outputDir: outputDir, promtoolBinary: promtoolBinary).runTests()
        if (rcSum > 0)
            throw new AssertionError("There are failed tests")
    }
}
