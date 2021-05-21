package promunit.runner


import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

@Slf4j
class PromUnitTestRules {

    File testsDir
    File rulesDir
    File outputDir
    File promtoolBinary

    String hostname = hostname()

    int runTests() {
        File workDir = File.createTempDir("promunit")
        try {
            List<File> tests = testsDir.listFiles().findAll { it.name.endsWith(".yml") }
            List<File> rules = rulesDir.listFiles().findAll { it.name.endsWith(".yml") }

            // prepare testss
            List<File> preparedTests = tests.collect { prepareTest(it, rules, workDir) }

            log.info("Rules {}", rules)
            log.info("Tests {}", tests)

            // check rules + run tests
            return (rules.collect { runCheck(it) }.sum() ?: 0) +
                    (preparedTests.collect { runTest(it) }.sum() ?: 0)
        }
        finally {
            workDir.deleteDir()
        }
    }

    int runCheck(File rule) {
        ProcessResult result = new ProcessExecutor().exitValueAny()
                .command(promtoolBinary.getAbsolutePath(), "check", "rules", rule.getAbsolutePath())
                .redirectOutput(Slf4jStream.of(log).asInfo())
                .redirectError(Slf4jStream.of(log).asWarn())
                .readOutput(true)
                .execute()
        int rc = result.exitValue
        String output = result.outputUTF8()

        writeTestResult("CHECK", rule, rc, output)

        rc
    }

    int runTest(File test) {
        ProcessResult result = new ProcessExecutor().exitValueAny()
                .command(promtoolBinary.getAbsolutePath(), "test", "rules", test.getAbsolutePath())
                .redirectOutput(Slf4jStream.of(log).asInfo())
                .redirectError(Slf4jStream.of(log).asWarn())
                .readOutput(true)
                .execute()
        int rc = result.exitValue
        String output = result.outputUTF8()

        writeTestResult("TEST", test, rc, output)

        rc
    }

    void writeTestResult(String prefix, File test, int rc, String output) {
        boolean isError = rc > 1
        boolean isFailure = rc == 1

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.'testsuite'(name: test.name, tests: 1, skipped: 0, failures: isFailure ? 1 : 0, errors: isError ? 1 : 0, hostname: hostname) {
            'properties'()
            'testcase'(name: test.name, classname: test.name) {
                if (isError) {
                    'error'(message: "ERROR", type: "promtool")
                }
                if (isFailure) {
                    'failure'(message: "FAILED", type: "promtool")
                }
                'system-out'(output)
                'system-err'()
            }
        }

        new File(outputDir, "${prefix}-${test.name}.xml").text = writer.toString()
    }

    static File prepareTest(File test, List<File> rules, File workDir) {
        def yaml = new YamlSlurper().parse(test)
        List<String> mappedRuleFiles = yaml.rule_files.collect { String path ->
            def file = new File(path)
            def found = rules.find { it.name == file.name }
            found?.absolutePath ?: path
        }
        yaml.rule_files = mappedRuleFiles
        def builder = new YamlBuilder()
        builder(yaml)
        File res = new File(workDir, test.name)
        res.text = builder.toString()
        res
    }

    private static String hostname() {
        System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME")
    }
}
