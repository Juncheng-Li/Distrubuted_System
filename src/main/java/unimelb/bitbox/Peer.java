package unimelb.bitbox;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.HostPort;

import javax.crypto.*;
import java.net.*;

public class Peer
{
    private static Logger log = Logger.getLogger(Peer.class.getName());

    public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();

        Security.addProvider(new BouncyCastleProvider());
        JSONParser parser = new JSONParser();
        String peers = Configuration.getConfigurationValue("peers");
        String[] peersArray = peers.split(", ");
        connectedPeers TCPconnectedList = new connectedPeers();

        int udpServerPort = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        DatagramSocket dsServerSocket = new DatagramSocket(udpServerPort); //
        ackStorage as = new ackStorage();

        socketStorage ss = new socketStorage();
        ServerMain f = new ServerMain(ss, dsServerSocket, as);

        // Peer part
        if (Configuration.getConfigurationValue("mode").equals("tcp"))
        {
            // Peer_clientSide Start here
            for (String peer : peersArray)
            {
                try
                {
                    //System.out.println(peer);
                    HostPort peer_hp = new HostPort(peer);
                    Socket socket = new Socket(peer_hp.host, peer_hp.port);
                    ss.add(socket);
                    //System.out.println(ss.getSockets());
                    TCPconnectedList.add((JSONObject) parser.parse(peer_hp.toDoc().toJson()));
                    Peer_clientSide T_client = new Peer_clientSide(socket, f, ss);
                    T_client.start();
                }
                catch (IOException e)
                {
                    System.out.println(peer + " cannot be connected.");
                }
                catch (ParseException e)
                {
                    e.printStackTrace();
                }
            }

            // Peer_serverSide Start here
            listening Listening = new listening(ss, f);
            Listening.start();
        } else if (Configuration.getConfigurationValue("mode").equals("udp"))
        {
            // UDP clientServer
            udpClientServer udpCS_T = new udpClientServer(f, ss, dsServerSocket, as);
            udpCS_T.start();
        } else
        {
            System.out.println("Wrong mode: please choose udp or tcp");
        }


        // Server for Client

