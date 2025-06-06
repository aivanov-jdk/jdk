/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package ir_framework.tests;

import compiler.lib.ir_framework.Compiler;
import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.shared.TestFormatException;
import jdk.test.lib.Asserts;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * @test
 * @requires vm.debug == true & vm.compiler2.enabled & vm.flagless
 * @summary Test test format violations.
 * @library /test/lib /
 * @run driver ir_framework.tests.TestBadFormat
 */

public class TestBadFormat {

    public static void main(String[] args) {
        checkPreFlagVM();
        expectTestFormatException(BadNoTests.class);
        expectTestFormatException(BadArgumentsAnnotation.class);
        expectTestFormatException(BadOverloadedMethod.class);
        expectTestFormatException(BadCompilerControl.class);
        expectTestFormatException(BadWarmup.class);
        expectTestFormatException(BadBaseTests.class);
        expectTestFormatException(BadRunTests.class);
        expectTestFormatException(BadSetupTest.class);
        expectTestFormatException(BadCheckTest.class);
        expectTestFormatException(BadIRAnnotationBeforeFlagVM.class);
        expectTestFormatException(BadIRAnnotations.class);
        expectTestFormatException(BadIRAnnotationsAfterTestVM.class);
        expectTestFormatException(BadIRNodeForPhase.class);
        expectTestFormatException(BadInnerClassTest.class);
        expectTestFormatException(BadCompileClassInitializer.class, BadCompileClassInitializerHelper1.class,
                                  BadCompileClassInitializerHelper2.class, BadCompileClassInitializerHelper3.class);
    }

    /**
     * Format failures before flag VM
     */
    private static void checkPreFlagVM() {
        try {
            new TestFramework().setDefaultWarmup(-1);
            Asserts.fail("Should have thrown exception");
        } catch(Exception e) {
            assertViolationCount(e, 1);
        }
        try {
            new TestFramework().addScenarios((Scenario)null);
            Asserts.fail("Should have thrown exception");
        } catch(Exception e) {
            assertViolationCount(e, 1);
        }
        try {
            new TestFramework().addScenarios((Scenario[])null);
            Asserts.fail("Should have thrown exception");
        } catch(Exception e) {
            assertViolationCount(e, 1);
        }
        try {
            new TestFramework().addScenarios(new Scenario(1), null);
            Asserts.fail("Should have thrown exception");
        } catch(Exception e) {
            assertViolationCount(e, 1);
        }
        try {
            new TestFramework().addScenarios(new Scenario(1), new Scenario(1), new Scenario(1));
            Asserts.fail("Should have thrown exception");
        } catch(Exception e) {
            assertViolationCount(e, 2);
        }
    }

    private static void assertViolationCount(Exception e, int violationCount) {
        assertTestFormatException(e);
        getViolationCount(e.getMessage());
        Asserts.assertEQ(violationCount, getViolationCount(e.getMessage()));
    }

    private static void assertTestFormatException(Exception e) {
        if (!(e instanceof TestFormatException)) {
            e.printStackTrace();
            Asserts.fail("Unexpected exception", e);
        }
    }

    private static int getViolationCount(String msg) {
        Pattern pattern = Pattern.compile("Violations \\((\\d+)\\)");
        Matcher matcher = pattern.matcher(msg);
        Asserts.assertTrue(matcher.find(), "Could not find violations in" + System.lineSeparator() + msg);
        return Integer.parseInt(matcher.group(1));
    }

    private static void expectTestFormatException(Class<?> clazz, Class<?>... helpers) {
        try {
            if (helpers == null) {
                TestFramework.run(clazz);
            } else {
                new TestFramework(clazz).addHelperClasses(helpers).start();
            }
        } catch (Exception e) {
            checkException(clazz, e, helpers);
            return;
        }
        throw new RuntimeException("Should catch an exception");
    }

    private static void checkException(Class<?> clazz, Exception e, Class<?>[] helpers) {
        assertTestFormatException(e);
        String msg = e.getMessage();
        Violations violations = getViolations(clazz, helpers);
        violations.getFailedMethods().forEach(
                m -> Asserts.assertTrue(msg.contains(m),
                                        "Could not find method \"" + m + "\" in violations" + System.lineSeparator() + msg));
        int violationCount = getViolationCount(msg);
        Asserts.assertEQ(violationCount, violations.getViolationCount(), msg);
    }

    private static Violations getViolations(Class<?> clazz, Class<?>... helpers) {
        Violations violations = new Violations();
        collectViolations(clazz, violations);
        if (helpers != null) {
            Arrays.stream(helpers).forEach(c -> collectViolations(c, violations));
        }
        return violations;
    }

    private static void collectViolations(Class<?> clazz, Violations violations) {
        getViolationsOfClass(clazz, violations);
        for (Class<?> c : clazz.getDeclaredClasses()) {
            getViolationsOfClass(c, violations);
        }
    }

    private static void getViolationsOfClass(Class<?> clazz, Violations violations) {
        ClassFail classFail = clazz.getDeclaredAnnotation(ClassFail.class);
        if (classFail != null) {
            violations.addFail(clazz);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            NoFail noFail = m.getDeclaredAnnotation(NoFail.class);
            if (noFail == null) {
                FailCount failCount = m.getDeclaredAnnotation(FailCount.class);
                if (failCount != null) {
                    violations.addFail(m, failCount.value());
                } else {
                    violations.addFail(m, 1);
                }
            } else {
                // Cannot define both annotation at the same method.
                Asserts.assertEQ(m.getDeclaredAnnotation(FailCount.class), null);
            }
        }
    }

}

