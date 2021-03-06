package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.*;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Timer;


class Peer_clientSide extends Thread
{
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private ServerMain f;
    private Timer timer = new Timer();
    private socketStorage ss;

    Peer_clientSide(Socket socket, ServerMain f, socketStorage ss) throws IOException, NoSuchAlgorithmException
    {
        this.socket = socket;
        System.out.println("Connection established");
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
        this.out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));
        this.f = f;
        this.ss = ss;
    }

    public void run()
    {
        try
        {
            // Handshake - fixed!
            JSONObject hs = new JSONObject();
            JSONObject hostPort = new JSONObject();
            hostPort.put("host", Configuration.getConfigurationValue("advertisedName"));
            hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
            hs.put("command", "HANDSHAKE_REQUEST");
            hs.put("hostPort", hostPort);
            out.write(hs.toJSONString() + "\n");
            out.flush();
            System.out.println("sent: " + hs);


            // Receive incoming reply
            JSONParser parser = new JSONParser();
            String message = null;
            while ((message = in.readLine()) != null)
            {
                JSONObject command = (JSONObject) parser.parse(message);
                System.out.println("Message from peer: " + command.toJSONString());

                //Copied from server
                if (command.getClass().getName().equals("org.json.simple.JSONObject"))
                {
                    if (command.get("command").toString().equals("HANDSHAKE_RESPONSE"))
                    {
                        // Synchronizing Events after Handshake!!!
                        timer.schedule(new SyncEvents(f), 0,
                                Integer.parseInt(Configuration.getConfigurationValue("syncInterval")) * 1000);
                    } else if (command.get("command").toString().equals("CONNECTION_REFUSED"))
                    {
                        System.out.println("Peer(" +
                                socket.getRemoteSocketAddress().toString().replaceAll("/", "")
                                + ") maximum connection limit reached...");
                        break;
                    } else
                    {
                        commNProcess process_T = new commNProcess(command, socket, f);
                        process_T.start();
                    }
                } else
                {
                    // If not a JSONObject
                    JSONObject reply = new JSONObject();
                    reply.put("command", "INVALID_PROTOCOL");
                    reply.put("message", "message must contain a command field as string");
                    System.out.println("sent: " + reply);
                    out.write(reply.toJSONString() + "\n");
                    out.flush();
                }
            }
        } catch (UnknownHostException e)
        {
            System.out.println("Unknown host");
            //e.printStackTrace();
        } catch (IOException e)
        {
            if (e.toString().contains("ConnectException"))
            {
                System.out.println("Peer working as a server");
            }
            else
            {
                /*
                System.out.println("Peer_clientSide class IOException error!!");
                e.printStackTrace();
                 */
                System.out.println("Peer_clientSide class socket close caught!");
            }
        } catch (ParseException e)
        {
            System.out.println("Parse invalid letter at " + e.getPosition());
            //e.printStackTrace();
        } finally
        {
            // Close the socket
            if (socket != null)
            {
                try
                {
                    System.out.println("Peer("
                            + socket.getRemoteSocketAddress().toString().replaceAll("/", "")
                            + ") socket closed...");
                    timer.cancel();
                    timer.purge();
                    ss.remove(socket);
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e)
                {
                    System.out.println("Socket cannot be closed...");
                    //e.printStackTrace();
                }
            }
        }
    }
}
