/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.consumer.streaming;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.consumer.EventStream;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test that it is possible to start a stream against a directory,
 *          specified on command line, where multiple processes starts
 *          simultaneously
 * @requires vm.flagless
 * @requires vm.hasJFR
 * @library /test/lib
 * @build jdk.jfr.api.consumer.streaming.Application
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestBaseRepositoryMultipleProcesses
 */
public class TestBaseRepositoryMultipleProcesses {
    public static void main(String... args) throws Exception {
        AtomicBoolean success = new AtomicBoolean();
        Path repository = Path.of("multiple");
        Application app1 = new Application(repository);
        Application app2 = new Application(repository);
        Application app3 = new Application(repository);
        app1.start();
        app2.start();
        app3.start();
        try (EventStream es = EventStream.openRepository(repository)) {
            es.onEvent(e -> {
                success.set(true);
                es.close();
            });
            es.start();
        }
        app1.stop();
        app2.stop();
        app3.stop();
        Asserts.assertTrue(success.get(), "Unable to stream when there are multiple processes");
    }
}
