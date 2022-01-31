import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The test case for
 * <a href="https://bugs.openjdk.java.net/browse/JDK-8249592">JDK-8249592:</a>
 * Robot.mouseMove moves cursor to incorrect location when display scale varies
 */
public class RobotScalingTest {

    public static void main(String[] args) {

        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        JFrame roboTestFrm = new JFrame("Robot Test");
        roboTestFrm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        roboTestFrm.setLayout(new GridLayout(1, screens.length));

        for (int screenNum=0; screenNum < screens.length; screenNum++) {
            GraphicsDevice device = screens[screenNum];

            JPanel pan = new JPanel();
            pan.setPreferredSize(new Dimension(200, 200));
            pan.setBorder(BorderFactory.createLineBorder(Color.GRAY, 5));
            pan.setBackground(Color.BLACK);

            pan.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Point panelCoords = e.getPoint();

                    System.out.println(Arrays.toString(device.getConfigurations()));

                    Point screenCoordsViaConversion = new Point(panelCoords);
                    SwingUtilities.convertPointToScreen(screenCoordsViaConversion, pan);

                    Point screenCoordsViaMouseEvent = e.getLocationOnScreen();
                    Point screenCoordsViaMouseInfo = MouseInfo.getPointerInfo().getLocation();

// Note: These 3 points always seem to match / agree
                    System.out.println("From MouseInfo: "+screenCoordsViaMouseInfo);
                    System.out.println("From convertPointToScreen: "+screenCoordsViaConversion);
                    System.out.println("From MouseEvent: "+screenCoordsViaMouseEvent);

                    try {
// The results of the "mouseMove" call appear to be the same regardless of which device is passed to the CTOR here
                        Robot robo = new Robot(device);
                        robo.mouseMove(screenCoordsViaConversion.x, screenCoordsViaConversion.y);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                }
            });

            roboTestFrm.add(pan);
        }

        roboTestFrm.pack();
        roboTestFrm.setVisible(true);
    }
}
