package JGT; /**
 * Created by Błażej on 26.04.2017.
 *
 * It implements multithreaded server on port 8189
 */

import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.sql.*;

public class Server
{
    private static Connection mySQLconnection;

    public static void main(String[] args )
    {
        /*
        1. Ustanowienie polaczenia z baza (ewentualny error i huj)
        2. Zalogowanie bezposrednio komunikujac sie z baza (ewentulane niepowodzenie)
        3. Otwarcie serwera z mozliwoscia uruchomienia itp.

            logi na zywo i w pliku
            dane o polaczeniach, moze rezerwacjach

        4. mozliwosc zatrzymania serwera
        */
        try
        {
            mySQLconnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/hotelbookingdatabase", "root", "");

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
                System.out.println("Spawning " + i);

                ClientServiceThread cliThread = new ClientServiceThread(incoming);
                cliThread.start();
/*
                Runnable r = new ClientServiceThread(incoming);
                Thread t = new Thread(r);
                t.start();
  */
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