// Specify at least one @Test
@ClassFail
class BadNoTests {

}

class BadArgumentsAnnotation {

    @Test
    public void noArgAnnotation(int a) {}

    @FailCount(0) // Combined with both checkNoArgAnnotation2() below
    @Test
    public void noArgAnnotation2(int a) {}

    @Check(test = "noArgAnnotation2")
    public void checkNoArgAnnotation2() {}

    @Test
    @Arguments(values = Argument.DEFAULT)
    public void argNumberMismatch(int a, int b) {}

    @Test
    @Arguments(values = Argument.DEFAULT)
    public void argNumberMismatch2() {}

    @Test
    @Arguments(values = Argument.NUMBER_42)
    public void notBoolean(boolean a) {}

    @Test
    @Arguments(values = Argument.NUMBER_MINUS_42)
    public void notBoolean2(boolean a) {}

    @Test
    @Arguments(values = Argument.TRUE)
    public void notNumber(int a) {}

    @Test
    @Arguments(values = Argument.FALSE)
    public void notNumber2(int a) {}

    @Test
    @Arguments(values = Argument.BOOLEAN_TOGGLE_FIRST_TRUE)
    public void notNumber3(int a) {}

    @Test
    @Arguments(values = Argument.BOOLEAN_TOGGLE_FIRST_FALSE)
    public void notNumber4(int a) {}

    @Test
    @Arguments(values = {Argument.BOOLEAN_TOGGLE_FIRST_FALSE, Argument.TRUE})
    public void notNumber5(boolean a, int b) {}

    @FailCount(2)
    @Test
    @Arguments(values = {Argument.BOOLEAN_TOGGLE_FIRST_FALSE, Argument.NUMBER_42})
    public void notNumber6(int a, boolean b) {}

    @FailCount(2)
    @Test
    @Arguments(values = {Argument.MIN, Argument.MAX})
    public void notNumber7(boolean a, boolean b) {}

    @Test
    @Arguments(values = Argument.DEFAULT)
    public void missingDefaultConstructor(ClassNoDefaultConstructor a) {}

    @Test
    @Arguments(values = Argument.TRUE)
    public void wrongArgumentNumberWithRun(Object o1, Object o2) {
    }

    // Also fails: Cannot use @Arguments together with @Run
    @Run(test="wrongArgumentNumberWithRun")
    public void forRun() {
    }

    @Test
    @Arguments(values = Argument.TRUE)
    public void wrongArgumentNumberWithCheck(Object o1, Object o2) {
    }

    @NoFail
    @Check(test="wrongArgumentNumberWithCheck")
    public void forCheck() {
    }
}

class BadOverloadedMethod {

    @FailCount(0) // Combined with both sameName() below
    @Test
    public void sameName() {}

    @Test
    @Arguments(values = Argument.DEFAULT)
    public void sameName(boolean a) {}

    @Test
    @Arguments(values = Argument.DEFAULT)
    public void sameName(double a) {}
}

class BadCompilerControl {

    @Test
    @DontCompile
    public void test1() {}

    @Test
    @ForceCompile
    public void test2() {}

    @Test
    @DontInline
    public void test3() {}

    @Test
    @ForceInline
    public void test4() {}

    @Test
    @ForceInline
    @ForceCompile
    @DontInline
    @DontCompile
    public void test5() {}

    @DontInline
    @ForceInline
    public void mix1() {}

    @DontCompile
    @ForceCompile
    public void mix2() {}

    @NoFail
    @Test
    public void test6() {}

    @Run(test = "test6")
    @DontCompile
    public void notAtRun() {}

    @NoFail
    @Test
    public void test7() {}

    @Run(test = "test7")
    @ForceCompile
    public void notAtRun2() {}

    @NoFail
    @Test
    public void test8() {}

    @Run(test = "test8")
    @DontInline
    public void notAtRun3() {}

    @NoFail
    @Test
    public void test9() {}

    @Run(test = "test9")
    @ForceInline
    public void notAtRun4() {}

    @NoFail
    @Test
    public void test10() {}

    @Run(test = "test10")
    @ForceInline
    @ForceCompile
    @DontInline
    @DontCompile
    public void notAtRun5() {}

    @NoFail
    @Test
    public void test11() {}

    @Check(test = "test11")
    @DontCompile
    public void notAtCheck() {}

    @NoFail
    @Test
    public void test12() {}

    @Check(test = "test12")
    @ForceCompile
    public void notAtCheck2() {}

    @NoFail
    @Test
    public void test13() {}

    @Check(test = "test13")
    @DontInline
    public void notAtCheck3() {}

    @NoFail
    @Test
    public void test14() {}

    @Check(test = "test14")
    @ForceInline
    public void notAtCheck4() {}

    @NoFail
    @Test
    public void test15() {}

    @Check(test = "test15")
    @ForceInline
    @ForceCompile
    @DontInline
    @DontCompile
    public void notAtCheck5() {}

    @ForceCompile(CompLevel.SKIP)
    public void invalidSkip1() {}

    @ForceCompile(CompLevel.WAIT_FOR_COMPILATION)
    public void invalidWaitForCompilation() {}

    @ForceCompile(CompLevel.C1_SIMPLE)
    @DontCompile(Compiler.C1)
    public void overlappingCompile1() {}

    @ForceCompile(CompLevel.C2)
    @DontCompile(Compiler.C2)
    public void overlappingCompile2() {}

