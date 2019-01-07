package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import protocol.Protocol;

/**
 * Represents each connection from a client (whether from the web server or a store client).
 * Forwards all incoming data to the Server object to be processed and forwarded to appropriate clients
 * @author Bespoke Burgers
 *
 */
public class ServerConnection extends Thread {
    private Server server;
    private Socket socket;
    private boolean isRunning;
    private int id;
    private short registeredTo;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Constructor
     * @param socket Socket: The socket for this connection
     * @param server Server: The main server object
     */
    public ServerConnection(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;
            this.isRunning = true;
            this.registeredTo = Server.NONE;
            this.id = socket.getPort();
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream());
            System.out.println("Client accepted: " + id);
        } catch (IOException e) {
            System.err.println("Client could not initiate");
            e.printStackTrace();
        }
    }
    
    /**
     * Runs on a separate thread to the rest of the server.
     * Incoming data will be caught here and forwarded to the main Server object for processing.
     */
    @Override
    public void run() {
        try {
            while (server.isRunning() && this.isRunning) {
                String message = in.readLine();
                server.process(id, registeredTo, message);
                if (message.startsWith(Protocol.DEREGISTER)) {
                    this.registeredTo = Server.NONE;
                    this.isRunning = false;
                    out.println(Protocol.ACKNOWLEDGE_DISCONNECT);
                    out.flush();
                }
            }
            close();
        } catch (IOException e) {
            System.err.printf("Error reading input from %d\n", id);
            e.printStackTrace();
        }
    }
    
    /**
     * Closes this connection
     * @throws IOException 
     */
    public void close() throws IOException {
        socket.close();
        System.out.println("Client closed: " + id);
    }
    
    /**
     * Registers this connection to Server.WEB/SHOP
     * @param registerTo short: corresponds to Server.WEB or Server.SHOP
     */
    public void register(short registerTo) {
        this.registeredTo = registerTo;
    }
    
    /**
     * Returns the writer for this connection
     * @return the writer for this connection
     */
    public PrintWriter getWriter() {
        return out;
    }

}
