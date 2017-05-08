package JGT.GUI;

import JGT.Server;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.text.DateFormat;
import java.util.Date;
import java.text.*;


public class ServerGUI extends JFrame
{
    private Container contentPane;
    private JLabel labelTest;
    private static JTextArea textAreaLog;
    private JButton buttonServerStart;
    private JButton buttonServerStop;

    private Thread waitingThread;

    public ServerGUI()
    {
        super("Hotel Booking Server");

        contentPane = getContentPane();

        setBounds(0, 0, 600, 500);
        contentPane.setLayout(null);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);

        /*
		 * Labels
		 */

        labelTest = new JLabel("SERVER", SwingConstants.RIGHT);
        labelTest.setBounds(250, 29, 100, 11);
        labelTest.setForeground(Color.BLACK);
        contentPane.add(labelTest);

        /*
		 * Textfields
		 */

        textAreaLog = new JTextArea();
        textAreaLog.setBounds(10, 50, 575, 350);
        textAreaLog.setColumns(10);
        textAreaLog.setEditable(false);
        contentPane.add(textAreaLog);

        /*
		 * Button
		 */

        buttonServerStart = new JButton("Start");
        buttonServerStart.setBounds(175, 420, 100, 30);
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
                JGT.GUI.ServerGUI.writeToLog("Server started.");
            }
        });

        buttonServerStop = new JButton("Stop");
        buttonServerStop.setBounds(325, 420, 100, 30);
        contentPane.add(buttonServerStop);

        buttonServerStop.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                //zatrzymanie servera
            }
        });


        pack();
        setSize(600, 500);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void writeToLog(String message)
    {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        Date date = new Date();
        System.out.println(dateFormat.format(date));
        textAreaLog.setText(textAreaLog.getText() + "\n[" + dateFormat.format(date) + "] " + message);
    }
}
