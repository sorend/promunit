package promunit.runner


import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import org.zeroturnaround.exec.stream.LogOutputStream
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
        Map res = runPromTool(promtoolBinary.absolutePath, "check", "rules", rule.absolutePath)
        writeTestResult("CHECK", rule, res.rc, res.stdout, res.stderr)
        res.rc
    }

    int runTest(File test) {
        Map res = runPromTool(promtoolBinary.absolutePath, "test", "rules", test.absolutePath)
        writeTestResult("TEST", test, res.rc, res.stdout, res.stderr)
        res.rc
    }

    private void writeTestResult(String prefix, File test, int rc, String stdout, String stderr) {
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
                'system-out' { mkp.yieldUnescaped("<![CDATA[${stdout}]]>") }
                'system-err' { mkp.yieldUnescaped("<![CDATA[${stderr}]]>") }
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

    private static Map runPromTool(String... args) {
        StringBuilder stderr = new StringBuilder()
        ProcessResult result = new ProcessExecutor().exitValueAny()
                .command(args)
                .redirectOutput(Slf4jStream.of(log).asInfo())
                .redirectError(new LogOutputStream() {
                    @Override
                    protected void processLine(String line) {
                        stderr.append(line).append("\n")
                        log.warn(line)
                    }
                })
                .readOutput(true)
                .execute()
        int rc = result.exitValue
        String output = result.outputUTF8()
        [rc: rc, stdout: output, stderr: stderr.toString()]
    }

    private static String hostname() {
        System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME")
    }
}