    @ForceCompile(CompLevel.ANY)
    @DontCompile(Compiler.C1)
    public void invalidMix1() {}

    @ForceCompile(CompLevel.ANY)
    @DontCompile(Compiler.C2)
    public void invalidMix2() {}

    @ForceCompile(CompLevel.ANY)
    @DontCompile
    public void invalidMix3() {}
}

class BadWarmup {

    @Warmup(10000)
    public void warmUpNonTest() {}

    @Test
    @Warmup(1)
    public void someTest() {}

    @FailCount(0) // Combined with someTest()
    @Run(test = "someTest")
    @Warmup(1)
    public void twoWarmups() {}

    @Test
    @Warmup(-1)
    public void negativeWarmup() {}

    @NoFail
    @Test
    public void someTest2() {}

    @Run(test = "someTest2")
    @Warmup(-1)
    public void negativeWarmup2() {}

    @NoFail
    @Test
    public void someTest3() {}

    @FailCount(2) // Negative warmup and invoke once
    @Run(test = "someTest3", mode = RunMode.STANDALONE)
    @Warmup(-1)
    public void noWarmupAtStandalone() {}

    @Test(compLevel = CompLevel.C1_SIMPLE)
    public void testNoCompLevelStandalone() {}

    @Test(compLevel = CompLevel.WAIT_FOR_COMPILATION)
    public void testNoCompLevelStandalone2() {}

    @NoFail
    @Test
    public void someTest4() {}

    @FailCount(0) // Negative warmup and invoke once
    @Run(test = {"someTest4", "testNoCompLevelStandalone", "testNoCompLevelStandalone2"}, mode = RunMode.STANDALONE)
    public void runNoCompLevelStandalone() {}
}

class BadBaseTests {
    @Test
    @Arguments(values = Argument.DEFAULT)
    @FailCount(3) // No default constructor + parameter + return
    public TestInfo cannotUseTestInfoAsParameterOrReturn(TestInfo info) {
        return null;
    }

    @Test
    @Arguments(values = Argument.DEFAULT)
    @FailCount(3) // No default constructor + parameter + return
    public RunInfo cannotUseRunInfoAsParameterOrReturn(RunInfo info) {
        return null;
    }

    @Test
    @Arguments(values = Argument.DEFAULT)
    @FailCount(3) // No default constructor + parameter + return
    public AbstractInfo cannotUseAbstractInfoAsParameterOrReturn(AbstractInfo info) {
        return null;
    }
}

class BadRunTests {
    @Run(test = "runForRun2")
    public void runForRun() {}

    @Run(test = "runForRun")
    public void runForRun2() {}

    @Test
    public void sharedByTwo() {}

    @FailCount(0) // Combined with sharedByTwo()
    @Run(test = "sharedByTwo")
    public void share1() {}

    @FailCount(0) // Combined with sharedByTwo()
    @Run(test = "sharedByTwo")
    public void share2() {}

    @Run(test = "doesNotExist")
    public void noTestExists() {}

    @Test
    @Arguments(values = Argument.DEFAULT)
    public void argTest(int x) {}

    @FailCount(0) // Combined with argTest()
    @Run(test = "argTest")
    public void noArgumentAnnotationForRun() {}

    @NoFail
    @Test
    public void test1() {}

    @Run(test = "test1")
    public void wrongParameters1(int x) {}

    @NoFail
    @Test
    public void test2() {}

    @Run(test = "test2")
    public void wrongParameters(RunInfo info, int x) {}

    @Test
    public void invalidShare() {}

    @FailCount(0) // Combined with invalidShare()
    @Run(test = "invalidShare")
    public void shareSameTestTwice1() {}

    @FailCount(0) // Combined with invalidShare()
    @Run(test = "invalidShare")
    public void shareSameTestTwice2() {}

    @Test
    public void invalidShareCheckRun() {}

    @FailCount(0) // Combined with invalidShare()
    @Run(test = "invalidShareCheckRun")
    public void invalidShareCheckRun1() {}

    @FailCount(0) // Combined with invalidShare()
    @Check(test = "invalidShareCheckRun")
    public void invalidShareCheckRun2() {}

    @NoFail
    @Test
    public void testInvalidRunWithArgAnnotation() {}

    @Arguments(values = Argument.DEFAULT)
    @Run(test = "testInvalidRunWithArgAnnotation")
    public void invalidRunWithArgAnnotation(RunInfo info) {}

    @NoFail
    @Test
    public void testRunWithTestInfo() {}

    @Run(test = "testRunWithTestInfo")
    public void invalidRunWithTestInfo(TestInfo info) {}

    @Run(test = {})
    public void invalidRunWithNoTest() {}

    @Run(test = "")
    public void invalidRunWithEmptyTestName() {}

    @NoFail
    @Test
    public void someExistingTest() {}

    @FailCount(2)
    @Run(test = {"unknown1", "someExistingTest", "unknown2"})
    public void invalidRunWithInvalidTests() {}

    @NoFail
    @Test
    public void testInvalidReuse() {}

    @Test
    public void testInvalidReuse2() {}

    @NoFail
    @Test
    public void testInvalidReuse3() {}

    @FailCount(0)
    @Run(test = {"testInvalidReuse", "testInvalidReuse2"})
    public void runInvalidReuse1() {}

    @FailCount(0)
    @Run(test = {"testInvalidReuse2", "testInvalidReuse3"})
    public void runInvalidReuse2() {}
}

class BadSetupTest {
    // ----------- Bad Combinations of Annotations -----------------
    @Setup
    @Test
    public Object[] badSetupTestAnnotation() {
      return new Object[]{1, 2, 3};
    }

