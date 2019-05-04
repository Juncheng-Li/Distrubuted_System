package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ServerSocketFactory;
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import unimelb.bitbox.util.*;

import java.nio.file.Files;

public class Server extends Thread
{
    private String ip;
    private int port;
    private int i = 0;
    private ServerSocket listeningSocket = null;
    private Socket clientSocket = null;
    private String clientMsg = null;
    private JSONObject command = null;
    private ServerMain f;
    private Timer timer = new Timer();

    Server(int port, Socket clientSocket, int i, ServerSocket serverSocket)
    {
        this.port = port;
        this.clientSocket = clientSocket;
        this.i = i;
        this.listeningSocket = serverSocket;
        this.ip = ip;
    }


    public void run()
    {
        System.out.println("Thread: Server for client-" + i + " started...");
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
            JSONParser parser = new JSONParser();
            f = new ServerMain(clientSocket);

            try
            {
                while ((clientMsg = in.readLine()) != null)
                {
                    command = (JSONObject) parser.parse(clientMsg);
                    System.out.println("(Server)Message from client " + i + ": " + command.toJSONString());
                    //doCommand(command, out);
                    if (command.getClass().getName().equals("org.json.simple.JSONObject"))
                    {
                        if (command.get("command").toString().equals("HANDSHAKE_REQUEST"))
                        {
                            JSONObject hs_res = new JSONObject();
                            JSONObject hostPort = new JSONObject();
                            String ip = Configuration.getConfigurationValue("advertisedName");
                            int port = Integer.parseInt(Configuration.getConfigurationValue("port"));
                            hs_res.put("command", "HANDSHAKE_RESPONSE");
                            hostPort.put("host", ip);
                            hostPort.put("port", port);
                            hs_res.put("hostPort", hostPort);
                            System.out.println(hs_res.toJSONString());
                            out.write(hs_res + "\n");
                            out.flush();

                            timer.schedule(new SyncEvents(f), 0,
                                        Integer.parseInt(Configuration.getConfigurationValue("syncInterval"))*1000);
                        }
                        else
                        {
                            commNProcess command_T = new commNProcess(command, clientSocket, f);
                            command_T.start();
                        }
                    }
                    else
                    {
                        // If not a JSONObject
                        JSONObject reply = new JSONObject();
                        reply.put("command", "INVALID_PROTOCOL");
                        reply.put("message", "message must contain a command field as string");
                        System.out.println(reply);
                        out.write(reply + "\n");
                        out.flush();
                    }
                }
            } catch (SocketException e)
            {
                clientSocket.close();
                System.out.println("closed...");
            } catch (ParseException e)
            {
                e.printStackTrace();
            }
            clientSocket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        } finally
        {
            // kill timer
            timer.cancel();
            timer.purge();
            // kill serverMain
            f.fileSystemManager.stop();
            System.out.println("Client " + i + " disconnected.");
        }
    }


}
