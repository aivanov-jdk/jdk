/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import static jdk.internal.util.OperatingSystem.LINUX;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.LinuxHelper;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.TKit;

/**
 * Test --license-file parameter. Output of the test should be commonlicensetest*.*
 * package bundle. The output package should provide the same functionality as
 * the default package and also incorporate license information from
 * test/jdk/tools/jpackage/resources/license.txt file from OpenJDK repo.
 *
 * deb:
 *
 * Package should install license file /opt/commonlicensetest/share/doc/copyright
 * file.
 *
 * rpm:
 *
 * Package should install license file in
 * %{_defaultlicensedir}/licensetest-1.0/license.txt file.
 *
 * Mac:
 *
 * Windows
 *
 * Installer should display license text matching contents of the license file
 * during installation.
 */

/*
 * @test
 * @summary jpackage with --license-file
 * @library /test/jdk/tools/jpackage/helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @compile -Xlint:all -Werror LicenseTest.java
 * @requires (jpackage.test.SQETest != null)
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=LicenseTest.testCommon
 */

/*
 * @test
 * @summary jpackage with --license-file
 * @library /test/jdk/tools/jpackage/helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @compile -Xlint:all -Werror LicenseTest.java
 * @requires (jpackage.test.SQETest == null)
 * @run main/othervm/timeout=1440 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=LicenseTest
 */

public class LicenseTest {

    @Test
    public static void testCommon() {
        PackageTest test = new PackageTest().configureHelloApp()
        .addInitializer(cmd -> {
            cmd.addArguments("--license-file", TKit.createRelativePathCopy(
                    LICENSE_FILE));
        });

        initLinuxLicenseVerifier(test.forTypes(PackageType.LINUX));

        test.run();
    }

    @Test(ifOS = LINUX)
    public static void testLinuxLicenseInUsrTree() {
        testLinuxLicenseInUsrTree("/usr");
    }

    @Test(ifOS = LINUX)
    public static void testLinuxLicenseInUsrTree2() {
        testLinuxLicenseInUsrTree("/usr/local");
    }

    @Test(ifOS = LINUX)
    public static void testLinuxLicenseInUsrTree3() {
        testLinuxLicenseInUsrTree("/usr/foo");
    }

    @Test(ifOS = LINUX)
    public static void testLinuxLicenseInUsrTree4() {
        testLinuxLicenseInUsrTree("/usrbuz");
    }

    @Test(ifOS = LINUX)
    public static void testCustomDebianCopyright() {
        new CustomDebianCopyrightTest().run();
    }

    @Test(ifOS = LINUX)
    public static void testCustomDebianCopyrightSubst() {
        new CustomDebianCopyrightTest().withSubstitution(true).run();
    }

    private static PackageTest initLinuxLicenseVerifier(PackageTest test) {
        return test
        .addBundleVerifier(cmd -> {
            verifyLicenseFileInLinuxPackage(cmd, linuxLicenseFile(cmd));
        })
        .addInstallVerifier(cmd -> {
            verifyLicenseFileInstalledLinux(cmd);
        })
        .addUninstallVerifier(cmd -> {
            verifyLicenseFileNotInstalledLinux(linuxLicenseFile(cmd));
        });
    }

    private static void testLinuxLicenseInUsrTree(String installDir) {
        PackageTest test = new PackageTest()
        .forTypes(PackageType.LINUX)
        .configureHelloApp()
        .addInitializer(cmd -> {
            cmd.setFakeRuntime();
            cmd.addArguments("--license-file", TKit.createRelativePathCopy(
                    LICENSE_FILE));
            cmd.addArguments("--install-dir", installDir);
        });

        initLinuxLicenseVerifier(test);

        test.run();
    }

    private static Path rpmLicenseFile(JPackageCommand cmd) {
        final Path licenseRoot = Path.of(
                new Executor()
                .setExecutable("rpm")
                .addArguments("--eval", "%{_defaultlicensedir}")
                .executeAndGetFirstLineOfOutput());

        final Path licensePath = licenseRoot.resolve(String.format("%s-%s",
                LinuxHelper.getPackageName(cmd), cmd.version())).resolve(
                LICENSE_FILE.getFileName());

        return licensePath;
    }

    private static Path debLicenseFile(JPackageCommand cmd) {
        Path installDir = cmd.appInstallationDirectory();

        if (installDir.equals(Path.of("/")) || installDir.startsWith("/usr")) {
            // Package is in '/usr' tree
            return Path.of("/usr/share/doc/", LinuxHelper.getPackageName(cmd),
                    "copyright");
        }

        return installDir.resolve("share/doc/copyright");
    }

    private static Path linuxLicenseFile(JPackageCommand cmd) {
        cmd.verifyIsOfType(PackageType.LINUX);
        final Path licenseFile;
        switch (cmd.packageType()) {
            case LINUX_DEB:
                licenseFile = debLicenseFile(cmd);
                break;

            case LINUX_RPM:
                licenseFile = rpmLicenseFile(cmd);
                break;

            default:
                throw new IllegalArgumentException();
        }

        return cmd.pathToUnpackedPackageFile(licenseFile);
    }