    @NoFail
    @Test
    public void testForBadSetupCheckAnnotation() {}

    @Setup
    @Check(test = "testForBadSetupCheckAnnotation")
    public void badSetupCheckAnnotation() {}

    @Setup
    @Arguments(values = {Argument.NUMBER_42, Argument.NUMBER_42})
    public void badSetupArgumentsAnnotation(int a, int b) {}

    @NoFail
    @Test
    public void testForBadSetupRunAnnotation() {}

    @Setup
    @Run(test = "testForBadSetupRunAnnotation")
    public void badSetupRunAnnotation() {}

    // ----------- Useless but ok: Setup Without Test Method -----
    @NoFail
    @Setup
    public void setupWithNoTest() {}

    // ----------- Bad: Test where Setup Method does not exist ---
    @Test
    @Arguments(setup = "nonExistingMethod")
    public void testWithNonExistingMethod() {}

    // ----------- Bad Arguments Annotation ----------------------
    @NoFail
    @Setup
    public Object[] setupForTestSetupAndValues() {
        return new Object[]{1, 2};
    }

    @Test
    @Arguments(setup = "setupForTestSetupAndValues",
               values = {Argument.NUMBER_42, Argument.NUMBER_42})
    public void testSetupAndValues(int a, int b) {}

    // ----------- Overloaded Setup Method ----------------------
    @NoFail
    @Setup
    Object[] setupOverloaded() {
        return new Object[]{3, 2, 1};
    }

    @Setup
    Object[] setupOverloaded(SetupInfo info) {
        return new Object[]{1, 2, 3};
    }

    @NoFail
    @Test
    @Arguments(setup = "setupOverloaded")
    void testOverloaded(int a, int b, int c) {}
}

class BadCheckTest {
    @Check(test = "checkForCheck2")
    public void checkForCheck() {}

    @Check(test = "checkForCheck")
    public void checkForCheck2() {}

    @Test
    public void sharedByTwo() {}

    @FailCount(0) // Combined with sharedByTwo()
    @Check(test = "sharedByTwo")
    public void share1() {}

    @FailCount(0) // Combined with sharedByTwo()
    @Check(test = "sharedByTwo")
    public void share2() {}

    @Check(test = "doesNotExist")
    public void noTestExists() {}

    @NoFail
    @Test
    public void test1() {}

    @Check(test = "test1")
    public void wrongReturnParameter1(int x) {}

    @NoFail
    @Test
    public short test2() {
        return 3;
    }

    @Check(test = "test2")
    public void wrongReturnParameter2(int x) {}

    @NoFail
    @Test
    public short test3() {
        return 3;
    }

    @Check(test = "test3")
    public void wrongReturnParameter3(String x) {}

    @NoFail
    @Test
    public short test4() {
        return 3;
    }

    @Check(test = "test4")
    public void wrongReturnParameter4(TestInfo info, int x) {} // Must flip parameters

    @NoFail
    @Test
    public int test5() {
        return 3;
    }

    @Check(test = "test5")
    public void wrongReturnParameter5(short x, TestInfo info) {}

    @Test
    public void invalidShare() {}

    @FailCount(0) // Combined with invalidShare()
    @Check(test = "invalidShare")
    public void shareSameTestTwice1() {}

    @FailCount(0) // Combined with invalidShare()
    @Check(test = "invalidShare")
    public void shareSameTestTwice2() {}

    @NoFail
    @Test
    public void testInvalidRunWithArgAnnotation() {}

    @Arguments(values = Argument.DEFAULT)
    @Check(test = "testInvalidRunWithArgAnnotation")
    public void invalidRunWithArgAnnotation(TestInfo info) {}
}


class BadIRAnnotationBeforeFlagVM {

    @Test
    @IR(failOn = IRNode.CALL, phase = {})
    public void emptyCompilePhases() {}
}

class BadIRAnnotations {
    @IR(failOn = IRNode.CALL)
    public void noIRAtNonTest() {}

    @IR(failOn = IRNode.CALL)
    @IR(failOn = IRNode.CALL)
    public void noIRAtNonTest2() {}

    @NoFail
    @Test
    public void test() {}

    @Run(test = "test")
    @IR(failOn = IRNode.CALL)
    public void noIRAtRun() {}

    @NoFail
    @Test
    public void test2() {}

    @Run(test = "test2")
    @IR(failOn = IRNode.CALL)
    @IR(failOn = IRNode.CALL)
    public void noIRAtRun2() {}

    @NoFail
    @Test
    public void test3() {}

    @Check(test = "test3")
    @IR(failOn = IRNode.CALL)
    public void noIRAtCheck() {}

    @NoFail
    @Test
    public void test4() {}

    @Check(test = "test4")
    @IR(failOn = IRNode.CALL)
    @IR(failOn = IRNode.CALL)
    public void noIRAtCheck2() {}

    @Test
    @IR
    public void mustSpecifyAtLeastOneConstraint() {
    }

    @FailCount(2)
    @Test
    @IR
    @IR
    public void mustSpecifyAtLeastOneConstraint2() {
    }

    @Test
    @IR(applyIf = {"TLABRefillWasteFraction", "50"})
    public void mustSpecifyAtLeastOneConstraint3() {
    }

