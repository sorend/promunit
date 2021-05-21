
# promunit -- simple wrapper for promtool test to convert prometheus tests to junit output

At [my workplace](https://bankdata.dk/) we use Prometheus a lot, and promtool is part of our automation toolbox for
providing self-service alerting to developer teams.

This promunit is a simple wrapper around promtool check rules (validation of alert rules) and promtool test rules
(evaluation of prometheus alert unittests).

## Installation

Download from jitpack. Latest version is [![Release](https://jitpack.io/v/sorend/promunit.svg)](https://jitpack.io/#sorend/promunit).

```bash
$ export PROMUNIT_VERSION=0.1
$ curl -sLo promunit.jar https://jitpack.io/com/github/sorend/promunit/${PROMUNIT_VERSION}/promunit-${PROMUNIT_VERSION}-all.jar
```


## Usage

```bash
$ java -jar promunit.jar --help
Missing required options: '--tests-dir=<path>', '--rules-dir=<path>', '--output-dir=<path>'
Usage: promunit -o=<path> [-P=<path>] -r=<path> -t=<path>
Prometheus unit tests to JUnit output
  -o, --output-dir=<path>   Where to output junit test files
  -P, --promtool=<path>     Where to find promtool (default on path)
  -r, --rules-dir=<path>    Path to unit prometheus rule ymls
  -t, --tests-dir=<path>    Path to unit prometheus test ymls
```

Example:
```bash
$ java -jar promunit.jar -r ./rules -t ./tests -o ./out -P ./promtool
$ cat ./out/TEST-test.yml.xml
<testsuite name='test.yml' tests='1' skipped='0' failures='0' errors='0' hostname=''>
  <properties />
  <testcase name='test.yml' classname='test.yml'>
    <system-out>Unit Testing:  /tmp/promunit13357663905500429500/test.yml
  SUCCESS

</system-out>
    <system-err />
  </testcase>
</testsuite>
```
