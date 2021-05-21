package promunit.runner

import spock.lang.Specification

class PromUnitTestRulesTest extends Specification {

    File rulesDir
    File testsDir
    File workDir

    void setup() {
        testsDir = File.createTempDir()
        rulesDir = File.createTempDir()
        workDir = File.createTempDir()
    }

    void cleanup() {
        testsDir.deleteDir()
        rulesDir.deleteDir()
        workDir.deleteDir()
    }

    def "test we can prepare test"() {
        given:
        new File(testsDir, "test.yml").text = PromUnitTestRulesTest.getResource("/test.yml").text
        new File(rulesDir, "alerts.yml").text = PromUnitTestRulesTest.getResource("/alerts.yml").text
        PromUnitTestRules sut = new PromUnitTestRules(testsDir: testsDir, rulesDir: rulesDir)

        when:
        sut.prepareTest(new File(testsDir, "test.yml"), [new File(rulesDir, "alerts.yml")], workDir)

        then:
        true
    }

    def "test we can prepare test when rule file not found"() {
        given:
        new File(testsDir, "test.yml").text = PromUnitTestRulesTest.getResource("/test.yml").text
        new File(rulesDir, "alertsx.yml").text = PromUnitTestRulesTest.getResource("/alerts.yml").text
        PromUnitTestRules sut = new PromUnitTestRules(testsDir: testsDir, rulesDir: rulesDir)

        when:
        sut.prepareTest(new File(testsDir, "test.yml"), rulesDir.listFiles() as List<File>, workDir)

        then:
        true
    }

}