    @FailCount(3)
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "50"}, applyIfNot = {"UseTLAB", "true"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "50", "UseTLAB", "true"},
        applyIfOr = {"TLABRefillWasteFraction", "50", "UseTLAB", "true"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "50"}, applyIfNot = {"TLABRefillWasteFraction", "50"},
        applyIfAnd = {"TLABRefillWasteFraction", "50", "UseTLAB", "true"},
        applyIfOr = {"TLABRefillWasteFraction", "50", "UseTLAB", "true"})
    public void onlyOneApply() {}

    @FailCount(3)
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "50", "UseTLAB", "true"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "51", "UseTLAB"})
    public void applyIfTooManyFlags() {}

    @FailCount(2)
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction"})
    @IR(failOn = IRNode.CALL, applyIf = {"Bla"})
    public void applyIfMissingValue() {}

    @FailCount(2)
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"PrintIdealGraphFilee", "true"})
    @IR(failOn = IRNode.CALL, applyIf = {"Bla", "foo"})
    public void applyIfUnknownFlag() {}

    @FailCount(5)
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"PrintIdealGraphFile", ""})
    @IR(failOn = IRNode.CALL, applyIf = {"UseTLAB", ""})
    @IR(failOn = IRNode.CALL, applyIf = {"", "true"})
    @IR(failOn = IRNode.CALL, applyIf = {"", ""})
    @IR(failOn = IRNode.CALL, applyIf = {" ", " "})
    public void applyIfEmptyValue() {}

    @FailCount(5)
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "! 34"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "!== 34"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "<<= 34"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "=<34"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "<"})
    public void applyIfFaultyComparator() {}

    @FailCount(3)
    @Test
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction", "50", "UseTLAB", "true"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction", "50", "UseTLAB"})
    public void applyIfNotTooManyFlags() {}

    @FailCount(2)
    @Test
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"Bla"})
    public void applyIfNotMissingValue() {}

    @FailCount(2)
    @Test
    @IR(failOn = IRNode.CALL, applyIfNot = {"PrintIdealGraphFilee", "true"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"Bla", "foo"})
    public void applyIfNotUnknownFlag() {}

    @FailCount(5)
    @Test
    @IR(failOn = IRNode.CALL, applyIfNot = {"PrintIdealGraphFile", ""})
    @IR(failOn = IRNode.CALL, applyIfNot = {"UseTLAB", ""})
    @IR(failOn = IRNode.CALL, applyIfNot = {"", "true"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"", ""})
    @IR(failOn = IRNode.CALL, applyIfNot = {" ", " "})
    public void applyIfNotEmptyValue() {}

    @FailCount(5)
    @Test
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction", "! 34"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction", "!== 34"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction", "<<= 34"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction", "=<34"})
    @IR(failOn = IRNode.CALL, applyIfNot = {"TLABRefillWasteFraction", "<"})
    public void applyIfNotFaultyComparator() {}


    @FailCount(2)
    @Test
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "50"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "51", "UseTLAB"})
    public void applyIfAndNotEnoughFlags() {}

    @FailCount(5)
    @Test
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "51", "UseTLAB"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"Bla"})
    public void applyIfAndMissingValue() {}

    @FailCount(3)
    @Test
    @IR(failOn = IRNode.CALL, applyIfAnd = {"PrintIdealGraphFilee", "true", "TLABRefillWasteFraction", "< 34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "!= 50", "Bla", "bla", "Bla2", "bla2"})
    public void applyIfAndUnknownFlag() {}

    @FailCount(18)
    @Test
    @IR(failOn = IRNode.CALL, applyIfAnd = {"PrintIdealGraphFile", ""})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"PrintIdealGraphFile", "", "PrintIdealGraphFile", ""})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"UseTLAB", ""})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"UseTLAB", "", "UseTLAB", ""})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"", "true"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"", "true", "", "true"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"", ""})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"", "", "", ""})
    @IR(failOn = IRNode.CALL, applyIfAnd = {" ", " ", " ", " "})
    public void applyIfAndEmptyValue() {}

    @FailCount(20)
    @Test
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "! 34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "! 34", "TLABRefillWasteFraction", "! 34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "!== 34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "!== 34", "TLABRefillWasteFraction", "=== 34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "<<= 34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "<<= 34", "TLABRefillWasteFraction", ">>= 34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "=<34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "=<34", "TLABRefillWasteFraction", "=<34"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "<"})
    @IR(failOn = IRNode.CALL, applyIfAnd = {"TLABRefillWasteFraction", "<", "TLABRefillWasteFraction", "!="})
    public void applyIfAndFaultyComparator() {}

    @FailCount(2)
    @Test
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "50"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "51", "UseTLAB"})
    public void applyIfOrNotEnoughFlags() {}

    @FailCount(5)
    @Test
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "51", "UseTLAB"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"Bla"})
    public void applyIfOrMissingValue() {}

    @FailCount(3)
    @Test
    @IR(failOn = IRNode.CALL, applyIfOr = {"PrintIdealGraphFilee", "true", "TLABRefillWasteFraction", "< 34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "!= 50", "Bla", "bla", "Bla2", "bla2"})
    public void applyIfOrUnknownFlag() {}

    @FailCount(18)
    @Test
    @IR(failOn = IRNode.CALL, applyIfOr = {"PrintIdealGraphFile", ""})
    @IR(failOn = IRNode.CALL, applyIfOr = {"PrintIdealGraphFile", "", "PrintIdealGraphFile", ""})
    @IR(failOn = IRNode.CALL, applyIfOr = {"UseTLAB", ""})
    @IR(failOn = IRNode.CALL, applyIfOr = {"UseTLAB", "", "UseTLAB", ""})
    @IR(failOn = IRNode.CALL, applyIfOr = {"", "true"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"", "true", "", "true"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"", ""})
    @IR(failOn = IRNode.CALL, applyIfOr = {"", "", "", ""})
    @IR(failOn = IRNode.CALL, applyIfOr = {" ", " ", " ", " "})
    public void applyIfOrEmptyValue() {}

    @FailCount(20)
    @Test
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "! 34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "! 34", "TLABRefillWasteFraction", "! 34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "!== 34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "!== 34", "TLABRefillWasteFraction", "=== 34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "<<= 34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "<<= 34", "TLABRefillWasteFraction", ">>= 34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "=<34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "=<34", "TLABRefillWasteFraction", "=<34"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "<"})
    @IR(failOn = IRNode.CALL, applyIfOr = {"TLABRefillWasteFraction", "<", "TLABRefillWasteFraction", "!="})
    public void applyIfOrFaultyComparator() {}


    @Test
    @FailCount(3)
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "true"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "SomeString"})
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "48"}) // valid
    @IR(failOn = IRNode.CALL, applyIf = {"TLABRefillWasteFraction", "48.5"})
    public void wrongFlagValueLongFlag() {}

    @Test
    @FailCount(3)
    @IR(failOn = IRNode.CALL, applyIf = {"UseTLAB", "true"}) // valid
    @IR(failOn = IRNode.CALL, applyIf = {"UseTLAB", "SomeString"})
    @IR(failOn = IRNode.CALL, applyIf = {"UseTLAB", "48"})
    @IR(failOn = IRNode.CALL, applyIf = {"UseTLAB", "48.5"})
    public void wrongFlagValueBooleanFlag() {}

    @Test
    @FailCount(2)
    @IR(failOn = IRNode.CALL, applyIf = {"CompileThresholdScaling", "true"})
    @IR(failOn = IRNode.CALL, applyIf = {"CompileThresholdScaling", "SomeString"})
    @IR(failOn = IRNode.CALL, applyIf = {"CompileThresholdScaling", "48"}) // valid
    @IR(failOn = IRNode.CALL, applyIf = {"CompileThresholdScaling", "48.5"}) // valid
    public void wrongFlagValueDoubleFlag() {}

    @Test
    @NoFail
    @IR(failOn = IRNode.CALL, applyIf = {"ErrorFile", "true"}) // valid
    @IR(failOn = IRNode.CALL, applyIf = {"ErrorFile", "SomeString"}) // valid
    @IR(failOn = IRNode.CALL, applyIf = {"ErrorFile", "48"}) // valid
    @IR(failOn = IRNode.CALL, applyIf = {"ErrorFile", "48.5"}) // valid
    public void anyValueForStringFlags() {}
}

