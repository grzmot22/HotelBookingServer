package JGT;

import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.sql.*;

public class Server
{
    private static Connection mySQLconnection;

    public static void main(String[] args )
    {
        try
        {
            //mySQLconnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/hotelbookingdatabase", "Blazej", "enter121");
            mySQLconnection = DriverManager.getConnection("jdbc:mysql://studentnet.cst.beds.ac.uk:3306/hotelbookingdatabase","1612761", "1612761");

            if(mySQLconnection.isValid(15))
            {
                JGT.GUI.Login login = new JGT.GUI.Login();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void waitForConnection()
    {
        try
        {
            int i = 1;
            ServerSocket s = new ServerSocket(8189);
            while (true)
            {
                Socket incoming = s.accept();

                ClientServiceThread cliThread = new ClientServiceThread(incoming);
                cliThread.start();
                i++;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static Connection getDatabaseConnection()
    {
        return mySQLconnection;
    }


}
