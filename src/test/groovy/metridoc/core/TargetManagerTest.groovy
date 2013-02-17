package metridoc.core

import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 2/16/13
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
class TargetManagerTest {

    def targetManager = new TargetManager()

    @Test
    void "when a job is interrupted it should throw an exception"() {
        targetManager.interrupt()

        try {
            targetManager.profile("do something") {

            }
            assert false: "exception should have occurred"
        } catch (JobInterruptionException e) {
        }
    }

    @Test
    void "test general functionality of including and using targets"() {
        targetManager.includeTargets(MetridocJobTestTargetHelper)
        targetManager.defaultTarget = "bar"
        targetManager.runDefaultTarget()
        assert targetManager.binding.fooRan
        assert targetManager.targetsRan.contains("foo")
        assert targetManager.targetsRan.contains("bar")
    }

    class MetridocJobTestTargetHelper extends Script {

        @Override
        Object run() {
            fooRan = false
            target(foo: "runs foo") {
                fooRan = true
                assert targetManager instanceof TargetManager
            }

            target(bar: "runs bar") {
                depends("foo")
            }
        }
    }
}