class BadIRAnnotationsAfterTestVM {
    @Test
    @FailCount(4)
    @IR(failOn = {IRNode.STORE_OF_CLASS, ""})
    @IR(failOn = {IRNode.STORE_OF_CLASS, "", IRNode.LOAD_B_OF_CLASS, ""})
    @IR(counts = {IRNode.STORE_OF_CLASS, "", "3"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "", "3", IRNode.LOAD_B_OF_CLASS, "", "3"})
    public void emtpyUserProvidedPostfix() {}

    @Test
    @FailCount(2)
    @IR(counts = {IRNode.STORE})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo"})
    public void missingCountString() {}

    @Test
    @FailCount(51)
    @IR(counts = {IRNode.STORE, IRNode.STORE})
    @IR(counts = {IRNode.STORE, IRNode.STORE, IRNode.STORE, IRNode.STORE})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", IRNode.STORE})
    @IR(counts = {IRNode.STORE, ""})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", ""})
    @IR(counts = {IRNode.STORE, "<"})
    @IR(counts = {IRNode.STORE, "!"})
    @IR(counts = {IRNode.STORE, "!3"})
    @IR(counts = {IRNode.STORE, "=="})
    @IR(counts = {IRNode.STORE, "-45"})
    @IR(counts = {IRNode.STORE, "3.0"})
    @IR(counts = {IRNode.STORE, "a3"})
    @IR(counts = {IRNode.STORE, "0x1"})
    @IR(counts = {IRNode.STORE, ">-45"})
    @IR(counts = {IRNode.STORE, ">3.0"})
    @IR(counts = {IRNode.STORE, ">a3"})
    @IR(counts = {IRNode.STORE, ">0x1"})
    @IR(counts = {IRNode.STORE, "> -45"})
    @IR(counts = {IRNode.STORE, "> 3.0"})
    @IR(counts = {IRNode.STORE, "> a3"})
    @IR(counts = {IRNode.STORE, "> 0x1"})
    @IR(counts = {IRNode.STORE, " > -45"})
    @IR(counts = {IRNode.STORE, " > 3.0"})
    @IR(counts = {IRNode.STORE, " > a3"})
    @IR(counts = {IRNode.STORE, " > 0x1"})
    @IR(counts = {IRNode.STORE, "!= 1000"})
    @IR(counts = {IRNode.STORE, "< 0"})
    @IR(counts = {IRNode.STORE, "< 1"})
    @IR(counts = {IRNode.STORE, "<= 0"})
    @IR(counts = {IRNode.STORE, "> -1"})
    @IR(counts = {IRNode.STORE, ">= 0"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "<"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "!"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "!3"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "=="})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "-45"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "3.0"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "a3"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "0x1"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", ">-45"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", ">3.0"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", ">a3"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", ">0x1"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "> -45"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "> 3.0"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "> a3"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", "> 0x1"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", " > -45"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", " > 3.0"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", " > a3"})
    @IR(counts = {IRNode.STORE_OF_CLASS, "Foo", " > 0x1"})
    public void wrongCountString() {}

    @Test
    @FailCount(8)
    @IR(counts = {IRNode.LOAD_VECTOR_I, "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE_MAX, "> 0"}) // valid
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE_ANY, "> 0"}) // valid
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "", "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "xxx", "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "min()", "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "min(max_for_type)", "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "2,4,8,16,32,64,max_int", "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "2,4,8,16,32,64,-3", "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "min(max_for_type, xxx)", "> 0"})
    @IR(counts = {IRNode.LOAD_VECTOR_I, IRNode.VECTOR_SIZE + "min(max_for_type, min(max_for_type, max_for_type))", "> 0"})
    public int[] badVectorNodeSize() {
        int[] a = new int[1024*8];
        for (int i = 0; i < a.length; i++) {
            a[i]++;
        }
        return a;
    }
}

