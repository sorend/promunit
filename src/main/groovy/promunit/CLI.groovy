package promunit

import picocli.CommandLine
import promunit.commands.PromUnit

class CLI {
    static void main(String[] args) {
        int rc = new CommandLine(new PromUnit()).execute(args)
        System.exit(rc)
    }
}
