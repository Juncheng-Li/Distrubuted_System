package unimelb.bitbox;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;


class client_T extends Thread
{
    private Thread t;
    private String threadName;
    private String ip;
    private int port;
    public Socket socket = null;
    //private ServerMain f;
    public BufferedReader in;
    public BufferedWriter out;
    private ServerMain f;

    client_T(BufferedReader in, BufferedWriter out, ServerMain f)
    {
        this.in = in;
        this.out = out;
        this.f = f;
    }

    public void run()
    {

        try
        {
            // Handshake - fixed!
            JSONObject hs = new JSONObject();
            JSONObject hostPort = new JSONObject();
            hostPort.put("host", "10.0.0.50");
            hostPort.put("port", 3000);
            hs.put("command", "HANDSHAKE_REQUEST");
            hs.put("hostPort", hostPort);
            out.write(hs + "\n");
            out.flush();
            System.out.println(hs);

            /*
            // Send and Print what is sent
            JSONObject sendMessage = new JSONObject();
            sendMessage = parseRequest(eventString, pathName);
            out.write(sendMessage + "\n");
            out.flush();
            System.out.println(sendMessage.get("command") + " sent");
             */
            // Receive incoming reply
            JSONParser parser = new JSONParser();
            String message = null;
            while ((message = in.readLine()) != null)
            {
                JSONObject command = (JSONObject) parser.parse(message);
                System.out.println("(Client)Message from peer server: " + command.toJSONString());
                //out.write("Server Ack " + command.toJSONString() + "\n");
                //out.flush();
                //System.out.println("Reply sent");
                //Execute command
                //doCommand(command, out);

                if (command.get("command").toString().equals("HANDSHAKE_RESPONSE"))
                {
                    // Synchronizing Events after Handshake!!!
                    ArrayList<FileSystemManager.FileSystemEvent> sync = f.fileSystemManager.generateSyncEvents();
                    //System.out.println(sync);
                    FileSystemManager.FileSystemEvent currentEvent = null;
                    System.out.println("----------Synchronizing Events!!!----------");
                    while (sync.size() > 0)
                    {
                        System.out.println(currentEvent = sync.remove(0));
                        f.processFileSystemEvent(currentEvent);
                    }
                }

                if (command.get("command").toString().equals("FILE_BYTES_REQUEST"))
                {
                    // Unmarshall request
                    String pathName = command.get("pathName").toString();
                    JSONObject fd = (JSONObject) command.get("fileDescriptor");
                    String md5 = fd.get("md5").toString();
                    long lastModified = (long) fd.get("lastModified");
                    long fileSize = (long) fd.get("fileSize");
                    long position = (long) command.get("position");
                    long length = (long) command.get("length");

                    //Read file by
                    ByteBuffer buf = f.fileSystemManager.readFile(md5, position, length);
                    buf.rewind();
                    byte[] arr = new byte[buf.remaining()];
                    buf.get(arr, 0, arr.length);
                    String content = Base64.getEncoder().encodeToString(arr);

                    // Send BYTE
                    JSONObject rep = new JSONObject();
                    rep.put("command", "FILE_BYTES_RESPONSE");
                    rep.put("fileDescriptor", fd);
                    rep.put("pathName", pathName);
                    rep.put("position", 0);
                    rep.put("length", length);
                    rep.put("content", content);
                    rep.put("message", "successful read");
                    rep.put("status", true);
                    out.write(rep + "\n");
                    out.flush();
                }

            }

        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            if(e.toString().contains("ConnectException"))
            {
                System.out.println("Peer working as a server");
            }
            else
            {
                e.printStackTrace();
            }
        } catch (ParseException e)
        {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        finally
        {
            // Close the socket
            if (socket != null)
            {
                try
                {
                    socket.close();
                    System.out.println("client socket closed...");
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


    public JSONObject parseRequest(String eventString, String pathName)
    {
        JSONObject req = new JSONObject();
        if(eventString.equals("HANDSHAKE_REQUEST"))
        {
            req.put("command","HANDSHAKE_REQUEST");
            JSONObject hostPort = new JSONObject();
            hostPort.put("host",ip);
            hostPort.put("port",port);
            req.put("hostPort",hostPort);
        }
        else if (eventString.equals("DIRECTORY_CREATE_REQUEST"))
        {
            req.put("command","DIRECTORY_CREATE_REQUEST");
            req.put("pathName",pathName);
        }
        else if (eventString.equals("DIRECTORY_DELETE_REQUEST"))
        {
            req.put("command", "DIRECTORY_DELETE_REQUEST");
            req.put("pathName", pathName);
        }

        else
        {
            System.out.println("Wrong eventString!");
        }

        System.out.println(req.toJSONString());
        return req;
    }

}