class BadIRNodeForPhase {
    @Test
    @FailCount(5)
    @IR(failOn = IRNode.CHECKCAST_ARRAY, phase = CompilePhase.AFTER_PARSING)
    @IR(failOn = IRNode.CHECKCAST_ARRAY, phase = CompilePhase.OPTIMIZE_FINISHED)
    @IR(failOn = IRNode.CHECKCAST_ARRAY, phase = CompilePhase.PRINT_IDEAL)
    @IR(failOn = IRNode.CHECKCAST_ARRAY, phase = CompilePhase.PRINT_OPTO_ASSEMBLY) // works
    @IR(failOn = IRNode.FIELD_ACCESS, phase = CompilePhase.FINAL_CODE)
    @IR(failOn = IRNode.FIELD_ACCESS, phase = {CompilePhase.PRINT_OPTO_ASSEMBLY, CompilePhase.DEFAULT}) // works
    @IR(failOn = IRNode.FIELD_ACCESS, phase = {CompilePhase.PRINT_OPTO_ASSEMBLY, CompilePhase.DEFAULT, CompilePhase.PRINT_IDEAL})
    public void machNode() {}

    @Test
    @FailCount(2)
    @IR(failOn = IRNode.STORE, phase = {CompilePhase.AFTER_PARSING, CompilePhase.AFTER_PARSING})
    @IR(counts = {IRNode.STORE, "0"}, phase = {CompilePhase.AFTER_PARSING, CompilePhase.DEFAULT, CompilePhase.AFTER_PARSING})
    public void duplicatedPhase() {}

    @Test
    @FailCount(6)
    @IR(failOn = IRNode.ALLOC, phase = {CompilePhase.FINAL_CODE, CompilePhase.AFTER_MACRO_EXPANSION})  // FINAL_CODE not available
    @IR(failOn = IRNode.ALLOC, phase = CompilePhase.PRINT_IDEAL)  // PRINT_IDEAL not available
    @IR(failOn = IRNode.ALLOC, phase = {CompilePhase.ITER_GVN1, CompilePhase.AFTER_PARSING})  // works
    @IR(failOn = IRNode.ALLOC, phase = {CompilePhase.ITER_GVN1, CompilePhase.AFTER_PARSING,
                                        CompilePhase.PRINT_OPTO_ASSEMBLY})  // PRINT_OPTO_ASSEMBLY not available
    @IR(failOn = IRNode.ALLOC_ARRAY, phase = {CompilePhase.FINAL_CODE, CompilePhase.AFTER_MACRO_EXPANSION})  // FINAL_CODE not available
    @IR(failOn = IRNode.ALLOC_ARRAY, phase = CompilePhase.PRINT_IDEAL)  // PRINT_IDEAL not available
    @IR(failOn = IRNode.ALLOC_ARRAY, phase = {CompilePhase.ITER_GVN1, CompilePhase.AFTER_PARSING})  // works
    @IR(failOn = IRNode.ALLOC_ARRAY, phase = {CompilePhase.ITER_GVN1, CompilePhase.AFTER_PARSING,
                                        CompilePhase.PRINT_OPTO_ASSEMBLY})  // PRINT_OPTO_ASSEMBLY not available
    public void alloc() {}

    @Test
    @FailCount(9)
    @IR(failOn = IRNode.LOOP, phase = CompilePhase.BEFORE_BEAUTIFY_LOOPS)
    @IR(failOn = IRNode.COUNTED_LOOP, phase = CompilePhase.BEFORE_BEAUTIFY_LOOPS)
    @IR(failOn = IRNode.COUNTED_LOOP_MAIN, phase = CompilePhase.BEFORE_BEAUTIFY_LOOPS)
    @IR(failOn = IRNode.LOOP, phase = {CompilePhase.FINAL_CODE, CompilePhase.BEFORE_BEAUTIFY_LOOPS})
    @IR(failOn = IRNode.COUNTED_LOOP, phase = {CompilePhase.FINAL_CODE, CompilePhase.BEFORE_BEAUTIFY_LOOPS})
    @IR(failOn = IRNode.COUNTED_LOOP_MAIN, phase = {CompilePhase.FINAL_CODE, CompilePhase.BEFORE_BEAUTIFY_LOOPS})
    @IR(failOn = IRNode.LOOP, phase = {CompilePhase.OPTIMIZE_FINISHED, CompilePhase.BEFORE_BEAUTIFY_LOOPS})
    @IR(failOn = IRNode.COUNTED_LOOP, phase = {CompilePhase.OPTIMIZE_FINISHED, CompilePhase.BEFORE_BEAUTIFY_LOOPS})
    @IR(failOn = IRNode.COUNTED_LOOP_MAIN, phase = {CompilePhase.OPTIMIZE_FINISHED, CompilePhase.BEFORE_BEAUTIFY_LOOPS})
    @IR(failOn = IRNode.LOOP, phase = CompilePhase.FINAL_CODE) // works
    @IR(failOn = IRNode.COUNTED_LOOP, phase = CompilePhase.FINAL_CODE) // works
    @IR(failOn = IRNode.COUNTED_LOOP_MAIN, phase = CompilePhase.FINAL_CODE) // works
    public void loops() {}

