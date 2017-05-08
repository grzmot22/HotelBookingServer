package JGT; /**
 * Created by Błażej on 26.04.2017.
 *
 * It's responsible for communication with specified client.
 */
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class ClientServiceThread extends Thread
{
    Socket clientSocket;
    boolean threadRun = true;
    private Connection dataBaseConnection;
    private Statement statementSQL;
    private ResultSet resultSetSQL;
    private ResultSet resultSetSQL1;

    public ClientServiceThread()
    {
        super();
    }

    ClientServiceThread(Socket s)
    {
        clientSocket = s;
    }

    public void run()
    {
        BufferedReader in = null;
        PrintWriter out = null;

        // Print out details of this connection
        System.out.println("Accepted Client Address - " + clientSocket.getInetAddress().getHostName());
        JGT.GUI.ServerGUI.writeToLog("New client connected from address: "+ clientSocket.getInetAddress().getHostName());

        try
        {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));


            while(threadRun)
            {
                // read incoming stream
                String clientCommand = in.readLine();
                //TEST
                System.out.println("Client Says :" + clientCommand);


                if(clientCommand.equals("<!ENDSESSION!>"))
                {
                    threadRun = false;
                    System.out.print("Stopping client thread for client : ");
                }
                else
                {
                    //SEND RESPOND TO REQUEST
                    //TEST
                    out.println(processRequest(clientCommand));

                    out.flush();
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // Clean up
            try
            {
                in.close();
                out.close();
                clientSocket.close();
                JGT.GUI.ServerGUI.writeToLog("Client from address: " + clientSocket.getInetAddress().getHostName() + " disconnected.");
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    private String processRequest(String clientCommand)
    {
        String typeOfCommand = clientCommand.substring(0,clientCommand.indexOf('>') + 1);
        String respond = "";

        switch (typeOfCommand)
        {
            case "<!LOGIN!>":
            {
                String login = clientCommand.substring(9,clientCommand.indexOf(',')).trim();
                String password = clientCommand.substring(clientCommand.indexOf(',') + 1).trim();

                try {
                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT * FROM users");

                    while (resultSetSQL.next())
                    {
                        if (resultSetSQL.getString("username").equals(login) && resultSetSQL.getString("password").equals(password))
                        {
                            if(resultSetSQL.getString("role").equals("USER"))
                                respond = "ACCEPTED_USER";
                            if(resultSetSQL.getString("role").equals("HOTEL_ADMIN"))
                                respond = "ACCEPTED_HOTEL_ADMIN";
                        }
                    }

                    if(respond == "")
                        respond = "DENIED";
                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                    respond = "DENIED";
                }
                break;
            }
            case "<!GET_USER_RESERVATIONS!>":
            {
                String userLogin = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).trim();
                try {
                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT reservations.reservationID, CONCAT(users.Name, ' ', users.Surname) as name, hotels.Name, hotels.City, hotels.Country, reservations.isDoubleRoom, reservations.startDate, reservations.endDate FROM reservations, hotels, users WHERE reservations.userName = users.username AND reservations.userName = '"+userLogin+"' AND reservations.hotelID = hotels.hotelID");

                    while (resultSetSQL.next())
                    {
                        for(int i = 1; i < 9; i++)
                        {
                            respond += resultSetSQL.getString(i) + ",";
                        }
                        respond= respond.substring(0,respond.length()-1) + ";";
                    }



                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case "<!GET_COUNTRY_LIST!>":
            {
                try
                {
                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT hotels.Country FROM hotels");

                    while (resultSetSQL.next())
                    {
                        respond += resultSetSQL.getString(1) + ",";
                    }
                    respond= respond.substring(0,respond.length()-1);
                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }

            case "<!GET_CITIES_LIST!>":
            {
                String country = clientCommand.substring(20,clientCommand.indexOf(']')).trim();

                try
                {
                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT City FROM hotels WHERE Country = \"" + country + "\"");
                    while (resultSetSQL.next())
                    {
                        respond += resultSetSQL.getString(1) + ",";
                    }
                    respond = respond.substring(0,respond.length()-1);
                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }

            case "<!CHECK_AVAILABILITY_BY_QUERY!>":
            {
                String query = clientCommand.substring(32,clientCommand.indexOf(']')).trim();
                String startDate = clientCommand.substring(clientCommand.indexOf('{')+1,clientCommand.indexOf('}')).split(",")[0];
                String endDate = clientCommand.substring(clientCommand.indexOf('{')+1,clientCommand.indexOf('}')).split(",")[1];
                boolean isSingle = clientCommand.contains("SingleRoomCount > 0");
                int roomsCount = 0;
                try
                {
                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery(query);


                    if(resultSetSQL.next()) {
                        do
                        {
                            if (isSingle)
                                roomsCount = resultSetSQL.getInt("SingleRoomCount");
                            else
                                roomsCount = resultSetSQL.getInt("DoubleRoomCount");

                            //check avability a potem dodaj lub nie do odpowiedzi
                            if (checkAvailability(resultSetSQL.getInt("hotelID"), startDate, endDate, isSingle, roomsCount))
                            {
                                respond += resultSetSQL.getString(2) + ",";
                                respond += resultSetSQL.getString(3) + ",";
                                respond += resultSetSQL.getString(4) + ",";
                                if (isSingle)
                                    respond += "Single," + resultSetSQL.getInt("Rate") + "," + resultSetSQL.getString("SingleRoomPrice") + ";";
                                else
                                    respond += "Double," + resultSetSQL.getInt("Rate") + "," + resultSetSQL.getString("DoubleRoomPrice") + ";";
                            }
                        } while (resultSetSQL.next());
                    }
                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }

                break;
            }
            case("<!MAKE_RESERVATION!>"):
            {
                try {
                    int hotelId = -1;
                    int roomType;
                    String username = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[0];
                    String hotelName = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[1];
                    String hotelCity = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[2];
                    String hotelCountry = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[3];

                    System.out.println(clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[4].toString());

                    if(clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[4].contains("Single"))
                        roomType = 0;
                    else
                        roomType = 1;
                    String startDate = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[5];
                    String endDate = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[6];
                    startDate = startDate.split("/")[2] + "-" + startDate.split("/")[1] + "-" + startDate.split("/")[0];
                    endDate = endDate.split("/")[2] + "-" + endDate.split("/")[1] + "-" + endDate.split("/")[0];

                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT hotelID FROM hotels WHERE Name='"+hotelName+"' AND City='"+hotelCity+"' AND Country='"+hotelCountry+"'");
                    while (resultSetSQL.next())
                        hotelId = resultSetSQL.getInt(1);


                    int result = statementSQL.executeUpdate("INSERT INTO reservations(userName, hotelID, isDoubleRoom, startDate, endDate) VALUES ('" +username+ "'," +hotelId+ "," +roomType+ ",'" +startDate+ "','" +endDate+ "');");
                    JGT.GUI.ServerGUI.writeToLog("Client from address: " + clientSocket.getInetAddress().getHostName() + " made a reservation.");

                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case("<!REGISTER!>"):
            {
                try
                {
                    String username = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[0];
                    String password = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[1];
                    String name = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[2];
                    String surname = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[0];

                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    int result = statementSQL.executeUpdate("INSERT INTO users(username, password, role, Name, Surname) VALUES ('" +username+ "','" +password+ "','USER','" +name+ "','" +surname+ "');");

                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case("<!GET_NAME!>"):
            {
                try
                {
                    String username = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']'));

                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT users.Name,users.Surname FROM users WHERE users.username = '"+username+"';");
                    while (resultSetSQL.next())
                    {
                        respond += resultSetSQL.getString(1) + " " + resultSetSQL.getString(2);
                    }

                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case("<!EDIT_RESERVATION!>"):
            {
                try {
                    int hotelId = -1;
                    boolean isSingle = false;
                    int roomType = 0;
                    int roomsCount = 0;
                    String username = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[0];
                    String hotelName = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[1];
                    String hotelCity = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[2];
                    String hotelCountry = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[3];

                    if (clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[4].contains("Single"))
                        isSingle = true;
                    else
                        isSingle = false;
                    String startDate = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[5];
                    String endDate = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[6];
                    String reservationID = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']')).split(",")[7];

                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT hotelID, SingleRoomCount, DoubleRoomCount FROM hotels WHERE Name='"+hotelName+"' AND City='"+hotelCity+"' AND Country='"+hotelCountry+"'");
                    while (resultSetSQL.next()) {
                        if (isSingle) {
                            roomsCount = resultSetSQL.getInt("SingleRoomCount");
                            roomType = 0;
                        }
                        else {
                            roomsCount = resultSetSQL.getInt("DoubleRoomCount");
                            roomType = 1;
                        }
                        hotelId = resultSetSQL.getInt(1);
                    }

                    boolean result = checkAvailability(hotelId, startDate, endDate, isSingle, roomsCount);

                    if(result)
                    {
                        if(startDate.contains("/")) {
                            startDate = startDate.split("/")[2] + "-" + startDate.split("/")[1] + "-" + startDate.split("/")[0];
                            endDate = endDate.split("/")[2] + "-" + endDate.split("/")[1] + "-" + endDate.split("/")[0];
                        }
                        int res = statementSQL.executeUpdate("UPDATE reservations SET isDoubleRoom="+roomType+", startDate='"+startDate+"', endDate='"+endDate+"' WHERE reservationID="+reservationID+";");
                        JGT.GUI.ServerGUI.writeToLog("Client from address: " + clientSocket.getInetAddress().getHostName() + " edited his reservation id: "+ resultSetSQL+".");
                    }
                    else
                    {
                        respond = "DENIED";
                    }
                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case("<!DELETE_RESERVATION!>"):
            {
                try
                {
                    String id = clientCommand.substring(clientCommand.indexOf('[') + 1, clientCommand.indexOf(']'));

                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    int result = statementSQL.executeUpdate("DELETE FROM reservations WHERE reservationID = " + id);
                    JGT.GUI.ServerGUI.writeToLog("Client from address: " + clientSocket.getInetAddress().getHostName() + " canceled his reservation id: " + id + ".");
                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case("<!GET_HOTEL_NAME!>"):
            {
                try
                {
                    String hotelID = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']'));

                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    System.out.println("SELECT Name, City FROM hotels WHERE hotels.hotelID = '"+hotelID+"';");
                    resultSetSQL = statementSQL.executeQuery("SELECT Name, City FROM hotels WHERE hotels.hotelID = '"+hotelID+"';");
                    while (resultSetSQL.next())
                    {
                        respond += resultSetSQL.getString(1) + " " + resultSetSQL.getString(2);
                    }

                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case "<!GET_HOTEL_RESERVATIONS!>":
            {
                String hotelID = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).trim();
                try {
                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();
                    resultSetSQL = statementSQL.executeQuery("SELECT reservations.reservationID, CONCAT(users.Name, ' ', users.Surname) as name, reservations.isDoubleRoom, reservations.startDate, reservations.endDate, reservations.RoomNumber FROM reservations, hotels, users WHERE reservations.userName = users.username AND reservations.hotelID = '"+hotelID+"' AND reservations.hotelID = hotels.hotelID");

                    while (resultSetSQL.next())
                    {
                        for(int i = 1; i < 7; i++)
                        {
                            respond += resultSetSQL.getString(i) + ",";
                        }
                        respond= respond.substring(0,respond.length()-1) + ";";
                    }
                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
            case "<!UPDATE_ROOM_INFORMATION!>":
            {
                String reservationID = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[0].trim();
                String roomNo = clientCommand.substring(clientCommand.indexOf('[')+1,clientCommand.indexOf(']')).split(",")[1].trim();
                try {
                    dataBaseConnection = Server.getDatabaseConnection();
                    statementSQL = dataBaseConnection.createStatement();

                    int res = statementSQL.executeUpdate("UPDATE reservations SET RoomNumber='"+roomNo+"' WHERE reservationID="+reservationID+";");
                    JGT.GUI.ServerGUI.writeToLog("Client from address: " + clientSocket.getInetAddress().getHostName() + " updated room information.");

                }
                catch(Exception e1)
                {
                    e1.printStackTrace();
                }
                break;
            }
        }

        return respond;
    }

    private boolean checkAvailability(int hotelID, String startDate, String endDate, boolean isSingle, int roomsCount)
    {
        if(startDate.contains("/")) {
            startDate = startDate.split("/")[2] + "-" + startDate.split("/")[1] + "-" + startDate.split("/")[0];
            endDate = endDate.split("/")[2] + "-" + endDate.split("/")[1] + "-" + endDate.split("/")[0];
        }
        int reservationCounter = 0;

        if(isSingle)
        {
            try
            {
                dataBaseConnection = Server.getDatabaseConnection();
                statementSQL = dataBaseConnection.createStatement();


                System.out.println("SELECT * FROM `reservations` WHERE ((reservations.startDate <= '" + startDate + "' AND reservations.endDate >= '" + endDate + "') OR\n" +
                        "(reservations.startDate >= '" + startDate + "' AND reservations.startDate <= '" + endDate + "') OR\n" +
                        "(reservations.endDate >= '" + startDate + "' AND reservations.endDate <= '" + endDate + "')) AND reservations.hotelID = '" + hotelID + "' AND reservations.isDoubleRoom = 0");
                resultSetSQL1 = statementSQL.executeQuery(
                    "SELECT * FROM `reservations` WHERE ((reservations.startDate <= '" + startDate + "' AND reservations.endDate >= '" + endDate + "') OR\n" +
                    "(reservations.startDate >= '" + startDate + "' AND reservations.startDate <= '" + endDate + "') OR\n" +
                    "(reservations.endDate >= '" + startDate + "' AND reservations.endDate <= '" + endDate + "')) AND reservations.hotelID = '" + hotelID + "' AND reservations.isDoubleRoom = 0");

                while (resultSetSQL1.next())
                {
                    reservationCounter++;
                }

            }
            catch(Exception e1)
            {
                e1.printStackTrace();
            }
        }
        else
        {
            try
            {
                dataBaseConnection = Server.getDatabaseConnection();
                statementSQL = dataBaseConnection.createStatement();
                System.out.println("SELECT * FROM `reservations` WHERE ((reservations.startDate <= '" + startDate + "' AND reservations.endDate >= '" + endDate + "') OR\n" +
                        "(reservations.startDate >= '" + startDate + "' AND reservations.startDate <= '" + endDate + "') OR\n" +
                        "(reservations.endDate >= '" + startDate + "' AND reservations.endDate <= '" + endDate + "')) AND reservations.hotelID = '" + hotelID + "' AND reservations.isDoubleRoom = 1");
                resultSetSQL1 = statementSQL.executeQuery(
                        "SELECT * FROM `reservations` WHERE ((reservations.startDate <= '" + startDate + "' AND reservations.endDate >= '" + endDate + "') OR\n" +
                                "(reservations.startDate >= '" + startDate + "' AND reservations.startDate <= '" + endDate + "') OR\n" +
                                "(reservations.endDate >= '" + startDate + "' AND reservations.endDate <= '" + endDate + "')) AND reservations.hotelID = '" + hotelID + "' AND reservations.isDoubleRoom = 1");

                while (resultSetSQL1.next())
                {
                    reservationCounter++;
                }
            }
            catch(Exception e1)
            {
                e1.printStackTrace();
            }
        }
        System.out.println(reservationCounter +" < "+ roomsCount);
        if(reservationCounter < roomsCount)
            return true;
        else
            return false;
    }

}