        ServerSocket listeningSocket_client = null;
        Socket clientSocket_client = null;
        try
        {
            listeningSocket_client = new ServerSocket(Integer.parseInt(Configuration.getConfigurationValue("clientPort")));
            while (true)
            {
                System.out.println("listening on clientPort " +
                        Integer.parseInt(Configuration.getConfigurationValue("clientPort")));
                clientSocket_client = listeningSocket_client.accept();
                System.out.println("Secure Client accepted.");

                // Server for Client
                try
                {
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket_client.getInputStream(), "UTF-8"));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket_client.getOutputStream(), "UTF-8"));
                    SecretKey secretKey = null;

                    String clientMsg = null;
                    while ((clientMsg = in.readLine()) != null)
                    {
                        JSONObject command = (JSONObject) parser.parse(clientMsg);
                        System.out.println("(Server for secure Client)Message from Client: " + command.toJSONString());
                        //Make sure it is an JSONObject
                        if (command.getClass().getName().equals("org.json.simple.JSONObject"))
                        {
                            //Handle encryptedCommand
                            if (command.containsKey("payload"))
                            {
                                if (secretKey != null)
                                {
                                    // Firstly, decrypt the command
                                    JSONObject decryptedCommand = wrapPayload.unWrap(command, secretKey);
                                    // Handle LIST_PEERS_REQUEST
                                    if (decryptedCommand.get("command").equals("LIST_PEERS_REQUEST"))
                                    {
                                        if (Configuration.getConfigurationValue("mode").equals("tcp"))
                                        {

                                            // Send reply
                                            JSONObject reply = new JSONObject();
                                            reply.put("command", "LIST_PEERS_RESPONSE");
                                            //reply.put("peers", TCPconnectedList.getStringList());
                                            reply.put("peers", ss.getTcpList());
                                            System.out.println("Sent encrypted: " + reply);
                                            out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                            out.flush();
                                        }
                                        else if (Configuration.getConfigurationValue("mode").equals("udp"))
                                        {
                                            // Send reply
                                            JSONObject reply = new JSONObject();
                                            reply.put("command", "LIST_PEERS_RESPONSE");
                                            reply.put("peers", ss.getUdpList());
                                            System.out.println("Sent encrypted: " + reply);
                                            out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                            out.flush();
                                        }
                                        else
                                        {
                                            System.out.println("Wrong mode - please choose either tcp or udp");
                                        }
                                    }
                                    // Handle CONNECT_PEER_REQUEST
                                    else if (decryptedCommand.get("command").equals("CONNECT_PEER_REQUEST"))
                                    {
                                        //Connect peer
                                        try
                                        {
                                            if (Configuration.getConfigurationValue("mode").equals("tcp"))
                                            {
                                                //Connect tcp
                                                String host = decryptedCommand.get("host").toString();
                                                int port = Integer.parseInt(decryptedCommand.get("port").toString());
                                                Socket socket = new Socket(host, port);
                                                System.out.println(host + ":" + port + " successfully connected.");
                                                //add to successful connected peerList
                                                ss.add(socket);
                                                JSONObject peer = new JSONObject();
                                                peer.put("host", decryptedCommand.get("host").toString());
                                                peer.put("port", Integer.parseInt(decryptedCommand.get("port").toString()));
                                                TCPconnectedList.add(peer);
                                                //Start thread
                                                Peer_clientSide T_client = new Peer_clientSide(socket, f, ss);
                                                T_client.start();
                                                //reply
                                                JSONObject reply = new JSONObject();
                                                reply.put("command", "CONNECT_PEER_RESPONSE");
                                                reply.put("host", host);
                                                reply.put("port", port);
                                                reply.put("status", true);
                                                reply.put("message", "connected to peer");
                                                System.out.println("Sent encrypted: " + reply);
                                                out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                out.flush();
                                            }
                                            else if (Configuration.getConfigurationValue("mode").equals("udp"))
                                            {
                                                //Connect udp
                                                String host = null;
                                                if (decryptedCommand.get("host").toString().equals("localhost"))
                                                {
                                                    host = InetAddress.getByName(decryptedCommand.get("host").toString()).getHostAddress();
                                                }
                                                else
                                                {
                                                    host = decryptedCommand.get("host").toString();
                                                }
                                                int udpPort = Integer.parseInt(decryptedCommand.get("port").toString());
                                                HostPort udpSocket = new HostPort(host, udpPort);
                                                //ss.add(udpSocket);
                                                int num = ss.getUdpSockets().size();
                                                if (num < Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections")))
                                                {
                                                    //handshake
                                                    byte[] buf = null;
                                                    InetAddress ip = InetAddress.getByName(host);
                                                    JSONObject hs = new JSONObject();
                                                    JSONObject hostPort = new JSONObject();
                                                    hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
                                                    hostPort.put("port", udpServerPort);
                                                    hs.put("command", "HANDSHAKE_REQUEST");
                                                    hs.put("hostPort", hostPort);
                                                    buf = hs.toJSONString().getBytes();
                                                    DatagramPacket packet = new DatagramPacket(buf, buf.length,ip, udpPort);
                                                    dsServerSocket.send(packet);  //IOException  //need "/n" ?              //////
                                                    System.out.println("udp sent " + ip.getHostAddress() + ":" + udpPort + " : " + hs);

                                                    try
                                                    {
                                                        Thread.sleep(500);
                                                    }
                                                    catch (InterruptedException e)
                                                    {
                                                        System.out.println("Thread interrupted.");
                                                    }
                                                    int secondNum = ss.getUdpSockets().size();
                                                    if (secondNum > num)
                                                    {
                                                        //reply
                                                        JSONObject reply = new JSONObject();
                                                        reply.put("command", "CONNECT_PEER_RESPONSE");
                                                        reply.put("host", host);
                                                        reply.put("port", udpPort);
                                                        reply.put("status", true);
                                                        reply.put("message", "connected to peer");
                                                        System.out.println("Sent encrypted: " + reply);
                                                        out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                        out.flush();
                                                    }
                                                    else
                                                    {
                                                        //reply
                                                        JSONObject reply = new JSONObject();
                                                        reply.put("command", "CONNECT_PEER_RESPONSE");
                                                        reply.put("host", host);
                                                        reply.put("port", udpPort);
                                                        reply.put("status", false);
                                                        reply.put("message", "connection failed");
                                                        System.out.println("Sent encrypted: " + reply);
                                                        out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                        out.flush();
                                                    }
                                                }
                                                else
                                                {
                                                    //reply
                                                    JSONObject reply = new JSONObject();
                                                    reply.put("command", "CONNECT_PEER_RESPONSE");
                                                    reply.put("host", host);
                                                    reply.put("port", udpPort);
                                                    reply.put("status", false);
                                                    reply.put("message", "connection failed, maximum reached");
                                                    System.out.println("Sent encrypted: " + reply);
                                                    out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                    out.flush();
                                                }

                                            }
                                            else
                                            {
                                                System.out.println("Wrong mode - choose either tcp or udp");
                                            }
                                        }
                                        // If connection unsuccessful
                                        catch (IOException e)
                                        {
                                            System.out.println(decryptedCommand.get("host").toString() + ":" +
                                                    decryptedCommand.get("port").toString() + " cannot be connected.");
                                            //reply
                                            JSONObject reply = new JSONObject();
                                            reply.put("command", "CONNECT_PEER_RESPONSE");
                                            reply.put("host", decryptedCommand.get("host").toString());
                                            reply.put("port", Integer.parseInt(decryptedCommand.get("port").toString()));
                                            reply.put("status", false);
                                            reply.put("message", "connection failed");
                                            System.out.println("Sent encrypted: " + reply);
                                            out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                            out.flush();
                                        }
                                    }
                                    // Handle DISCONNECT_PEER_REQUEST
                                    else if (decryptedCommand.get("command").equals("DISCONNECT_PEER_REQUEST"))
                                    {
                                        if (Configuration.getConfigurationValue("mode").equals("tcp"))
                                        {
                                            //Disconnect peer
                                            String host = decryptedCommand.get("host").toString();
                                            int port = Integer.parseInt(decryptedCommand.get("port").toString());
                                            boolean contains = ss.contains(host, port);
                                            if (contains)
                                            {
                                                ss.disNremove(host, port);
                                                //reply
                                                JSONObject reply = new JSONObject();
                                                reply.put("command", "DISCONNECT_PEER_RESPONSE");
                                                reply.put("host", host);
                                                reply.put("port", port);
                                                reply.put("status", true);
                                                reply.put("message", "disconnected from peer");
                                                System.out.println("Sent encrypted: " + reply);
                                                out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                out.flush();
                                            }
                                            else
                                            {
                                                System.out.println("The peer want to disconnect does not exist in peer");
                                                JSONObject reply = new JSONObject();
                                                reply.put("command", "DISCONNECT_PEER_RESPONSE");
                                                reply.put("host", host);
                                                reply.put("port", port);
                                                reply.put("status", false);
                                                reply.put("message", "connection not active");
                                                System.out.println("Sent encrypted: " + reply);
                                                out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                out.flush();
                                            }
                                        }
                                        else if (Configuration.getConfigurationValue("mode").equals("udp"))
                                        {
                                            String host = decryptedCommand.get("host").toString();
                                            int port = Integer.parseInt(decryptedCommand.get("port").toString());
                                            HostPort temp = new HostPort(host + ":" + port);
                                            boolean contains = ss.contains(temp);
                                            if (contains)
                                            {
                                                ss.remove("udp", temp.toString());
                                                //reply
                                                JSONObject reply = new JSONObject();
                                                reply.put("command", "DISCONNECT_PEER_RESPONSE");
                                                reply.put("host", host);
                                                reply.put("port", port);
                                                reply.put("status", true);
                                                reply.put("message", "disconnected from peer");
                                                System.out.println("Sent encrypted: " + reply);
                                                out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                out.flush();
                                            }
                                            else
                                            {
                                                System.out.println("The peer want to disconnect does not exist in peer");
                                                JSONObject reply = new JSONObject();
                                                reply.put("command", "DISCONNECT_PEER_RESPONSE");
                                                reply.put("host", host);
                                                reply.put("port", port);
                                                reply.put("status", false);
                                                reply.put("message", "connection not active");
                                                System.out.println("Sent encrypted: " + reply);
                                                out.write(wrapPayload.payload(reply, secretKey).toJSONString() + "\n");
                                                out.flush();
                                            }
                                        }
                                    }
                                } else
                                {
                                    System.out.println("Secret key is null!");
                                }
                            } else
                            {
                                //Handle AUTH_RESPONSE
                                if (command.get("command").toString().equals("AUTH_REQUEST"))
                                {
                                    String id = command.get("identity").toString();
                                    String[] keys = Configuration.getConfigurationValue("authorized_keys").split(",");
                                    boolean ifContains = false;
                                    for (String key : keys)
                                    {
                                        //See if public key exists
                                        String key_id = key.split(" ")[2];
                                        if (key_id.equals(id))
                                        {
                                            ifContains = true;
                                            System.out.println("Identity exists, creating AES");
                                            //generate AES
                                            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                                            keyGen.init(128);
                                            secretKey = keyGen.generateKey();
                                            //take public key out
                                            PublicKey pubKey = decodeKey.decodeOpenSSH(key);
                                            //encrypt AES with public key
                                            SecureRandom random = new SecureRandom();
                                            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                            cipher.init(Cipher.ENCRYPT_MODE, pubKey, random);
                                            byte[] encryptedAES = cipher.doFinal(secretKey.getEncoded());
                                            String encryptedAES_Base64 = Base64.getEncoder().encodeToString(encryptedAES);
                                            //reply with AES secret key
                                            JSONObject reply = new JSONObject();
                                            reply.put("command", "AUTH_RESPONSE");
                                            reply.put("AES128", encryptedAES_Base64);
                                            reply.put("status", true);
                                            reply.put("message", "public key found");
                                            System.out.println("Sent: " + reply);
                                            out.write(reply.toJSONString() + "\n");
                                            out.flush();
                                        }
                                    }
                                    if (!ifContains)
                                    {
                                        //reply pub key not found
                                        JSONObject reply = new JSONObject();
                                        reply.put("command", "AUTH_RESPONSE");
                                        reply.put("status", false);
                                        reply.put("message", "public key found");
                                        System.out.println("Sent: " + reply);
                                        out.write(reply.toJSONString() + "\n");
                                        out.flush();
                                    }
                                } else
                                {
                                    //other options
                                    System.out.println("Received unknown request");
                                }
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
                } catch (IOException e)
                {
                    //e.printStackTrace();
                } catch (ParseException e)
                {
                    System.out.println("Parse Invalid letter at " + e.getPosition());
                    //e.printStackTrace();
                } catch (NoSuchProviderException e)
                {
                    System.out.println("No such security provider");
                    //e.printStackTrace();
                } catch (NoSuchAlgorithmException e)
                {
                    //e.printStackTrace();
                } catch (NoSuchPaddingException e)
                {
                    //e.printStackTrace();
                } catch (IllegalBlockSizeException e)
                {
                    System.out.println("Illigual block size");
                    //e.printStackTrace();
                } catch (BadPaddingException e)
                {
                    //e.printStackTrace();
                } catch (InvalidKeyException e)
                {
                    System.out.println("Invalid key..");
                    //e.printStackTrace();
                } catch (InvalidKeySpecException e)
                {
                    System.out.println("Invalid KeySpec");
                    //e.printStackTrace();
                } catch (NullPointerException e)
                {
                    System.out.println("Client invalid request");
                }
            }
        } catch (SocketException ex)
        {
            //ex.printStackTrace();
        } catch (IOException e)
        {
            //e.printStackTrace();
        } finally
        {
            if (listeningSocket_client != null)
            {
                try
                {
                    listeningSocket_client.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


    private static void removeSocket(ArrayList<Socket> socketList, JSONArray connectedPeer, LinkedList<Integer> removeIndex)
    {
        if (removeIndex.size() > 0)
        {
            removeIndex.sort(Comparator.reverseOrder());
            for (int i = 0; i < removeIndex.size(); i++)
            {
                int ind = Integer.parseInt(removeIndex.pop().toString());
                socketList.remove(ind);
                connectedPeer.remove(ind);
            }
        }
    }
}
