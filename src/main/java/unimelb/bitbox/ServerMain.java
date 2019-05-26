package unimelb.bitbox;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver
{
    private static Logger log = Logger.getLogger(ServerMain.class.getName());
    private BufferedReader in;
    private BufferedWriter out;
    protected FileSystemManager fileSystemManager;
    private Socket socket = null;
    private InetAddress udpIP;
    private int udpPort;

    private ArrayList<Socket> sockets;
    socketStorage ss;
    /*
    public ServerMain(Socket socket) throws NumberFormatException, IOException, NoSuchAlgorithmException
    {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
        this.out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));

        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
    }
     */

    public ServerMain(InetAddress udpIP, int udpPort) throws IOException, NoSuchAlgorithmException
    {
        this.udpIP = udpIP;
        this.udpPort = udpPort;
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
    }

    public ServerMain(socketStorage ss) throws IOException, NoSuchAlgorithmException
    {
        this.ss = ss;
        fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
    }

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent)
    {
        // TODO: process events

        try
        {
            if (fileSystemEvent.event.toString().equals("FILE_CREATE"))
            {
                //Ask to create file loader
                JSONObject req = new JSONObject();
                JSONObject fileDescriptor = new JSONObject();
                fileDescriptor.put("md5", fileSystemEvent.fileDescriptor.md5);
                fileDescriptor.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
                fileDescriptor.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);
                req.put("command", "FILE_CREATE_REQUEST");
                req.put("fileDescriptor", fileDescriptor);
                req.put("pathName", fileSystemEvent.pathName);
                if (Configuration.getConfigurationValue("mode").equals("tcp"))
                {
                    send(req, ss);
                }
                else if (Configuration.getConfigurationValue("mode").equals("udp"))
                {
                    send(req, udpIP, udpPort);
                }
                else
                {
                    System.out.println("wrong mode!");
                }
            }

            if (fileSystemEvent.event.toString().equals("FILE_DELETE"))
            {
                //Destination remove file
                JSONObject req = new JSONObject();
                JSONObject fileDescriptor = new JSONObject();
                fileDescriptor.put("md5", fileSystemEvent.fileDescriptor.md5);
                fileDescriptor.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
                fileDescriptor.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);
                req.put("command", "FILE_DELETE_REQUEST");
                req.put("fileDescriptor", fileDescriptor);
                req.put("pathName", fileSystemEvent.pathName);
                if (Configuration.getConfigurationValue("mode").equals("tcp"))
                {
                    send(req, ss);
                }
                else if (Configuration.getConfigurationValue("mode").equals("udp"))
                {
                    send(req, udpIP, udpPort);
                }
                else
                {
                    System.out.println("wrong mode!");
                }
            }

            if (fileSystemEvent.event.toString().equals("FILE_MODIFY"))
            {
                //System.out.println("Yes, there is a file modified");
                JSONObject req = new JSONObject();
                JSONObject fileDescriptor = new JSONObject();
                fileDescriptor.put("md5", fileSystemEvent.fileDescriptor.md5);
                fileDescriptor.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
                fileDescriptor.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);
                req.put("command", "FILE_MODIFY_REQUEST");
                req.put("fileDescriptor", fileDescriptor);
                req.put("pathName", fileSystemEvent.pathName);
                if (Configuration.getConfigurationValue("mode").equals("tcp"))
                {
                    send(req, ss);
                }
                else if (Configuration.getConfigurationValue("mode").equals("udp"))
                {
                    send(req, udpIP, udpPort);
                }
                else
                {
                    System.out.println("wrong mode!");
                }
            }

            if (fileSystemEvent.event.toString().equals("DIRECTORY_CREATE"))
            {
                //Destination create dir
                JSONObject req = new JSONObject();
                String pathName = fileSystemEvent.pathName;
                req.put("command", "DIRECTORY_CREATE_REQUEST");
                req.put("pathName", pathName);
                if (Configuration.getConfigurationValue("mode").equals("tcp"))
                {
                    send(req, ss);
                }
                else if (Configuration.getConfigurationValue("mode").equals("udp"))
                {
                    send(req, udpIP, udpPort);
                }
                else
                {
                    System.out.println("wrong mode!");
                }
            }

            if (fileSystemEvent.event.toString().equals("DIRECTORY_DELETE"))
            {
                //Destination delete dir
                JSONObject req = new JSONObject();
                String pathName = fileSystemEvent.pathName;
                req.put("command", "DIRECTORY_DELETE_REQUEST");
                req.put("pathName", pathName);
                if (Configuration.getConfigurationValue("mode").equals("tcp"))
                {
                    send(req, ss);
                }
                else if (Configuration.getConfigurationValue("mode").equals("udp"))
                {
                    send(req, udpIP, udpPort);
                }
                else
                {
                    System.out.println("wrong mode!");
                }
            }
        }
        catch (IOException e)
        {
            if (e.toString().equals("java.net.SocketException: Socket closed"))
            {
                try
                {
                    socket.close();
                    System.out.println("Socket closed!");
                }
                catch (IOException es)
                {
                    es.printStackTrace();
                }
            } else
            {
                System.out.println(e.toString());
                e.printStackTrace();
            }
        }
    }


    public static void send(JSONObject message, InetAddress ip, int port) throws IOException
    {
        DatagramSocket dsSocket = new DatagramSocket();
        byte[] buf = message.toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, ip, port);
        dsSocket.send(packet);
        System.out.println("udp sent: " + message.toJSONString());
    }

    private static void send(JSONObject message, socketStorage ss) throws IOException
    {
        for (Socket socket : ss.getSockets())
        {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            out.write(message.toJSONString() + "\n");
            out.flush();
            System.out.println("sent: " + message.toJSONString());
        }
    }


}
