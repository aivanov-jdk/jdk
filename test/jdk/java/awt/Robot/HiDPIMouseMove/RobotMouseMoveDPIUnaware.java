import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createLineBorder;

/**
 * Test for
 * <a href="https://bugs.openjdk.java.net/browse/JDK-8249592">JDK-8249592</a>.
 * It moves the mouse the same position as the click.
 */
public class RobotMouseMoveDPIUnaware extends MouseAdapter {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RobotMouseMoveDPIUnaware::new);
    }

    private final Robot robot;

    private RobotMouseMoveDPIUnaware() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final GraphicsDevice[] screens = GraphicsEnvironment
                                         .getLocalGraphicsEnvironment()
                                         .getScreenDevices();
        for (int i = 0; i < screens.length; i++) {
            JPanel panel = new JPanel();
            panel.setBorder(createCompoundBorder(
                    createLineBorder(Color.WHITE, 5),
                    createLineBorder(Color.RED, 3)));
            panel.setBackground(Color.BLACK);
            panel.setPreferredSize(new Dimension(200, 200));
            panel.addMouseListener(this);

            final GraphicsConfiguration gc = screens[i].getDefaultConfiguration();
            JFrame frame = new JFrame("Robot Mouse Move - screen: " + i, gc);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.pack();
            Rectangle scrSize = gc.getBounds();
            Dimension frameSize = frame.getSize();
            scrSize.translate((scrSize.width - frameSize.width) / 2,
                              (scrSize.height - frameSize.height) / 2);
            frame.setLocation(scrSize.getLocation());
            frame.setVisible(true);

            System.out.println("Screen " + i + ": " + scrSize);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("Click  coords: " + e.getPoint());
        Point scrLoc = e.getLocationOnScreen();
        System.out.println("Screen coords: " + scrLoc);

        robot.mouseMove(scrLoc.x, scrLoc.y);
        SwingUtilities.invokeLater(this::printMouse);
    }

    private void printMouse() {
        System.out.println("Mouse: " + MouseInfo.getPointerInfo().getLocation());
    }
}