    @Test
    @FailCount(6)
    @IR(failOn = IRNode.LOAD_VECTOR_I, phase = CompilePhase.BEFORE_REMOVEUSELESS) // works
    @IR(failOn = IRNode.STORE_VECTOR, phase = CompilePhase.BEFORE_REMOVEUSELESS) // works
    @IR(failOn = IRNode.VECTOR_CAST_B2I, phase = CompilePhase.BEFORE_REMOVEUSELESS) // works
    @IR(failOn = IRNode.LOAD_VECTOR_I, phase = CompilePhase.BEFORE_MATCHING) // works
    @IR(failOn = IRNode.STORE_VECTOR, phase = CompilePhase.BEFORE_MATCHING) // works
    @IR(failOn = IRNode.VECTOR_CAST_B2I, phase = CompilePhase.BEFORE_MATCHING) // works
    @IR(failOn = IRNode.LOAD_VECTOR_I, phase = {CompilePhase.MATCHING, CompilePhase.MATCHING})
    @IR(failOn = IRNode.STORE_VECTOR, phase = {CompilePhase.MATCHING, CompilePhase.MATCHING})
    @IR(failOn = IRNode.VECTOR_CAST_B2I, phase = {CompilePhase.MATCHING, CompilePhase.MATCHING})
    @IR(failOn = IRNode.LOAD_VECTOR_I, phase = CompilePhase.FINAL_CODE)
    @IR(failOn = IRNode.STORE_VECTOR, phase = CompilePhase.FINAL_CODE)
    @IR(failOn = IRNode.VECTOR_CAST_B2I, phase = CompilePhase.FINAL_CODE)
    public void vector() {}

    @Test
    @IR(failOn = "notAnIRNode")
    public void noDefaultSpecified() {}

    @Test
    @IR(failOn = IRNode.COUNTED_LOOP, phase = CompilePhase.BEFORE_REMOVEUSELESS)
    public void noRegexSpecifiedForPhase() {}

    @Test
    @FailCount(2)
    @IR(failOn = "_#asdf#_", phase = CompilePhase.BEFORE_REMOVEUSELESS)
    @IR(failOn = "_#asdf#_")
    public void noIRNodeMapping() {}

}

@ClassFail
class BadInnerClassTest {

    class InnerClass {
        @Test
        public void noTestInInnerClass1() {}

        @Test
        public void noTestInInnerClass2() {}

        @Check(test = "noTestInInnerClass2")
        public void checkNoTestInInnerClass2() {}

        @Test
        public void noTestInInnerClass3() {}

        @Run(test = "noTestInInnerClass3")
        public void checkNoTestInInnerClass3() {}
    }


    static class StaticInnerClass {
        @Test
        public void noTestInInnerStaticClass1() {}

        @Test
        public void noTestInStaticInnerClass2() {}

        @Check(test = "noTestInStaticInnerClass2")
        public void checkNoTestInStaticInnerClass2() {}

        @Test
        public void noTestInStaticInnerClass3() {}

        @Run(test = "noTestInStaticInnerClass3")
        public void checkNoTestInStaticInnerClass3() {}
    }
}

@ForceCompileClassInitializer
class BadCompileClassInitializer {
    static int iFld = 3;

    @Test
    @ForceCompileClassInitializer
    public void test() {}

    @ForceCompileClassInitializer
    public void helper() {}
}

@ClassFail
@ForceCompileClassInitializer(CompLevel.SKIP)
class BadCompileClassInitializerHelper1 {

}

@ClassFail
@ForceCompileClassInitializer(CompLevel.WAIT_FOR_COMPILATION)
class BadCompileClassInitializerHelper2 {

}

@ClassFail
@ForceCompileClassInitializer
class BadCompileClassInitializerHelper3 {
    // no <clinit>
}

class ClassNoDefaultConstructor {
    private ClassNoDefaultConstructor(int i) {
    }
}

// Test specific annotation:
// All methods without such an annotation must occur in the violation messages.
@Retention(RetentionPolicy.RUNTIME)
@interface NoFail {}

// Test specific annotation:
// Specify a fail count for a method without @NoFail. Use the value 0 if multiple methods are part of the same violation.
@Retention(RetentionPolicy.RUNTIME)
@interface FailCount {
    int value();
}

// Class specific annotation:
// All classes with such an annotation have exactly one violation with the class name in it.
@Retention(RetentionPolicy.RUNTIME)
@interface ClassFail {}

class Violations {
    private final List<String> failedMethods = new ArrayList<>();
    private int violations;

    public int getViolationCount() {
        return violations;
    }

    public List<String> getFailedMethods() {
        return failedMethods;
    }

    public void addFail(Method m, int count) {
        failedMethods.add(m.getName());
        violations += count;
    }

    public void addFail(Class<?> c) {
        failedMethods.add(c.getName());
        violations += 1;
    }
}
