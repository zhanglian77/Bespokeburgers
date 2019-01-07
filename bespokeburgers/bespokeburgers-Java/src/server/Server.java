package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

import static protocol.Protocol.*;

/**
 * Main entry point for the Server-side.
 * Listens for incoming client connections and classifies them appropriately as a web connection or a store connection.
 * Processes all incoming data from ServerConnection objects and forwards appropriately to other clients and/or the Database.
 * @author Bespoke Burgers
 *
 */
public class Server implements Runnable {
    static final short NONE = 50;
    static final short SHOP = 51;
    static final short WEB = 52;
    private final short ALL = -53;
    private final int PORT = 9090;
    private volatile boolean isRunning;
    private ServerSocket listener;
    private Map<Integer, ServerConnection> unregistered;
    private Map<Integer, PrintWriter> webOut;
    private Map<Integer, PrintWriter> shopOut;
    private int lastOrder = 0;
    private String previousOrderTime;
    
    public static void main(String[] args) throws IOException {
        try {
            new Server();
        } catch (IOException e) {
            System.err.println("Could not start server");
            throw(e);
        }
    }
    
    /**
     * Default constructor which also spins up a new thread
     * @throws IOException 
     */
    public Server() throws IOException {
        this.webOut = new HashMap<Integer, PrintWriter>();
        this.shopOut = new HashMap<Integer, PrintWriter>();
        this.unregistered = new HashMap<Integer, ServerConnection>();
        listener = new ServerSocket(PORT);
        this.isRunning = true;
        setOrderNum();
        new Thread(this).start();
        System.out.println("Server listening on port " + PORT);
    }
    
    private void setOrderNum() {
        String[] lastOrder = Database.getLatestOrder().split(DELIM);
//        System.out.println(lastOrder[0]+" "+lastOrder[1]);
        this.lastOrder = Integer.parseInt(lastOrder[0]);
        this.previousOrderTime = lastOrder[1];
    }

    /**
     * Listens for incoming connections and instantiates ServerConnection objects for them
     */
    @Override
    public void run() {
        while (this.isRunning) {
        try {
            Socket socket = listener.accept();
            ServerConnection connection = new ServerConnection(socket, this);
            connection.start();
            unregistered.put(socket.getPort(), connection);
        } catch (IOException e) {
            close();
            e.printStackTrace();
        }
    }
    }

