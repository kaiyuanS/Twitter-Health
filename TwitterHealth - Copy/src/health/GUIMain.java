package health;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;

/**

 */
public final class GUIMain {
    
    /**
     * Private constructor to prohibit instantiation.
     */
    private GUIMain() { }

    /**
     * main method that starts the program.
     * @param aRgs command line arguments, assumes none
     */
    public static void main(String[] aRgs) {
        EventQueue.invokeLater(new Runnable()
        {
            public void run() {
                // create a frame
                final JFrame frame = new JFrame();
                // create a panel
                final TwitterHealthPanel panel = new TwitterHealthPanel();
                // add a panel to the frame
                frame.add(panel);
                frame.setTitle("Twitter Health Project");
                frame.setPreferredSize(new Dimension(850, 700));
                frame.setResizable(false);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                
            }
        });
    }

}