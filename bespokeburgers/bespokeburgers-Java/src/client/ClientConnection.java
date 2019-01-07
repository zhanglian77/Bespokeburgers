package client;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;

import static protocol.Protocol.*;

/**
 * Processes incoming data and calls appropriate methods on IngredientsUI, OrdersUI, Ingredient, Category, and Order objects.<br>
 * Receives information from IngredientsUI, OrdersUI, Ingredient, Category, and Order objects and forwards that information through the server to the database and web server.

 * @author Bespoke Burgers
 *
 */
public class ClientConnection implements Runnable {
    private final String SERVER_IP = "127.0.0.1";
    private final int SERVER_PORT = 9090;
    private boolean isConnected;
    private IngredientsUI ingredientsUI;
    private OrdersUI ordersUI;
    private Socket socket;
    private PrintWriter serverOut;
    private BufferedReader serverIn;

    public ClientConnection() throws IOException {
       // this.connect(SERVER_IP, SERVER_PORT);
    }
    
    public void setUIs(IngredientsUI ingredientsUI, OrdersUI ordersUI) {
        this.ingredientsUI = ingredientsUI;
        this.ordersUI = ordersUI;
    }

    /**
     * Runs on a separate thread to the rest of the client.
     * Incoming data will be caught and processed here.
     */
    @Override
    public void run() {
    	System.out.println("run called");
    	System.out.println("isconnected? "+ this.isConnected);
    	
    	try {
    	    send(REQUEST_CATEGORIES);
        	String message = serverIn.readLine();
            System.out.println("message: " + message);
            process(message);
        
            send(REQUEST_INGREDIENTS);
            message = serverIn.readLine();
            System.out.println("message: " + message);
            process(message);
        
            send(REQUEST_ORDERS);
            message = serverIn.readLine();
            System.out.println("message: " + message);
            process(message);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
        while (this.isConnected) {
            try {
                String message = serverIn.readLine();
                System.out.println("message: " + message);
                process(message);
            } catch (EOFException e) {
                
            	e.printStackTrace();
            	//Thrown when the server unexpectedly closes connection because it's stuck on in.readObject() when the connection terminates
                try { disconnect(); } catch (IOException e1) {
                    //no action needed
                }
                break;
            } catch (IOException e) {
                System.err.println("IO error");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Connects to the requested server
     * @param ip String: The IP address to the server
     * @param port int: The port the server is listening to
     * @return boolean: true if connection was established
     * @throws IOException
     */
    public boolean connect() throws IOException {
        try {
            socket = new Socket(this.SERVER_IP, this.SERVER_PORT);
            serverOut = new PrintWriter(socket.getOutputStream());
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.isConnected = true;
            new Thread(this).start();
            send(REGISTER_AS+DELIM+STORE);
        } catch (UnknownHostException e){ return false; }
        return true;
    }
    
    /**
     * Disconnects from the server
     * @throws IOException
     */
    public void disconnect() throws IOException {
        try{
            send(DEREGISTER);
            this.isConnected = false;
            socket.close();
        } catch (IOException e) {
            System.err.println("error disconnecting");
            this.isConnected = false;
            throw e;
        }
    }
    
    /**
     * Processes incoming data from server
     * @param input String: raw data as-sent from the server
     */
    private void process(String input) {
        String[] tokens = input.split(DELIM);
        String protocol = tokens[0];
        System.out.println("Protocol" + protocol);
        
        switch (protocol) {
        case ACKNOWLEDGE_DISCONNECT:
            break; //no action needed
            
        case DEREGISTER:
            try { disconnect(); } catch (IOException e) {
                //no action needed
            }
            break;
        
        case ""+ERROR:
            undoAction(input.split(ERROR+DELIM)[1]);
            break;
            
        case ""+FAILURE:
            undoAction(input.split(FAILURE+DELIM)[1]);
            break;
            
        case NEW_ORDER:
            try {
                Map<Ingredient, Integer> ingredientMap = new HashMap<Ingredient, Integer>();
                int id = Integer.parseInt(tokens[1]);
                String customer = tokens[2];
                String timestamp = tokens[3];
                for (int i = 4; i < tokens.length-1; i++) {
                    String category = tokens[i++];
                    String ingredientName = tokens[i++];
                    int quantity = Integer.parseInt(tokens[i]);
                    Ingredient ingredient = ingredientsUI.getIngredient(category, ingredientName);
                    ingredientMap.put(ingredient, quantity);
                }
                double price = calculateCost(ingredientMap);
                Order order = new Order(id, customer, ingredientMap, price, timestamp);
                Platform.runLater(()->{                
                	ordersUI.add(order);
                });
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
            
        case UPDATE_STATUS:
            try {
                int id = Integer.parseInt(tokens[1]);
                String timestamp = tokens[2];
                String status = tokens[3];
                Platform.runLater(()->{
                	ordersUI.updateStatus(id, status, true);
                });
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
            
        case INCREASE_QUANTITY:
            try {
                String category = tokens[1];
                String ingredient = tokens[2];
                int byAmount = Integer.parseInt(tokens[3]);
                ingredientsUI.increaseQty(category, ingredient, byAmount, true);
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
            
        case DECREASE_QUANTITY:
            try {
                String category = tokens[1];
                String ingredient = tokens[2];
                int byAmount = Integer.parseInt(tokens[3]);
                ingredientsUI.decreaseQty(category, ingredient, byAmount, true);
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
            
        case SET_THRESHOLD:
            try {
                String category = tokens[1];
                String ingredient = tokens[2];
                int threshold = Integer.parseInt(tokens[3]);
                ingredientsUI.setMinThreshold(category, ingredient, threshold, true);
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
            
        case UPDATE_PRICE:
            try { 
                String category = tokens[1];
                String ingredient = tokens[2];
                double price = Double.parseDouble(tokens[3]);
                ingredientsUI.updatePrice(category, ingredient, price, true);
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;

        case ADD_INGREDIENT:
        	try {
        		String name = tokens[1];
        		int quantity = Integer.parseInt(tokens[2]);
        		int threshold = Integer.parseInt(tokens[3]);
        		double price = Double.parseDouble(tokens[4]);
        		String categoryName = tokens[5];
        		addIngredient(categoryName,name,price,quantity,threshold);

        	} catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
        	catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
        	break;

        case REMOVE_INGREDIENT:
            try {
                String category = tokens[1];
                String ingredient = tokens[2];
                Platform.runLater(()->{
                	ingredientsUI.removeIngredient(category, ingredient, true);
                });
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
            
        case ADD_CATEGORY:
            try {
                addCategory(tokens[1],Integer.parseInt(tokens[2]));
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
        
        case REMOVE_CATEGORY:
            try {
                String category = tokens[1];
                Platform.runLater(()->{                
                	ingredientsUI.removeCategory(category, true);
                });
            } catch (NumberFormatException e) {System.err.println("Error parsing message " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message " + input);}
            break;
            
        case SENDING_INGREDIENTS:
            try {
                addIngredients(tokens);
            } catch (NumberFormatException e) {System.err.println("Error parsing message NUMBERFORMATexception: " + input);}
              catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message IndexOutOfBoundsException: " + input);}
            break;
            
        case SENDING_CATEGORIES:
        	try {
        		addCategories(tokens);
        	} catch (NumberFormatException e) {System.err.println("Error parsing message NumberFormatException: " + input);}
        	catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message IndexOutOfBounds:  " + input);}
        	break;
            
        case SENDING_ORDERS:
            try {
                addOrders(tokens);
            } catch (NumberFormatException e) {System.err.println("Error parsing message NumberFormatException: " + input);}
            catch (IndexOutOfBoundsException e) {System.err.println("Error parsing message IndexOutOfBounds:  " + input);}
            break;

        default:
            System.err.println("Unrecognised or unsupported protocol from server");
        }
    }
    
    /**
     * Helper method to add a single ingredient to the system
     * @param tokens String[]: individual tokens from the server message
     * @throws NumberFormatException if unable to parse token to int or double
     * @throws IndexOutOfBoundsException if not enough tokens in server message
     */
    private void addIngredient(String categoryName, String name, double price, int quantity, int threshold) throws NumberFormatException, IndexOutOfBoundsException {
        
    	
        //Category category = ingredientsUI.getCategory(categoryName);
    	
        //System.out.println("category: " + category + "name: "+ categoryName);
        Ingredient ingredient = new Ingredient(null, name, quantity, threshold, price);
        
        Platform.runLater(()->{
        	ingredientsUI.addIngredient(ingredient, categoryName, true);
        });
    }
    
    
   private void addIngredients(String[] tokens) throws NumberFormatException, IndexOutOfBoundsException {
       
	   
	   for (int i = 1; i < tokens.length; i++) {
		   
		   String categoryName = tokens[i++];
	       String name = tokens[i++];
	       double price = Double.parseDouble(tokens[i++]);

	       int quantity = Integer.parseInt(tokens[i++]);
	       int threshold = Integer.parseInt(tokens[i]);
	       
	       addIngredient(categoryName,name,price,quantity,threshold);
	   }
	   
   }
    
    
    
    /**
     * Helper method to add a single category to the system
     * @param name String: category name from the server message
     * @param orderNum int: the order in which categories are sorted.
     * @throws NumberFormatException if unable to parse token to int
     * @throws IndexOutOfBoundsException if not enough tokens in server message
     */
    private void addCategory(String name,int orderNum) throws NumberFormatException, IndexOutOfBoundsException {

    	Category category = new Category(name, orderNum);
        
    	Platform.runLater(()->{
    		ingredientsUI.addCategory(category, true);
    	});
    }
    
    private void addCategories(String[] tokens) throws NumberFormatException, IndexOutOfBoundsException {

    	for (int i = 1; i < tokens.length; i++) {
    		addCategory(tokens[i],i);
    	}
    }
    
    //Error parsing message IndexOutOfBounds:  SEND_ORDER,1,yay,2018/11/16,pending,bread,Sesame,1,0,;,
    //1,yay,2018/11/16,pending,bread,sesame,1(0),ingredientCategory,ingredientName,num(etc),orderNumber2,customerName2,timestamp2 etc */
    
    private void addOrders(String[] tokens) {
        //SEND_ORDER,1003,Sally,07-11-2018 09:00,complete,Bread:Wholemeal, Patty: Chicken, Lettuce*3,  Onions*1, Jalapeno*1, Coleslaw*1, Sauces:Ranch-Tomato,15,;,1001,Johnny,01-11-2018 13:00,complete,Bread:Sesame, Patty: Falafel, Lettuce*1, Tomato*2  Onions*1, Sauces:BBQ-Tomato,20,;,1002,Donald,12-11-2018 15:00,complete,Bread:Plain, Patty: Beef, Cheese*1, Olives*2, Sauces:BBQ,15,;,1,NOSTAMP,2018/11/16,pending,bread,Plain,1,0,;,2,NOSTAMP,2018/11/16,pending,bread,Plain,1,0,;,
        System.out.println(tokens.length);
        Platform.runLater(()->{ 
            for (int i = 1; i < tokens.length; i++) {
                boolean orderIsValid = true;
                Map<Ingredient, Integer> ingredientMap = new HashMap<Ingredient, Integer>();
                int id = Integer.parseInt(tokens[i++]);
                String customer = tokens[i++];
                String timestamp = tokens[i++];
                String status = tokens[i++];
                while (!tokens[i].equals(";")) {
                    String category = tokens[i++];
                    String ingredientName = tokens[i++];
                    int quantity = Integer.parseInt(tokens[i++]);
                    
                    Ingredient ingredient = ingredientsUI.getIngredient(category, ingredientName);
                    if (ingredient == null) orderIsValid = false;
                    ingredientMap.put(ingredient, quantity);
                }
                if (!orderIsValid) {
                    //skip this order and set it to complete
    //                if (!status.equals(Order.COMPLETE)) //set to complete
                    continue;
                }
                double price = calculateCost(ingredientMap);
                Order order = new Order(id, customer, ingredientMap, price, timestamp);
                order.setStatus(status);               
                ordersUI.add(order);
            }
        });
    }
    
    private double calculateCost(Map<Ingredient,Integer> ingredients) {
    	
    	double cost = 0.0;
    	
    	for (Map.Entry<Ingredient, Integer> ing : ingredients.entrySet()) {
    		
    		cost += ing.getKey().getPrice() * ing.getValue();
    	}
    	
    	return cost;
    }
    
    
    
    /**
     * Send a protocol conforming string to the server
     * @param output String: protocol conforming output
     */
    private void send(String output) {
        serverOut.println(output);
        serverOut.flush();
    }
    
    
    /**
     * Undo an action the server said has failed
     * @param action String: raw protocol conforming string representation of the action
     */
    public void undoAction(String action) {
        //TODO
    }
    
    /**
     * Sends a update of order status for other store client displays.
     * @param orderID String: identifier of the order
     * @param status String: new status
     */
    public void updateStatus(String orderID, String timestamp, String status) {
        String message = UPDATE_STATUS+DELIM+orderID+DELIM+timestamp+DELIM+status;
        send(message);
    }
    
    /**
     * Sends an updated price for a specific ingredient.
     * @param category String: ingredient category (e.g. salad)
     * @param ingredient String: name of the ingredient (e.g. lettuce)
     * @param price double: cost to customer per unit
     */
    public void updatePrice(String category, String ingredient, double price) {
        String message = UPDATE_PRICE+DELIM+category+DELIM+ingredient+DELIM+price;
        send(message);
    }
    
    /**
     * Sends a request to increase the number of a specific ingredient held in stock
     * @param category String: ingredient category (e.g. salad)
     * @param ingredient String: name of the ingredient (e.g. lettuce)
     * @param byAmount int: number by which to increase the quantity
     */
    public void increaseQty(String category, String ingredient, int byAmount) {
        String message = INCREASE_QUANTITY+DELIM+category+DELIM+ingredient+DELIM+byAmount;
        send(message);
    }
    
    /**
     * Sends a request to increase the number of a specific ingredient held in stock
     * @param category String: ingredient category (e.g. salad)
     * @param ingredient String: name of the ingredient (e.g. lettuce)
     * @param byAmount int: number by which to decrease the quantity
     */
    public void decreaseQty(String category, String ingredient, int byAmount) {
        String message = DECREASE_QUANTITY+DELIM+category+DELIM+ingredient+DELIM+byAmount;
        send(message);
    }
    
    /**
     * Sends an update of the minimum acceptable quantity for a specific ingredient
     * @param category String: ingredient category (e.g. salad)
     * @param ingredient String: name of the ingredient (e.g. lettuce)
     * @param threshold int: minimum number in stock before shop is notified to restock
     */
    public void setMinThreshold(String category, String ingredient, int threshold) {
        String message = SET_THRESHOLD+DELIM+category+DELIM+ingredient+DELIM+threshold;
        send(message);
    }
    
    /**
     * Adds a new ingredient to the system
     * @param ingredient Ingredient: the ingredient object to be added
     */
    public void addIngredient(Ingredient ingredient) {
        //ingredientName,number,minThreshold,price,category
        String message = ADD_INGREDIENT+DELIM+ingredient.getName()+DELIM+ingredient.getQuantity()+DELIM+
                         ingredient.getMinThreshold()+DELIM+ingredient.getPrice()+DELIM+ingredient.getCategory().getName();
        send(message);
    }
    
    /**
     * Removes an ingredient from the system
     * @param category String: ingredient category (e.g. salad)
     * @param ingredient String: name of the ingredient (e.g. lettuce)
     */
    public void removeIngredient(String category, String ingredient) {
        String message = REMOVE_INGREDIENT+DELIM+category+DELIM+ingredient;
        send(message);
    }
    
    /**
     * Sends an update of the order for all categories
     * @param category String: ingredient category (e.g. salad)
     * @param newOrder int: number representing the order for this category (e.g 1 is the first category to be displayed)
     */
    public void reorderCategories(String category, int newOrder) {
        String message = UPDATE_ORDER+DELIM+category+DELIM+newOrder;
        send(message);
    }
    
    /**
     * Add a new category to the system
     * @param category String: ingredient category (e.g. salad)
     */
    public void addCategory(Category category) {
        String message = ADD_CATEGORY+DELIM+category.getName()+DELIM+category.getOrder();
        send(message);
    }
    
    /**
     * Remove a category from the system
     * @param category String: ingredient category (e.g. salad)
     */
    public void removeCategory(String category) {
        String message = REMOVE_CATEGORY+DELIM+category;
        send(message);
    }
    
    public void requestCategories() {
        send(REQUEST_CATEGORIES);
    }
    
    public void requestIngredients() {
        send(REQUEST_INGREDIENTS);
        
    }
    
    public void requestOrders() {
        send(REQUEST_ORDERS);
    }

}
