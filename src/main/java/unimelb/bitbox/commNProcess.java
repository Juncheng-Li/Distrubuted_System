package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.net.*;
import java.util.Timer;

public class commNProcess extends Thread
{
    private JSONObject command;
    private BufferedReader in;
    private BufferedWriter out;
    private ServerMain f;
    private Socket socket;

    public commNProcess(JSONObject command, Socket socket, ServerMain f) throws IOException
    {
        this.command = command;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
        this.out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"));
        this.f = f;
    }

    public void run()
    {
        try
        {


                // Handle DIRECTORY_CREATE_REQUEST
                if (command.get("command").toString().equals("DIRECTORY_CREATE_REQUEST"))
                {
                    int count = 0;
                    String[] folders;
                    String pathName = command.get("pathName").toString();

                    // Check if parent dir exists:
                    if (pathName.contains("/"))
                    {
                        //Split pathName with "/"
                        folders = pathName.split("/");
                        System.out.println("Path Spliter");
                        String temp = "";
                        for (int i = 0; i < folders.length - 1; i++)
                        {
                            temp = temp + folders[i];
                            //System.out.println(temp);
                            if (f.fileSystemManager.dirNameExists(temp))
                            {
                                count++;
                            }
                            temp = temp + "/";
                        }
                        count = count - (folders.length - 1);
                    }
                    //System.out.println(count);
                    try
                    {
                        // Check if parent dir exists
                        if (count == 0)
                        {
                            //System.out.println("correct");
                            if (f.fileSystemManager.isSafePathName(pathName))
                            {
                                if (f.fileSystemManager.dirNameExists(pathName))
                                {
                                    JSONObject reply = new JSONObject();
                                    reply.put("command", "DIRECTORY_CREATE_RESPONSE");
                                    reply.put("pathName", pathName);
                                    reply.put("message", "pathname already exists");
                                    reply.put("status", false);
                                    System.out.println(reply);
                                    out.write(reply + "\n");
                                    out.flush();
                                } else
                                {
                                    f.fileSystemManager.makeDirectory(pathName);
                                    JSONObject reply = new JSONObject();
                                    reply.put("command", "DIRECTORY_CREATE_RESPONSE");
                                    reply.put("pathName", pathName);
                                    reply.put("message", "directory created");
                                    reply.put("status", true);
                                    System.out.println(reply);
                                    out.write(reply + "\n");
                                    out.flush();
                                }
                            } else
                            {
                                JSONObject reply = new JSONObject();
                                reply.put("command", "DIRECTORY_CREATE_RESPONSE");
                                reply.put("pathName", pathName);
                                reply.put("message", "unsafe pathname given");
                                reply.put("status", false);
                                System.out.println(reply);
                                out.write(reply + "\n");
                                out.flush();
                            }
                        } else
                        {
                            JSONObject reply = new JSONObject();
                            reply.put("command", "DIRECTORY_CREATE_RESPONSE");
                            reply.put("pathName", pathName);
                            reply.put("message", "there was a problem creating the directory");
                            reply.put("status", false);
                            System.out.println(reply);
                            out.write(reply + "\n");
                            out.flush();
                        }
                    } catch (Exception e)
                    {
                        JSONObject reply = new JSONObject();
                        reply.put("command", "DIRECTORY_CREATE_RESPONSE");
                        reply.put("pathName", pathName);
                        reply.put("message", "there was a problem creating the directory");
                        reply.put("status", false);
                        System.out.println(reply);
                        out.write(reply + "\n");
                        out.flush();
                    }
                }
                // Handle DIRECTORY_DELETE_REQUEST
                else if (command.get("command").toString().equals("DIRECTORY_DELETE_REQUEST"))
                {
                    int count = 0;
                    String[] folders;
                    String pathName = command.get("pathName").toString();

                    // Check if parent dir exists:
                    if (pathName.contains("/"))
                    {
                        //Split pathName with "/"
                        folders = pathName.split("/");
                        System.out.println("Path Spliter");
                        String temp = "";
                        for (int i = 0; i < folders.length - 1; i++)
                        {
                            temp = temp + folders[i];
                            //System.out.println(element);
                            System.out.println(temp);
                            if (f.fileSystemManager.dirNameExists(temp))
                            {
                                count++;
                            }
                            temp = temp + "/";
                        }
                        count = count - (folders.length - 1);
                    }
                    //System.out.println(count);
                    try
                    {
                        if (count == 0)
                        {
                            if (f.fileSystemManager.isSafePathName(pathName))
                            {
                                if (f.fileSystemManager.dirNameExists(pathName))
                                {
                                    f.fileSystemManager.deleteDirectory(pathName);
                                    JSONObject reply = new JSONObject();
                                    reply.put("command", "DIRECTORY_DELETE_RESPONSE");
                                    reply.put("pathName", pathName);
                                    reply.put("message", "directory deleted");
                                    reply.put("status", true);
                                    System.out.println(reply);
                                    out.write(reply + "\n");
                                    out.flush();
                                } else
                                {
                                    JSONObject reply = new JSONObject();
                                    reply.put("command", "DIRECTORY_DELETE_RESPONSE");
                                    reply.put("pathName", pathName);
                                    reply.put("message", "pathname does not exist");
                                    reply.put("status", false);
                                    System.out.println(reply);
                                    out.write(reply + "\n");
                                    out.flush();
                                }
                            } else
                            {
                                JSONObject reply = new JSONObject();
                                reply.put("command", "DIRECTORY_DELETE_RESPONSE");
                                reply.put("pathName", pathName);
                                reply.put("message", "unsafe pathname given");
                                reply.put("status", false);
                                System.out.println(reply);
                                out.write(reply + "\n");
                                out.flush();
                            }
                        } else
                        {
                            JSONObject reply = new JSONObject();
                            reply.put("command", "DIRECTORY_DELETE_RESPONSE");
                            reply.put("pathName", pathName);
                            reply.put("message", "there was a problem deleting the directory");
                            reply.put("status", false);
                            System.out.println(reply);
                            out.write(reply + "\n");
                            out.flush();
                        }
                    } catch (Exception e)
                    {
                        System.out.println("There was a problem deleting the directory");
                        JSONObject reply = new JSONObject();
                        reply.put("command", "DIRECTORY_DELETE_RESPONSE");
                        reply.put("pathName", pathName);
                        reply.put("message", "there was a problem deleting the directory");
                        reply.put("status", false);
                        System.out.println(reply);
                        out.write(reply + "\n");
                        out.flush();
                    }
                }
                // Handle FILE_CREATE_REQUEST
                else if (command.get("command").toString().equals("FILE_CREATE_REQUEST"))
                {
                    String pathName = command.get("pathName").toString();
                    JSONObject fd = (JSONObject) command.get("fileDescriptor");
                    String md5 = fd.get("md5").toString();
                    long lastModified = (long) fd.get("lastModified");
                    long fileSize = (long) fd.get("fileSize");
                    if (f.fileSystemManager.isSafePathName(pathName))
                    {
                        if (f.fileSystemManager.fileNameExists(pathName, md5))    //should be if file already exists, should update or not? no need to delete
                        {                                                   //or should use checkShort cut to skip reCreating the file?
                            JSONObject reply = new JSONObject();
                            reply.put("command", "FILE_CREATE_RESPONSE");
                            reply.put("pathName", pathName);
                            reply.put("message", "file with same content already exists");
                            reply.put("status", false);
                            System.out.println(reply);
                            out.write(reply + "\n");
                            out.flush();
                        } else
                        {
                            // do here
                            // create fileLoader

                            try
                            {
                                f.fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);
                            } catch (NoSuchAlgorithmException e)
                            {
                                JSONObject reply = new JSONObject();
                                reply.put("command", "FILE_CREATE_RESPONSE");
                                reply.put("fileDescriptor", fd);
                                reply.put("pathName", pathName);
                                reply.put("message", "there was a problem creating file");
                                reply.put("status", false);
                                System.out.println(reply);
                                out.write(reply + "\n");
                                out.flush();
                                e.printStackTrace();
                            }

                            // FILE_CREATE_RESPONSE
                            JSONObject reply = new JSONObject();
                            reply.put("command", "FILE_CREATE_RESPONSE");
                            reply.put("fileDescriptor", fd);
                            reply.put("pathName", pathName);
                            reply.put("message", "file loader ready");
                            reply.put("status", true);
                            System.out.println(reply);
                            out.write(reply + "\n");
                            out.flush();
                            // FILE_BYTES_REQUEST
                            if (fileSize <= Long.parseLong(Configuration.getConfigurationValue("blockSize")))
                            {
                                JSONObject req = new JSONObject();
                                req.put("command", "FILE_BYTES_REQUEST");
                                req.put("fileDescriptor", fd);
                                req.put("pathName", pathName);
                                req.put("position", 0);
                                req.put("length", fileSize);
                                System.out.println(reply);
                                out.write(req + "\n");
                                out.flush();
                            } else
                            {
                                long remainingSize = fileSize;
                                long position = 0;
                                while (remainingSize > Long.parseLong(Configuration.getConfigurationValue("blockSize")))
                                {
                                    System.out.println("Large file transfering!");
                                    JSONObject req = new JSONObject();
                                    req.put("command", "FILE_BYTES_REQUEST");
                                    req.put("fileDescriptor", fd);
                                    req.put("pathName", pathName);
                                    req.put("position", position);
                                    req.put("length", Long.parseLong(Configuration.getConfigurationValue("blockSize")));
                                    System.out.println(reply);
                                    out.write(req + "\n");
                                    out.flush();
                                    // Update position
                                    position = position + Long.parseLong(Configuration.getConfigurationValue("blockSize"));
                                    remainingSize = remainingSize - Long.parseLong(Configuration.getConfigurationValue("blockSize"));
                                }
                                if (remainingSize != 0)
                                {
                                    JSONObject req = new JSONObject();
                                    req.put("command", "FILE_BYTES_REQUEST");
                                    req.put("fileDescriptor", fd);
                                    req.put("pathName", pathName);
                                    req.put("position", position);
                                    req.put("length", remainingSize);
                                    System.out.println(reply);
                                    out.write(req + "\n");
                                    out.flush();
                                }
                            }
                        }
                    } else
                    {
                        JSONObject reply = new JSONObject();
                        reply.put("command", "FILE_CREATE_RESPONSE");
                        reply.put("pathName", pathName);
                        reply.put("message", "unsafe pathname given");
                        reply.put("status", false);
                        System.out.println(reply);
                        out.write(reply + "\n");
                        out.flush();
                    }
                }
                // Handle FILE_BYTES_RESPONSE
                else if (command.get("command").toString().equals("FILE_BYTES_RESPONSE"))
                {
                    String pathName = command.get("pathName").toString();
                    String content = command.get("content").toString();
                    long position = (long) command.get("position");
                    //convert contentStr to byte then put into buffer
                    byte[] arr = Base64.getDecoder().decode(content);
                    ByteBuffer buf = ByteBuffer.wrap(arr);
                    f.fileSystemManager.writeFile(pathName, buf, position);
                    if (f.fileSystemManager.checkWriteComplete(pathName))
                    {
                        System.out.println(pathName + " write complete!");
                    }
                }
                //Handle FILE_DELETE_REQUEST
                else if (command.get("command").toString().equals("FILE_DELETE_REQUEST"))
                {
                    String pathName = command.get("pathName").toString();
                    JSONObject fd = (JSONObject) command.get("fileDescriptor");
                    String md5 = fd.get("md5").toString();
                    long lastModified = (long) fd.get("lastModified");
                    try
                    {
                        // check safe pathname
                        if (f.fileSystemManager.isSafePathName(pathName))
                        {
                            // check if dir exists
                            if (f.fileSystemManager.fileNameExists(pathName))
                            {
                                if (f.fileSystemManager.fileNameExists(pathName, md5))
                                {
                                    boolean ok = f.fileSystemManager.deleteFile(pathName, lastModified, md5);
                                    if (ok)
                                    {
                                        JSONObject reply = new JSONObject();
                                        reply.put("command", "FILE_DELETE_RESPONSE");
                                        reply.put("fileDescriptor", fd);
                                        reply.put("pathName", pathName);
                                        reply.put("message", "file deleted");
                                        reply.put("status", true);
                                        System.out.println(reply);
                                        out.write(reply + "\n");
                                        out.flush();
                                    } else
                                    {
                                        JSONObject reply = new JSONObject();
                                        reply.put("command", "FILE_DELETE_RESPONSE");
                                        reply.put("fileDescriptor", fd);
                                        reply.put("pathName", pathName);
                                        reply.put("message", "there is a problem deleting the file");
                                        reply.put("status", false);
                                        System.out.println(reply);
                                        out.write(reply + "\n");
                                        out.flush();
                                    }
                                } else
                                {
                                    JSONObject reply = new JSONObject();
                                    reply.put("command", "FILE_DELETE_RESPONSE");
                                    reply.put("fileDescriptor", fd);
                                    reply.put("pathName", pathName);
                                    reply.put("message", "md5 does not match");
                                    reply.put("status", false);
                                    System.out.println(reply);
                                    out.write(reply + "\n");
                                    out.flush();
                                }
                            } else
                            {
                                JSONObject reply = new JSONObject();
                                reply.put("command", "FILE_DELETE_RESPONSE");
                                reply.put("fileDescriptor", fd);
                                reply.put("pathName", pathName);
                                reply.put("message", "pathname does not exist");
                                reply.put("status", false);
                                System.out.println(reply);
                                out.write(reply + "\n");
                                out.flush();
                            }
                        } else
                        {
                            JSONObject reply = new JSONObject();
                            reply.put("command", "FILE_DELETE_RESPONSE");
                            reply.put("fileDescriptor", fd);
                            reply.put("pathName", pathName);
                            reply.put("message", "unsafe pathname given");
                            reply.put("status", false);
                            System.out.println(reply);
                            out.write(reply + "\n");
                            out.flush();
                        }
                    } catch (Exception e)
                    {
                        System.out.println("There was a problem deleting the file");
                        JSONObject reply = new JSONObject();
                        reply.put("command", "FILE_DELETE_RESPONSE");
                        reply.put("fileDescriptor", fd);
                        reply.put("pathName", pathName);
                        reply.put("message", "There was a problem deleting the file");
                        reply.put("status", false);
                        System.out.println(reply);
                        out.write(reply + "\n");
                        out.flush();
                    }
                }
                //Handle FILE_MODIFY_REQUEST
                else if (command.get("command").toString().equals("FILE_MODIFY_REQUEST"))
                {
                    String pathName = command.get("pathName").toString();
                    JSONObject fd = (JSONObject) command.get("fileDescriptor");
                    String md5 = fd.get("md5").toString();
                    long lastModified = (long) fd.get("lastModified");
                    long fileSize = (long) fd.get("fileSize");
                    if (f.fileSystemManager.modifyFileLoader(pathName, md5, lastModified))
                    {
                        //FILE_MODIFY_RESPONSE
                        JSONObject reply = new JSONObject();
                        reply.put("command", "FILE_MODIFY_RESPONSE");
                        reply.put("fileDescriptor", fd);
                        reply.put("pathName", pathName);
                        reply.put("message", "file loader ready");
                        reply.put("status", true);
                        System.out.println(reply);
                        out.write(reply + "\n");
                        out.flush();
                        //FILE_BYTES_REQUEST
                        if (fileSize <= Long.parseLong(Configuration.getConfigurationValue("blockSize")))
                        {
                            JSONObject req = new JSONObject();
                            req.put("command", "FILE_BYTES_REQUEST");
                            req.put("fileDescriptor", fd);
                            req.put("pathName", pathName);
                            req.put("position", 0);
                            req.put("length", fileSize);
                            System.out.println(reply);
                            out.write(req + "\n");
                            out.flush();
                        } else
                        {
                            long remainingSize = fileSize;
                            long position = 0;
                            while (remainingSize > Long.parseLong(Configuration.getConfigurationValue("blockSize")))
                            {
                                System.out.println("Large file transfering!");
                                JSONObject req = new JSONObject();
                                req.put("command", "FILE_BYTES_REQUEST");
                                req.put("fileDescriptor", fd);
                                req.put("pathName", pathName);
                                req.put("position", position);
                                req.put("length", Long.parseLong(Configuration.getConfigurationValue("blockSize")));
                                System.out.println(reply);
                                out.write(req + "\n");
                                out.flush();
                                // Update position
                                position = position + Long.parseLong(Configuration.getConfigurationValue("blockSize"));
                                remainingSize = remainingSize - Long.parseLong(Configuration.getConfigurationValue("blockSize"));
                            }
                            if (remainingSize != 0)
                            {
                                System.out.println(fileSize);
                                System.out.println(position);
                                System.out.println(remainingSize);
                                JSONObject req = new JSONObject();
                                req.put("command", "FILE_BYTES_REQUEST");
                                req.put("fileDescriptor", fd);
                                req.put("pathName", pathName);
                                req.put("position", position);
                                req.put("length", remainingSize);
                                System.out.println(reply);
                                out.write(req + "\n");
                                out.flush();
                            }
                        }
                    } else
                    {
                        JSONObject reply = new JSONObject();
                        reply.put("command", "FILE_MODIFY_RESPONSE");
                        reply.put("fileDescriptor", fd);
                        reply.put("pathName", pathName);
                        reply.put("message", "file with same content");
                        reply.put("status", false);
                        System.out.println(reply);
                        out.write(reply + "\n");
                        out.flush();
                    }
                }
                // Moved from client
                else if (command.get("command").toString().equals("FILE_BYTES_REQUEST"))
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
                            /*
                            if (fileSize < length)
                            {
                                length = fileSize;
                            }
                            */
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
                    rep.put("position", position);    //changed position from 0 to position
                    rep.put("length", length);
                    rep.put("content", content);
                    rep.put("message", "successful read");
                    rep.put("status", true);
                    System.out.println(rep);
                    out.write(rep + "\n");
                    out.flush();
                } else if (command.get("command").toString().equals("DIRECTORY_CREATE_RESPONSE") ||
                        command.get("command").toString().equals("DIRECTORY_DELETE_RESPONSE") ||
                        command.get("command").toString().equals("FILE_DELETE_RESPONSE") ||
                        command.get("command").toString().equals("FILE_CREATE_RESPONSE") ||
                        command.get("command").toString().equals("FILE_MODIFY_RESPONSE"))
                {
                    // Do nothing
                }
                // If command is invalid
                else
                {
                    System.out.println("INVALID_PROTOCOL");
                    JSONObject reply = new JSONObject();
                    reply.put("command", "INVALID_PROTOCOL");
                    reply.put("message", "message must contain a command field as string");
                    System.out.println(reply);
                    out.write(reply + "\n");
                    out.flush();
                }

        } catch (SocketException e)
        {
            System.out.println("catched!!!!!!!!!!!!!!!!!!!!!!!!!");
            try
            {
                System.out.println(e.toString());
                socket.close();
                System.out.println("Socket closed.");
            } catch (IOException ee)
            {
                ee.printStackTrace();
            }
        } catch (IOException e)
        {
            System.out.println("catch!!!!!!!!!!!!!!!!!");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

}