    /**
     * Processes incoming data from ServerConnection objects
     * @param id int: id corresponding to the connection
     * @param input String: raw data as-sent from the client
     */
    public void process(int id, short registeredTo, String input) {
        String[] tokens = input.split(DELIM);
        String protocol = tokens[0];
//        System.out.println(protocol);
        
        if (registeredTo == NONE) {
            switch (protocol) {
            case REGISTER_AS:
                String isRegistered = null;
                try {
                    String registerTo = tokens[1];
                    if (registerTo.equals(WEBSITE)) {
                        registerClient(id, WEB, webOut);
                        isRegistered = "web";
                    }
                    else if (registerTo.equals(STORE)) {
                        registerClient(id, SHOP, shopOut);
                        isRegistered = "shop";
                    }
                    else System.err.printf("Client %d attempting to register as unrecognised type %s\n", id, registerTo);
                } catch (IndexOutOfBoundsException e) {System.err.printf("Error registering client %d: insufficient tokens\n", id);}
                if (isRegistered != null) System.out.printf("Registered %d to %s.\n", id, isRegistered);
                break;
            default:
                System.err.printf("Unregistered client %d attempting to use additional protocols\n", id);
                unregistered.get(id).getWriter().println(FAILURE+DELIM+input);
                unregistered.get(id).getWriter().flush();
            }
        } else if (registeredTo == WEB || registeredTo == SHOP) {
        
            switch (protocol) {
            case DEREGISTER:
                if (registeredTo == WEB) deregisterClient(id, webOut);
                else if (registeredTo == SHOP) deregisterClient(id, shopOut);
                System.out.println("Deregistered " + id);
                break;
        
            case NEW_ORDER:
                //send order number or rejection
                int orderNum = verifyOrder(tokens);
                if (orderNum != -1) {
                    replyTo(registeredTo, id, SUCCESS+DELIM+orderNum);
                    sendTo(SHOP, ALL, input.replace(NO_NUMBER, String.valueOf(orderNum).replace(NO_TIMESTAMP, previousOrderTime)));
                    reduceIngredientQuantities(id, registeredTo, tokens);
                } else replyTo(registeredTo, id, FAILURE+DELIM+input);
                break;
            
            case UPDATE_STATUS:
                short success = ERROR; 
                try {
                    orderNum = Integer.parseInt(tokens[1]);
                    String timestamp = tokens[2];
                    String status = tokens[3];
                    success = Database.changeOrderStatus(orderNum, timestamp, status);
                } catch (NumberFormatException e) {
                    System.err.println("Failed due to NumberFormatException");
                    success = ERROR;
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("Failed due to IndexOutOfBoundsException");
                    success = ERROR;
                }
                if (success == SUCCESS) sendTo(SHOP, id, input);
                else replyTo(registeredTo, id, success+DELIM+input);
                break;
                
            case INCREASE_QUANTITY:
            case DECREASE_QUANTITY:
            case SET_THRESHOLD:
            case UPDATE_PRICE:
                success = ERROR;
                try {
                    String ingredient = tokens[2];
                    if (protocol.equals(INCREASE_QUANTITY)) {
                        int value = Integer.parseInt(tokens[3]);
                        success = Database.increaseQty(ingredient, value);
                    }
                    else if (protocol.equals(DECREASE_QUANTITY)) {
                        int value = Integer.parseInt(tokens[3]);
                        success = Database.decreaseQty(ingredient, value);
                    }
                    else if (protocol.equals(SET_THRESHOLD)) {
                        int value = Integer.parseInt(tokens[3]);
                        success = Database.updateThreshold(ingredient, value);
                    }
                    else if (protocol.equals(UPDATE_PRICE)) {
                        double value = Double.parseDouble(tokens[3]);
                        success = Database.updatePrice(ingredient, value);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Failed due to NumberFormatException");
                    success = ERROR;
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("Failed due to IndexOutOfBoundsException");
                    success = ERROR;
                }
                if (success == SUCCESS) sendTo(SHOP, id, input);
                else replyTo(registeredTo, id, success+DELIM+input);
                break;
                
            case UPDATE_ORDER:
                success = -1;
                try {
                    String category = tokens[1];
                    int newOrder = Integer.parseInt(tokens[2]);
                    success = Database.reorderCategory(category, newOrder);
                } catch (NumberFormatException e) {success = ERROR;}
                  catch (IndexOutOfBoundsException e) {success = ERROR;}
                
                if (success == SUCCESS) sendTo(SHOP, id, input);
                else replyTo(registeredTo, id, success+DELIM+input);
                break;                
            
            case ADD_INGREDIENT:
                try {
                    String ingredient = tokens[1];
                    int quantity = Integer.parseInt(tokens[2]);
                    int threshold = Integer.parseInt(tokens[3]);
                    double price = Double.parseDouble(tokens[4]);
                    String category = tokens[5];
                    success = Database.addIngredient(ingredient, price, quantity, category, threshold);
                } catch (NumberFormatException e) {success = ERROR;}
                  catch (IndexOutOfBoundsException e) {success = ERROR;}
                
                if (success == SUCCESS) sendTo(SHOP, id, input);
                else replyTo(registeredTo, id, success+DELIM+input);
                break;
                
            case ADD_CATEGORY:
                try {
                    String category = tokens[1];
                    int order = Integer.parseInt(tokens[2]);
                    success = Database.addCategory(category, order);
                } catch (NumberFormatException e) {success = ERROR;}
                  catch (IndexOutOfBoundsException e) {success = ERROR;}
                        
                if (success == SUCCESS) sendTo(SHOP, id, input);
                else replyTo(registeredTo, id, success+DELIM+input);
                break;
                
            case REMOVE_INGREDIENT:
            case REMOVE_CATEGORY:
                success = -1;
                try {
                    if (protocol.equals(REMOVE_INGREDIENT)) {
                        String ingredient = tokens[2];
                        success = Database.removeIngredient(ingredient);
                    }
                    else if (protocol.equals(REMOVE_CATEGORY)) {
                        String category = tokens[1];
                        success = Database.removeCategory(category);
                    }
                } catch (IndexOutOfBoundsException e) {success = ERROR;}
                
                if (success == SUCCESS) sendTo(SHOP, id, input);
                else replyTo(registeredTo, id, success+DELIM+input);
                break;
                
            case REQUEST_CATEGORIES:
                String categories = Database.getCategories();
                replyTo(registeredTo, id, SENDING_CATEGORIES+DELIM+categories);
                break;
            
            case REQUEST_INGREDIENTS:
                String ingredients = Database.getIngredients();
                replyTo(registeredTo, id, SENDING_INGREDIENTS+DELIM+ingredients);
                break;
                
            case REQUEST_ORDERS:
                String orders = Database.getOrders();
                replyTo(registeredTo, id, SENDING_ORDERS+DELIM+orders);
                break;
                
            default:
                System.err.printf("Unrecognised or unsupported protocol %s from client %d\n", protocol, id);
    
            }
        } else {
            
        }
    }
    
    /**
     * Registers a client connection as coming from the web server or an in-store client
     * @param id int: the id for this client
     * @param registerTo short: corresponds to Server.WEB or Server.SHOP
     * @param outMap {@literal Map<Integer, PrintWriter>}: the map of outputs for this register type
     */
    private void registerClient(int id, short registerTo, Map<Integer, PrintWriter> outMap) {
        ServerConnection connection = unregistered.get(id);
        connection.register(registerTo);
        outMap.put(id, connection.getWriter());
        unregistered.remove(id);
    }
    

    /**
     * Deregisters a client connection because that client has closed its connection
     * @param id int: the id for this client
     * @param outMap {@literal Map<Integer, PrintWriter>}: the map of outputs for this register type
     */
    private void deregisterClient(int id, Map<Integer, PrintWriter> outMap) {
        outMap.remove(id);
    }
    
    /**
     * Sends data in response to a specific client
     * @param destinationType short: WEB or SHOP
     * @param writer int: id for the writer to respond to
     * @param response String: Protocol conforming data to be sent to the client
     * @return
     */
    public short replyTo(short destinationType, int writer, String response) {
        Map<Integer, PrintWriter> destinationMap = null;
        if (destinationType == WEB) destinationMap = webOut;
        else if (destinationType == SHOP) destinationMap = shopOut;
        else return ERROR;
        
        PrintWriter out = destinationMap.get(writer);
        if (out == null) return ERROR;
        
        out.println(response);
        out.flush();
        return SUCCESS;
    }

    /**
     * Sends data out to the appropriate client
     * @param destinationType short: WEB or SHOP
     * @param input String: Protocol conforming data to be sent to the client
     * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
     */
    public short sendTo(short destinationType, int except, String input) {
        Map<Integer, PrintWriter> destinationMap = null;
        if (destinationType == WEB) destinationMap = webOut;
        else if (destinationType == SHOP) destinationMap = shopOut;
        else return ERROR;
        
        for (Map.Entry<Integer, PrintWriter> entry : destinationMap.entrySet()) {
            if (except != ALL && entry.getKey() == except) continue;
            PrintWriter out = entry.getValue(); 
            out.println(input);
            out.flush();

        }
        return SUCCESS;
    }
    
    /**
     * Checks ingredient quantities against the database; if the database says we have at least the 
     * amount of each ingredient that is being requested, the order is valid. If the database says
     * we have less than the number of any requested ingredient, the order is not valid.
     * @param tokens String[]: individual tokens from the message
     * @return boolean: True if order is valid
     */
    private int verifyOrder(String[] tokens) {
        int orderNum = -1;
        try {
            String[] actualIngredients = Database.getIngredients().split(DELIM);
            Map<String, Integer> actualQuantities = new TreeMap<String, Integer>();
            for (int i = 0; i < actualIngredients.length; i++) {
                //category,ingredientName,price,num,minThreshold
                i++;
                String ingredientName = actualIngredients[i++];
                i++;
                //System.out.println("parsing actual");
                int quantity = Integer.parseInt(actualIngredients[i]);
                //System.out.println("parsed actual");
                actualQuantities.put(ingredientName, quantity);
                i++;
            }
            
            String ingredients = "";
            Map<String, Integer> orderQuantities = new TreeMap<String, Integer>();
            for (int i = 4; i < tokens.length-1; i++) {
                //System.out.println(i);
                String category = tokens[i++];
                //System.out.println(i);
                String ingredientName = tokens[i++];
                //System.out.println(i);
                //System.out.println("parsing order");
                int quantity = Integer.parseInt(tokens[i]);
                //System.out.println(i);
                //System.out.println("parsed order");
                orderQuantities.put(ingredientName, quantity);
                ingredients += category+DELIM+ingredientName+DELIM+quantity+DELIM; 
                
                for (Map.Entry<String, Integer> entry : orderQuantities.entrySet()) {
                    String ingredient = entry.getKey();
                    if (actualQuantities.get(ingredient) < entry.getValue()) {
                        System.out.println("failed 'cause we don't have it");
                        return -1;
                    }
                }
            }
            ingredients =  ingredients.substring(0, ingredients.length() - DELIM.length());//remove final delim
            orderNum = nextOrder();
            //orderNumber,customerName,timestamp,status,
            String customerName = tokens[2];
            String timestamp = previousOrderTime;
            Database.addOrder(orderNum, customerName, timestamp, client.Order.PENDING, ingredients);
        } catch (NumberFormatException e) {
            System.out.println("failed 'cause NumberFormatException");
            return -1;}
          catch (IndexOutOfBoundsException e) {
              System.out.println("failed 'cause IndexOutOfBounds");
              return -1;}
          catch (NullPointerException e) {
              System.out.println("failed 'cause NullPointer");
              return -1;} 
        
        return orderNum;
    }
    
    /**
     * Returns the order id number for a new order, resetting to 0 on a new day
     * @return String: the string representation of the order number
     */
    private int nextOrder() {
        String now = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        if (previousOrderTime == null) previousOrderTime = now;
        if (previousOrderTime.compareTo(now) < 0) lastOrder = 0;
        previousOrderTime = now;
        return ++lastOrder;
    }
    
    private void reduceIngredientQuantities(int id, short registeredTo, String[] tokens) throws NullPointerException, IndexOutOfBoundsException {
        for (int i = 4; i < tokens.length; i++) {
            String category = tokens[i++];
            String ingredient = tokens[i++];
            String quantity = tokens[i];
            process(id, registeredTo, DECREASE_QUANTITY+DELIM+category+DELIM+ingredient+DELIM+quantity);
        }
    }
    
    /**
     * Closes the server's listener and sets isRunning to false
     */
    private void close() {
        this.isRunning = false;
        try {
            listener.close();
            System.out.println("Server closed");
        } catch (IOException e) {
            System.err.println("Error closing server listener");
            e.printStackTrace();
        }
    }
    //
    /**
     * Returns the current status of this server
     * @return boolean: true if server is running
     */
    boolean isRunning() {
        return this.isRunning;
    }

}