    private static void verifyLicenseFileInLinuxPackage(JPackageCommand cmd,
            Path expectedLicensePath) {
        TKit.assertTrue(LinuxHelper.getPackageFiles(cmd).filter(path -> path.equals(
                expectedLicensePath)).findFirst().orElse(null) != null,
                String.format("Check license file [%s] is in %s package",
                        expectedLicensePath, LinuxHelper.getPackageName(cmd)));
    }

    private static void verifyLicenseFileInstalledRpm(Path licenseFile) throws
            IOException {
        TKit.assertStringListEquals(Files.readAllLines(LICENSE_FILE),
                Files.readAllLines(licenseFile), String.format(
                "Check contents of package license file [%s] are the same as contents of source license file [%s]",
                licenseFile, LICENSE_FILE));
    }

    private static void verifyLicenseFileInstalledDebian(Path licenseFile)
            throws IOException {

        List<String> actualLines = Files.readAllLines(licenseFile).stream().dropWhile(
                line -> !line.startsWith("License:")).collect(
                        Collectors.toList());
        // Remove leading `License:` followed by the whitespace from the first text line.
        actualLines.set(0, actualLines.get(0).split("\\s+", 2)[1]);

        actualLines = DEBIAN_COPYRIGT_FILE_STRIPPER.apply(actualLines);

        TKit.assertNotEquals(0, String.join("\n", actualLines).length(),
                "Check stripped license text is not empty");

        TKit.assertStringListEquals(DEBIAN_COPYRIGT_FILE_STRIPPER.apply(
                Files.readAllLines(LICENSE_FILE)), actualLines, String.format(
                "Check subset of package license file [%s] is a match of the source license file [%s]",
                licenseFile, LICENSE_FILE));
    }

    private static void verifyLicenseFileInstalledLinux(JPackageCommand cmd)
            throws IOException {

        final Path licenseFile = linuxLicenseFile(cmd);
        TKit.assertReadableFileExists(licenseFile);

        switch (cmd.packageType()) {
            case LINUX_DEB:
                verifyLicenseFileInstalledDebian(licenseFile);
                break;

            case LINUX_RPM:
                verifyLicenseFileInstalledRpm(licenseFile);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    private static void verifyLicenseFileNotInstalledLinux(Path licenseFile) {
        TKit.assertPathExists(licenseFile.getParent(), false);
    }

    private static class CustomDebianCopyrightTest {
        CustomDebianCopyrightTest() {
            withSubstitution(false);
        }

        private List<String> licenseFileText(String copyright, String licenseText) {
            List<String> lines = new ArrayList<>(List.of(
                    String.format("Copyright=%s", copyright),
                    "Foo",
                    "Bar",
                    "Buz"));
            lines.addAll(List.of(licenseText.split("\\R", -1)));
            return lines;
        }

        private List<String> licenseFileText() {
            if (withSubstitution) {
                return licenseFileText("APPLICATION_COPYRIGHT",
                        "APPLICATION_LICENSE_TEXT");
            } else {
                return expectedLicenseFileText();
            }
        }

        private List<String> expectedLicenseFileText() {
            return licenseFileText(copyright, licenseText);
        }

        CustomDebianCopyrightTest withSubstitution(boolean v) {
            withSubstitution = v;
            // Different values just to make easy to figure out from the test log which test was executed.
            if (v) {
                copyright = "Duke (C)";
                licenseText = "The quick brown fox\n jumps over the lazy dog";
            } else {
                copyright = "Java (C)";
                licenseText = "How vexingly quick daft zebras jump!";
            }
            return this;
        }

        void run() {
            final Path srcLicenseFile = TKit.workDir().resolve("license");
            new PackageTest().forTypes(PackageType.LINUX_DEB).configureHelloApp()
            .addInitializer(cmd -> {
                // Create source license file.
                Files.write(srcLicenseFile, List.of(
                        licenseText.split("\\R", -1)));

                cmd.setFakeRuntime();
                cmd.setArgumentValue("--name", String.format("%s%s",
                        withSubstitution ? "CustomDebianCopyrightWithSubst" : "CustomDebianCopyright",
                        cmd.name()));
                cmd.addArguments("--license-file", srcLicenseFile);
                cmd.addArguments("--copyright", copyright);
                cmd.addArguments("--resource-dir", RESOURCE_DIR);

                // Create copyright template file in a resource dir.
                Files.createDirectories(RESOURCE_DIR);
                Files.write(RESOURCE_DIR.resolve("copyright"),
                        licenseFileText());
            })
            .addInstallVerifier(cmd -> {
                Path installedLicenseFile = linuxLicenseFile(cmd);
                TKit.assertStringListEquals(expectedLicenseFileText(),
                        DEBIAN_COPYRIGT_FILE_STRIPPER.apply(Files.readAllLines(
                                installedLicenseFile)), String.format(
                                "Check contents of package license file [%s] are the same as contents of source license file [%s]",
                                installedLicenseFile, srcLicenseFile));
            })
            .run();
        }

        private boolean withSubstitution;
        private String copyright;
        private String licenseText;

        private final Path RESOURCE_DIR = TKit.workDir().resolve("resources");
    }

    private static final Path LICENSE_FILE = TKit.TEST_SRC_ROOT.resolve(
            Path.of("resources", "license.txt"));

    private static final Function<List<String>, List<String>> DEBIAN_COPYRIGT_FILE_STRIPPER = (lines) -> Arrays.asList(
            String.join("\n", lines).stripTrailing().split("\n"));
}
