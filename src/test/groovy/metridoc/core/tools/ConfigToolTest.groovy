package metridoc.core.tools

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class ConfigToolTest extends Specification {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    def "test in script adhoc configuration"() {
        when: "an adhoc configuration is created"
        def variable = new ConfigTool().addConfig {
            foo.bar = "foobar"
        }.getVariable("foo.bar")

        then: "the variable can be extracted"
        "foobar" == variable
    }

    def "test from file adhoc configuration"() {
        given: "a configuration file"
        def file = folder.newFile("foobar")
        file.withPrintWriter {
            it.println("foo.bar = \"foobar\"")
        }

        when: "add file to configuration"
        def variable = new ConfigTool().addConfig(file).getVariable("foo.bar")

        then: "the variable can be extracted"
        "foobar" == variable
    }
}
