import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicDirectoryModel;

/*
 * @test
 * @bug 8323670 8307091 8240690
 * @requires os.family == "mac" | os.family == "linux"
 * @summary Verifies thread-safety of BasicDirectoryModel (JFileChooser)
 */
public final class BasicDirectoryModelConcurrency extends ThreadGroup {
    /** Initial number of files. */
    private static final long NUMBER_OF_FILES = 2_000;
    /** Maximum number of files created or removed on a timer tick. */
    private static final long LIMIT_FILES = 200;


    /**
     * Number of threads running {@code fileChooser.rescanCurrentDirectory()}.
     */
    private static final int NUMBER_OF_THREADS = 5;
    /** Number of repeated calls to {@code rescanCurrentDirectory}. */
    private static final int NUMBER_OF_REPEATS = 2_000;


    private static final CyclicBarrier end = new CyclicBarrier(NUMBER_OF_THREADS + 1);

    private static final CyclicBarrier edtBlocker = new CyclicBarrier(2);
    private static final CyclicBarrier scannerScanStart = new CyclicBarrier(NUMBER_OF_THREADS + 1);
    private static final CyclicBarrier scannerScanEnd = new CyclicBarrier(NUMBER_OF_THREADS + 1);

    private static final List<Thread> threads = new ArrayList<>(NUMBER_OF_THREADS);

    private static final AtomicReference<Throwable> exception =
            new AtomicReference<>();


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

        try {
            createFiles(temp);

            final JFileChooser fc = new JFileChooser(temp.toFile());
            final BasicDirectoryModel bdm = new BasicDirectoryModel(fc);

            final TimerTask fileCreator = new CreateFilesTimerTask(temp);

            IntStream.range(0, NUMBER_OF_THREADS)
                     .forEach(i -> {
                         Thread thread = new Thread(new Scanner(bdm));
                         threads.add(thread);
                         thread.start();
                     });
            threads.add(Thread.currentThread());

            int counter = 0;
            try {
                do {
                    // Block EDT
                    SwingUtilities.invokeLater(() -> {
                        try {
                            edtBlocker.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            handleException(e);
                        }
                    });

                    // Create new files and schedule update
                    fileCreator.run();
                    bdm.validateFileCache();

                    // Create more files
                    fileCreator.run();

                    // Unblock EDT and updates file cache again
                    edtBlocker.await();
                    scannerScanStart.await();

                    // Wait until scanner threads return from validateFileCache()
                    scannerScanEnd.await();
                } while (++counter < NUMBER_OF_REPEATS
                         && !Thread.interrupted());
            } catch (InterruptedException e) {
                // Just exit the loop
            }
        } catch (Throwable e) {
            threads.forEach(Thread::interrupt);
            throw e;
        } finally {
            try {
                System.out.println("end.await");
                end.await();
            } finally {
                deleteFiles(temp);
                Files.delete(temp);
            }
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
                int counter = 0;
                try {
                    do {
                        scannerScanStart.await();
                        bdm.validateFileCache();
                        scannerScanEnd.await();
                    } while (++counter < NUMBER_OF_REPEATS
                             && !Thread.interrupted());
                } catch (InterruptedException e) {
                    // Just exit the loop
                }
            } catch (Throwable throwable) {
                handleException(throwable);
            } finally {
                try {
                    System.out.println("end.await" + Thread.currentThread().getName());
                    end.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    handleException(e);
                }
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
                if (no > NUMBER_OF_FILES / 2) {
                    LongStream.range(no - count, no)
                              .forEach(n -> deleteFile(temp.resolve(n + ".file")));
                    no -= count;
                } else {
                    createFiles(temp, no, no + count);
                    no += count;
                }
            } catch (Throwable t) {
                handleException(t);
            }
        }
    }
}
