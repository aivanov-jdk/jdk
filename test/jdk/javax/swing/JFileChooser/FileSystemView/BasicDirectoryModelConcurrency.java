import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.basic.BasicDirectoryModel;

/*
 * @test
 * @bug 8323670 8307091 8240690
 * @requires os.family == "mac" | os.family == "linux"
 * @summary Verifies thread-safety of BasicDirectoryModel (JFileChooser)
 */
public final class BasicDirectoryModelConcurrency extends ThreadGroup {
    /** Initial number of files. */
    private static final long NUMBER_OF_FILES = 1_000;
    /** Maximum number of files created on a timer tick. */
    private static final long LIMIT_FILES = 20;

    /** Timer period (delay) for creating new files. */
    private static final long TIMER_PERIOD = 500;

    /**
     * Number of threads running {@code fileChooser.rescanCurrentDirectory()}.
     */
    private static final int NUMBER_OF_THREADS = 5;
    /** Number of repeated calls to {@code rescanCurrentDirectory}. */
    private static final int NUMBER_OF_REPEATS = 2_000;
    /** Maximum amount a thread waits before initiating rescan. */
    private static final long LIMIT_SLEEP = 100;


    private static final CyclicBarrier start = new CyclicBarrier(NUMBER_OF_THREADS);
    private static final CyclicBarrier end = new CyclicBarrier(NUMBER_OF_THREADS + 1);

    private static final CyclicBarrier edtBlocker = new CyclicBarrier(2);
    private static final CyclicBarrier eventBlocker = new CyclicBarrier(2);
    private static final CyclicBarrier scannerBlocker = new CyclicBarrier(NUMBER_OF_THREADS + 1);

    private static final List<Thread> threads = new ArrayList<>(NUMBER_OF_THREADS);

    private static final AtomicReference<Throwable> exception =
            new AtomicReference<>();

    private static final AtomicLong eventCounter = new AtomicLong();


    public static void main(String[] args) throws Throwable {
        try {
            ThreadGroup threadGroup = new BasicDirectoryModelConcurrency();
            Thread runner = new Thread(threadGroup,
                                       BasicDirectoryModelConcurrency::wrapper,
                                       "Test Runner");
            runner.start();
            runner.join();
        } catch (Throwable throwable) {
            handleException(throwable);
        }

        if (exception.get() != null) {
            throw exception.get();
        }
    }

    private static void wrapper() {
        final long timeStart = System.currentTimeMillis();
        try {
            runTest(timeStart);
        } catch (Throwable throwable) {
            handleException(throwable);
        } finally {
            System.out.printf("Duration: %,d\n",
                              (System.currentTimeMillis() - timeStart));
        }
    }

