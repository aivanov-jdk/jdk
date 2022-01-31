import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;

/**
 * Test for
 * <a href="https://bugs.openjdk.java.net/browse/JDK-8249592">JDK-8249592</a>.
 * It moves the mouse each corner and then to the middle of the screen.
 */
public class RobotMouseMoveScreens {
    private static final long DELAY = 1500;

    public static void main(String[] args) throws AWTException, InterruptedException {
        final GraphicsDevice[] screens = GraphicsEnvironment
                                         .getLocalGraphicsEnvironment()
                                         .getScreenDevices();
        final Robot robot = new Robot();
        for (GraphicsDevice screen : screens) {
            Rectangle bounds = screen.getDefaultConfiguration()
                                     .getBounds();
            System.out.println(bounds);
            moveMouseTo(robot, bounds);
        }
    }

    private static void moveMouseTo(final Robot robot, final Rectangle bounds)
            throws InterruptedException {
        for (Point p : new Point[] {
                new Point(bounds.x, bounds.y),
                new Point(bounds.x + bounds.width - 1, bounds.y),
                new Point(bounds.x + bounds.width - 1, bounds.y + bounds.height - 1),
                new Point(bounds.x, bounds.y + bounds.height - 1),
                new Point((int) bounds.getCenterX(), (int) bounds.getCenterY())
                }) {
            System.out.println("    " + p);
            robot.mouseMove(p.x, p.y);
            Thread.sleep(DELAY);
            final Point mouse = MouseInfo.getPointerInfo().getLocation();
            assert p.x == mouse.x && p.y == mouse.y
                   : "Mouse is not at the same location: "
                   + "p = " + p + "; mouse = " + mouse;
        }
    }
}
