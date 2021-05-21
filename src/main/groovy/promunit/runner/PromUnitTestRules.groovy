package promunit.runner

import groovy.transform.builder.Builder
import groovy.xml.MarkupBuilder
import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult

class PromUnitTestRules {

    File testsDir
    File rulesDir
    File outputDir
    File promtoolBinary

    String hostname = hostname()

    void runTests() {
        File workDir = File.createTempDir("promunit")
        try {
            List<File> tests = testsDir.listFiles().findAll { it.name.endsWith(".yml") }
            List<File> rules = rulesDir.listFiles().findAll { it.name.endsWith(".yml") }

            List<File> preparedTests = tests.collect { prepareTest(it, rules, workDir) }

            preparedTests.each { runTest(it) }
        }
        finally {
            workDir.deleteDir()
        }
    }

    void runTest(File test) {
        ProcessResult result = new ProcessExecutor().exitValueAny()
                .command(promtoolBinary.getAbsolutePath(), "test", "rules", test.getAbsolutePath())
                .readOutput(true)
                .execute()
        int rc = result.exitValue
        String output = result.outputUTF8()

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

        new File(outputDir, "TEST-${test.name}.xml").text = writer.toString()
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