    private static void runTest(final long timeStart) throws Throwable {
        final Path temp = Files.createDirectory(Paths.get("fileChooser-concurrency-" + timeStart));

        final Timer timer = new Timer("File creator");

        try {
            createFiles(temp);

            final JFileChooser fc = new JFileChooser(temp.toFile());
            final BasicDirectoryModel bdm = new BasicDirectoryModel(fc);
            bdm.addListDataListener(new ListDataListener() {
                private void handleEvent(ListDataEvent e, String type) {
                    System.out.println("handleEvent: " + type);
                    eventCounter.incrementAndGet();
//                    try {
//                        eventBlocker.await();
//                    } catch (InterruptedException | BrokenBarrierException ex) {
//                        handleException(ex);
//                    }
                }

                @Override
                public void intervalAdded(ListDataEvent e) {
                    handleEvent(e, "added");
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                    handleEvent(e, "removed");
                }

                @Override
                public void contentsChanged(ListDataEvent e) {
                    handleEvent(e, "changed");
                }
            });

            final TimerTask fileCreator = new CreateFilesTimerTask(temp);

            IntStream.range(0, NUMBER_OF_THREADS)
                     .forEach(i -> {
                         Thread thread = new Thread(new Scanner(bdm));
                         threads.add(thread);
                         thread.start();
                     });

            int counter = 0;
            do {
                System.out.println("Attempt " + counter);
                // Block EDT
                SwingUtilities.invokeLater(() -> {
                    try {
                        System.out.println("> EDT blocked");
                        edtBlocker.await();
                        System.out.println("< EDT unblocked");
                    } catch (InterruptedException | BrokenBarrierException e) {
                        handleException(e);
                    }
                });
                // Create new files and schedule update
                fileCreator.run();

                System.out.println(counter + " validateFileCache 1");
                bdm.validateFileCache();
                // Allow some time to post the event to EDT
                Thread.sleep(100);

                fileCreator.run();

                // Unblock EDT and updates file cache again
                edtBlocker.await();
                System.out.println(counter + " validateFileCache 2");
                bdm.validateFileCache();
//                scannerBlocker.await();
                Thread.sleep(200);
//                bdm.validateFileCache();
//                eventBlocker.await();
            } while (++counter < NUMBER_OF_REPEATS);

//
//            timer.scheduleAtFixedRate(new CreateFilesTimerTask(temp),
//                                      0, TIMER_PERIOD);

//            end.await();
        } catch (Throwable e) {
            threads.forEach(Thread::interrupt);
            throw e;
        } finally {
            timer.cancel();

            deleteFiles(temp);
            Files.delete(temp);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(200);
            System.out.println("EDT released");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private BasicDirectoryModelConcurrency() {
        super("bdmConcurrency");
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        handleException(t, e);
    }

    private static void handleException(Throwable throwable) {
        handleException(Thread.currentThread(), throwable);
    }

    private static void handleException(final Thread thread,
                                        final Throwable throwable) {
        System.err.println("Exception in " + thread.getName() + ": "
                           + throwable.getClass()
                           + (throwable.getMessage() != null
                              ? ": " + throwable.getMessage()
                              : ""));
        if (!exception.compareAndSet(null, throwable)) {
            exception.get().addSuppressed(throwable);
        }
        threads.stream()
               .filter(t -> t != thread)
               .forEach(Thread::interrupt);
    }


    private record Scanner(BasicDirectoryModel bdm)
            implements Runnable {

        @Override
        public void run() {
            try {
                //start.await();

                int counter = 0;
                try {
                    do {
                        scannerBlocker.await();
                        bdm.validateFileCache();
//                        Thread.sleep((long) (Math.random() * LIMIT_SLEEP));
                    } while (++counter < NUMBER_OF_REPEATS
                             && !Thread.interrupted());
                } catch (InterruptedException e) {
                    // Just exit the loop
                }
            } catch (Throwable throwable) {
                handleException(throwable);
            } finally {
//                try {
//                    end.await();
//                } catch (InterruptedException | BrokenBarrierException e) {
//                    handleException(e);
//                }
            }
        }
    }

    private static void createFiles(final Path parent) {
        createFiles(parent, 0, NUMBER_OF_FILES);
    }

    private static void createFiles(final Path parent,
                                    final long start,
                                    final long end) {
        LongStream.range(start, end)
                  .forEach(n -> createFile(parent.resolve(n + ".file")));
    }

    private static void createFile(final Path file) {
        try {
            Files.createFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteFiles(final Path parent) throws IOException {
        try (var stream = Files.walk(parent)) {
            stream.filter(p -> p != parent)
                  .forEach(BasicDirectoryModelConcurrency::deleteFile);
        }
    }

    private static void deleteFile(final Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CreateFilesTimerTask extends TimerTask {
        private final Path temp;
        private long no;

        public CreateFilesTimerTask(Path temp) {
            this.temp = temp;
            no = NUMBER_OF_FILES;
        }

        @Override
        public void run() {
            try {
                long count = (long) (Math.random() * LIMIT_FILES);
                createFiles(temp, no, no + count);
                no += count;
            } catch (Throwable t) {
                handleException(t);
            }
        }
    }
}
