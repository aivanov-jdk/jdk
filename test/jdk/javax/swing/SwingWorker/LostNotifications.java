/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/*
 * @test
 * @bug 8302778
 * @summary Verifies if SwingWorker delivers all the notifications
 * @run main LostNotifications
 */
public final class LostNotifications
        extends SwingWorker<String, String>
        implements PropertyChangeListener, Thread.UncaughtExceptionHandler {

    private static final AtomicBoolean doneInvoked = new AtomicBoolean();
    private static final AtomicBoolean startedNotified = new AtomicBoolean();
    private static final AtomicBoolean doneNotified = new AtomicBoolean();

    private static final CountDownLatch notificationLatch = new CountDownLatch(3);

    public static void main(String[] args)
            throws ExecutionException, InterruptedException, InvocationTargetException {
        final LostNotifications worker = new LostNotifications();
        worker.addPropertyChangeListener(worker);
        SwingUtilities.invokeAndWait(()
                -> Thread.currentThread().setUncaughtExceptionHandler(worker));

        worker.execute();
        System.out.println("Worker result: " + worker.get());

        if (!notificationLatch.await(3, TimeUnit.SECONDS)) {
            System.out.println(startedNotified.get());
            System.out.println(doneInvoked.get());
            System.out.println(doneNotified.get());
            throw new RuntimeException("Timed out: not all notifications delivered");
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() == StateValue.STARTED) {
            System.out.println("STARTED state");
            startedNotified.set(true);
            notificationLatch.countDown();
            throw new RuntimeException("Unexpected exception");
        }

        if (evt.getNewValue() == StateValue.DONE) {
            System.out.println("DONE state");
            doneNotified.set(true);
            notificationLatch.countDown();
        }
    }

    @Override
    protected void done() {
        System.out.println("done() method");
        doneInvoked.set(true);
        notificationLatch.countDown();
    }

    @Override
    protected String doInBackground() {
        // Return the result quickly
        try {
            return "finished";
        } finally {
            System.out.println("doInBackground finished");
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.out.println("EDT uncaughtException: " + t.getName()
                           + " - " + e.getMessage());
    }
}
