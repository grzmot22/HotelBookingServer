package JGT.GUI;

import JGT.Server;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;


public class Login extends JFrame
{
    private Container contentPane;

    private JLabel labelLogin;
    private JLabel labelPassword;

    private JTextField textFieldLogin;
    private JPasswordField textFieldPassword;

    private JButton buttonLogin;

    private Connection dataBaseConnection;
    private Statement statementSQL;
    private ResultSet resultSetSQL;

    public Login()
    {

        super("Login");

        contentPane = getContentPane();

        setBounds(0, 0, 340, 220);
        contentPane.setLayout(null);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);

		/*
		 * Labels
		 */

        labelLogin = new JLabel("Username: ", SwingConstants.RIGHT);
        labelLogin.setBounds(10, 29, 100, 11);
        labelLogin.setForeground(Color.BLACK);
        contentPane.add(labelLogin);

        labelPassword = new JLabel("Password: ", SwingConstants.RIGHT);
        labelPassword.setBounds(10, 59, 100, 11);
        labelPassword.setForeground(Color.BLACK);
        contentPane.add(labelPassword);

        /*
		 * Textfields
		 */

        textFieldLogin = new JTextField();
        textFieldLogin.setBounds(115, 25, 178, 20);
        textFieldLogin.setColumns(10);
        contentPane.add(textFieldLogin);

        textFieldPassword = new JPasswordField();
        textFieldPassword.setBounds(115, 55, 178, 20);
        textFieldPassword.setColumns(10);
        contentPane.add(textFieldPassword);


        /*
		 * Button
		 */

        buttonLogin = new JButton("Login");
        buttonLogin.setBounds(120, 90, 100, 30);
        contentPane.add(buttonLogin);

        buttonLogin.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String accessResult = checkLoginData();
                if(accessResult == "ACCEPTED")
                {
                    setVisible(false);
                    dispose();
                    JGT.GUI.ServerGUI serverGUI = new JGT.GUI.ServerGUI();
                }
                else
                {
                    if(accessResult == "DENIED")
                    {
                        JOptionPane.showMessageDialog(null, "Wrong login or password.");
                    }
                    else
                    {
                        if(accessResult == "DENIED_ROLE")
                        {
                            JOptionPane.showMessageDialog(null, "Access denied.");
                        }
                    }
                }
            }
        });

        pack();
        setSize(340, 170);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private String checkLoginData()
    {
        String login = this.textFieldLogin.getText();
        String password = new String(this.textFieldPassword.getPassword());

        try {
            dataBaseConnection = Server.getDatabaseConnection();
            statementSQL = dataBaseConnection.createStatement();
            resultSetSQL = statementSQL.executeQuery("SELECT * FROM users");

            while (resultSetSQL.next())
            {
                if (resultSetSQL.getString("username").equals(login) && resultSetSQL.getString("password").equals(password))
                {
                    if(resultSetSQL.getString("role").equals("SERVER_ADMIN"))
                        return "ACCEPTED";
                    else
                        return "DENIED_ROLE";
                }
            }
            return "DENIED";
        }
        catch(Exception e1)
        {
            e1.printStackTrace();
            return "DENIED";
        }

    }



}
