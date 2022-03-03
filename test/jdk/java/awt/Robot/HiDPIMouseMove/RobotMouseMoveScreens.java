import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for
 * <a href="https://bugs.openjdk.java.net/browse/JDK-8249592">JDK-8249592</a>.
 * It moves the mouse to different points on the screen(s) in a sequential
 * manner and reads mouse pointer position back to confirm that
 * mouse moved to the correct position as expected.
 */
public class RobotMouseMoveScreens {
    private static final int DELAY = 150;

    private static final int[] STEPS = {50, 53, 83, 97, 100};

    private static class Stats {
        /**
         * The step for mouse move.
         */
        private final int step;

        /**
         * The total number of mouse move.
         */
        private int totalMoves = 0;

        /**
         * The number of mouse coordinates that matched accurately.
         */
        private int match0 = 0;

        /**
         * The number of mouse coordinates that matched with 1px tolerance.
         */
        private int match1 = 0;

        /**
         * The number of NPEs.
         */
        private int npe = 0;

        /**
         * The number of mismatch for x coordinate.
         */
        private int xMismatch = 0;

        /**
         * The number of mismatch for y coordinate.
         */
        private int yMismatch = 0;

        public Stats(int step) {
            this.step = step;
        }

        public void runTest(final Robot robot,
                             final GraphicsDevice[] screens) {
            System.out.println("Step: " + step);

            int screenNo = 0;
            for (GraphicsDevice screen : screens) {
                Rectangle bounds = screen.getDefaultConfiguration()
                                         .getBounds();
                System.out.println("Screen " + (++screenNo) + ": " + bounds);
                runTest(robot, bounds);
            }
        }

        private void runTest(final Robot robot,
                             final Rectangle bounds) {
            final int xMax = bounds.x + bounds.width - 1;
            final int yMax = bounds.y + bounds.height - 1;

            for (int x = bounds.x; x < xMax; x += step) {
                for (int y = bounds.y; y < yMax; y += step) {
                    robot.mouseMove(x, y);
                    totalMoves++;

                    robot.delay(DELAY);

                    try {
                        final Point mouse = MouseInfo.getPointerInfo()
                                                     .getLocation();

                        boolean matched = x == mouse.x && y == mouse.y;
                        boolean xMismatched = Math.abs(x - mouse.x) == 1;
                        boolean yMismatched = Math.abs(y - mouse.y) == 1;
                        if (xMismatched) {
                            xMismatch++;
                        }
                        if (yMismatched) {
                            yMismatch++;
                        }
                        if (matched) {
                            match0++;
                        }
                        if (matched || xMismatched || yMismatched) {
                            match1++;
                        }
                    } catch (NullPointerException e) {
                        npe++;
                    }
                }
            }
        }

        public void print() {
            float match0Percent = ((match0 * 100f) / totalMoves);
            float match1Percent = ((match1 * 100f) / totalMoves);
            System.out.println("************************************************");
            System.out.printf("Percentage: %3.2f vs %3.2f\n", match0Percent, match1Percent);
            System.out.printf("Total:      %5d\n", totalMoves);
            System.out.printf("Matched0:   %5d\n", match0);
            System.out.printf("Matched1:   %5d\n", match1);
            System.out.printf("xMismatch:  %5d\n", xMismatch);
            System.out.printf("yMismatch:  %5d\n", yMismatch);
            System.out.printf("NPE:        %5d\n", npe);
        }

        public void printCSV() {
            int[] data = {step,totalMoves, match0, match1, xMismatch, yMismatch, npe};
            System.out.println(Arrays.stream(data)
                                     .mapToObj(Integer::toString)
                                     .collect(Collectors.joining(","))
            );
        }
    }

    public static void main(String[] args) throws AWTException {
        if (args.length < 1) {
            System.err.println("The type DPI Awareness is required: Per-Monitor or Unaware");
            System.exit(1);
        }
        final String typeOfScaling = args[0];

        final GraphicsDevice[] screens = GraphicsEnvironment
                                                 .getLocalGraphicsEnvironment()
                                                 .getScreenDevices();
        final Robot robot = new Robot();

        List<Stats> statsList = new ArrayList<>(STEPS.length);
        for (int step : STEPS) {
            Stats stats = new Stats(step);
            stats.runTest(robot, screens);
            stats.print();
            statsList.add(stats);
        }

        System.out.println("\n\nFinished: " + typeOfScaling);
        System.out.println("************************************************");
        System.out.println("************************************************");

        statsList.forEach(Stats::printCSV);
    }
}
