package OOP.Tests;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExpectedException;
import OOP.Solution.*;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;




public class ExampleTest {

    @Test
    public void testForExample() {

        OOPTestSummary result = OOPUnitCore.runClass(ExampleClass.class);
        assertNotNull(result);
        assertEquals(2, result.getNumSuccesses());
        assertEquals(1, result.getNumFailures());
        assertEquals(0, result.getNumErrors());
        assertEquals(0, result.getNumExceptionMismatches());


    }

    static
    @OOPTestClass(OOPTestClass.OOPTestClassType.ORDERED)
    public class ExampleClass {

        @OOPExceptionRule
        private OOPExpectedException expected = OOPExpectedExceptionImpl.none();

        private int field = 0;

        @OOPBefore({"test1"})
        public void beforeFirstTest() {
            this.field = 123;
        }


        // Should be successful
        @OOPTest(order = 1)
        public void test1() throws OOPAssertionFailure {
            //this must run before the other test. must not throw an exception to succeed
            OOPUnitCore.assertEquals(123, this.field);
        }

        // Should fail
        @OOPTest(order = 2)
        public void test2() throws OOPAssertionFailure {
            OOPUnitCore.assertEquals(321, this.field);
        }

        // Should be successful
        @OOPTest(order = 3)
        public void testThrows() throws Exception {
            expected.expect(Exception.class)
                    .expectMessage("rror messag");
            throw new Exception("error message");
        }
        protected void assertTarget(Class<?> c, ElementType expected) {
            ElementType[] values = c.getAnnotation(Target.class).value();
            Assert.assertEquals(1, values.length);
            Assert.assertEquals(expected, values[0]);
        }

        protected void assertTargetType(Class<?> c) {
            assertTarget(c, ElementType.TYPE);
        }

        protected void assertTargetMethod(Class<?> c) {
            assertTarget(c, ElementType.METHOD);
        }

        protected void assertTargetField(Class<?> c) {
            assertTarget(c, ElementType.FIELD);
        }

        protected void assertRetention(Class<?> c, RetentionPolicy expected) {
            RetentionPolicy actual = c.getAnnotation(Retention.class).value();
            Assert.assertEquals(expected, actual);
        }

        protected void assertRetentionRuntime(Class<?> c) {
            assertRetention(c, RetentionPolicy.RUNTIME);
        }
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected void expectException(Class<? extends Throwable> exception) {
        expectedException.expect(exception);
    }

    protected void runClassTag(Class<?> c, String tag, int successes, int failures, int errors, int mismatches) {
        OOPTestSummary result;
        if (tag == null) {
            result = OOPUnitCore.runClass(c);
        } else {
            result = OOPUnitCore.runClass(c, tag);
        }
        Assert.assertNotNull(result);
        Assert.assertEquals(successes, result.getNumSuccesses());
        Assert.assertEquals(failures, result.getNumFailures());
        Assert.assertEquals(errors, result.getNumErrors());
        Assert.assertEquals(mismatches, result.getNumExceptionMismatches());
    }

    protected void runClass(Class<?> c, int successes, int failures, int errors, int mismatches) {
        runClassTag(c, "", successes, failures, errors, mismatches);
    }

    private int getNumOfTestMethods(Class<?> c, String tag) {
        return (int) Stream.concat(Arrays.stream(c.getMethods()), Arrays.stream(c.getDeclaredMethods()))
                .filter(m ->
                        tag == null ? m.isAnnotationPresent(OOPTest.class) :
                                m.isAnnotationPresent(OOPTest.class) &&
                                        m.getAnnotation(OOPTest.class).tag().equals(tag)

                ).map(Method::getName).distinct().count();
    }

    private int getNumOfTestMethods(Class<?> c) {
        return getNumOfTestMethods(c, null);
    }

    protected void runClassTagAllSuccess(Class<?> c, String tag) {
        runClassTag(c, tag,
                getNumOfTestMethods(c, tag),
                0,
                0,
                0);
    }

    protected void runClassSuccess(Class<?> c, int success) {
        runClass(c,
                success,
                0,
                0,
                0);
    }

    protected void runClassAllSuccess(Class<?> c) {
        runClassTagAllSuccess(c, null);
    }

    protected void runClassAllFail(Class<?> c) {
        runClass(c,
                0,
                getNumOfTestMethods(c),
                0,
                0);
    }

    protected void runClassAllError(Class<?> c) {
        runClass(c,
                0,
                0,
                getNumOfTestMethods(c),
                0);
    }

    protected void runClassAllMismatch(Class<?> c) {
        runClass(c,
                0,
                0,
                0,
                getNumOfTestMethods(c));
    }

    protected void runClassSuccessAndFailure(Class<?> c, int success, int failures) {
        runClass(c,
                success,
                failures,
                0,
                0);
    }

    protected void runClassSuccessAndError(Class<?> c, int success, int errors) {
        runClass(c,
                success,
                0,
                errors,
                0);
    }

    protected void runClassSuccessAndMismatch(Class<?> c, int success, int mismatch) {
        runClassTagSuccessAndMismatch(c, null, success, mismatch);
    }

    protected void runClassTagSuccessAndMismatch(Class<?> c, String tag, int success, int mismatch) {
        runClassTag(c, tag,
                success,
                0,
                0,
                mismatch);
    }

    protected void runClassTagFailAndError(Class<?> c, String tag, int failures, int errors) {
        runClassTag(c, tag,
                0,
                failures,
                errors,
                0);
    }
}

