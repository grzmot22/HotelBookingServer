package JGT.GUI;

import JGT.Server;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ServerGUI extends JFrame
{
    private Container contentPane;
    private JLabel labelTest;
    private JTextArea textAreaLog;
    private JButton buttonServerStart;
    private JButton buttonServerStop;

    private Thread waitingThread;

    public ServerGUI()
    {
        super("Hotel Booking Server");

        contentPane = getContentPane();

        setBounds(0, 0, 300, 500);
        contentPane.setLayout(null);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);

        /*
		 * Labels
		 */

        labelTest = new JLabel("SERVER", SwingConstants.RIGHT);
        labelTest.setBounds(80, 29, 100, 11);
        labelTest.setForeground(Color.BLACK);
        contentPane.add(labelTest);

        /*
		 * Textfields
		 */

        textAreaLog = new JTextArea();
        textAreaLog.setBounds(10, 50, 275, 350);
        textAreaLog.setColumns(10);
        textAreaLog.setEditable(false);
        contentPane.add(textAreaLog);

        /*
		 * Button
		 */

        buttonServerStart = new JButton("Start");
        buttonServerStart.setBounds(100, 420, 100, 30);
        contentPane.add(buttonServerStart);

        buttonServerStart.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                waitingThread = new Thread() {
                    public void run() {
                        Server.waitForConnection();
                    }
                };
                waitingThread.start();
            }
        });

        buttonServerStop = new JButton("Stop");
        buttonServerStop.setBounds(100, 420, 100, 30);
        contentPane.add(buttonServerStop);

        buttonServerStop.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                //zatrzymanie servera
            }
        });


        pack();
        setSize(300, 500);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
