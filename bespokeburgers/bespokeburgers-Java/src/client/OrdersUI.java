package client;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import java.text.*;


/**
 * GUI layout for Orders tab.<br>
 * Sends completed orders to server for storage in database.<br>
 * Receives incoming orders from server via ClientConnection object.
 * @author Bespoke Burgers
 *
 */
public class OrdersUI extends Tab {
	
	//Attributes
    private ClientConnection client;
    private volatile Map<Integer, Order> orders;
    private Map<Integer, Order> currentOrders;
    
    private ScrollPane scrollPane;
    private HBox ordersHBox; //Where the orderPane objects are displayed.
    
    private Map<Integer, OrderPane> orderPanes; //Key is orderID, value is an orderPane object.
    
    private String filter;
    
    

    /**
     * Constructor
     * @param client ClientConnection: connection through which information can be sent to other clients
     */
    public OrdersUI(ClientConnection client) {
        this.client = client;
        this.orders = new HashMap<Integer, Order>();
        this.currentOrders = new HashMap<Integer, Order>();
        this.orderPanes = new HashMap<Integer, OrderPane>();
        
        this.filter = "Cook";
        
        setupOrdersTab();
    }
    

    
    /**
     * Adds a new order to the orders map and displays it in the UI.
     * @param order Order: the order to be added
     */
    public void add(Order order) {
    	        
    	this.orders.put(order.getId(), order);
    	addOrderPane(order);
    }
    
    
    /**
     * Sets the new status of the order
     * @param order int: The identification of the order
     * @param status String: the new status of the order
     * @param fromServer boolean: true if this method is being called from the ClientConnection object
     */
    public void updateStatus(int order, String status, boolean fromServer) {
      
    	System.out.println("updateStatus called for order: " + order + ". From server? " + fromServer);
		
    	orders.get(order).setStatus(status);
    	
    	OrderPane orderPane = orderPanes.get(order);
		orderPane.updateHeader();
		orderPane.updateActionButton();
		
		filterOrders();
		
    	//For updating not from server.
    	if (!fromServer) {
    		
    		client.updateStatus(Integer.toString(order), orders.get(order).getTimestamp(), status);
    		
    	}
    	
    }
    
    /**
     * Returns the specified order
     * @param order int: The identification of the order
     * @return Order: the specified order
     */
    public synchronized Order getOrder(int order) {
        
    	return orders.get(order);
    }
    
    /**
     * Returns current filter.
     * @return String: A string that represents the current state of the filter attribute.
     */
    public String getFilter() {
    	return this.filter;
    }
    
    /**
     * Sets the current filter.
     * @param filter String: A string representing the current state of the filter attribute.
     */
    public void setFilter(String filter) {
    	this.filter = filter;
    }
    
    /**
     * Adds order to the client's currentOrders.
     * @param order Order: the Order to be added.
     */
    public void addToCurrentOrders(Order order) {
    	
    	if (currentOrders != null) {
    		currentOrders.put(order.getId(), order);
    	}
    }
    
    /**
     * Removes order from the client's currentOrders.
     * @param order Order: the Order to be removed.
     */
    public void removeFromCurrentOrders(Order order) {
    	
    	if (currentOrders != null) {
        	currentOrders.remove(order.getId());
    	}
    }
    
    /**
     * Sets up the format of the orders tab.
     */
	public void setupOrdersTab() {
		
		//Set the text on the tab.
        this.setText("Orders");
        
        //Sets the HBox which will display the orders and adds it to the tab. Adds it to a ScrollPane.
        ordersHBox = new HBox();
        scrollPane = new ScrollPane(ordersHBox);

        this.setContent(scrollPane);
        
        //Setting the format of the ordersPane.
        ordersHBox.setSpacing(20);
        ordersHBox.setStyle("-fx-padding: 10 10 10 15");
		scrollPane.setStyle("-fx-padding: 30 0 0 0");

        /////TESTING/////
        //createTestOrders();
        refreshOrders();
	}
	
	private void addOrderPane(Order order) {
		
		System.out.println("add order pane called");
		
		OrderPane orderPane = new OrderPane(order,this);
		ordersHBox.getChildren().add(orderPane);
		orderPanes.put(order.getId(), orderPane);
		
		//filterOrders();
		
	}
	
	private void refreshOrders() {
		
		if (orders != null) {

			//Creates a treeMap from the orders map so that it is ordered by the keys (order ID).
			Map<Integer, Order> sortedTreeMap = new TreeMap<Integer, Order>(orders);

			//Iterate through the treeMap to create each order pane.
			for (int key : sortedTreeMap.keySet()) {

				OrderPane orderPane = new OrderPane(sortedTreeMap.get(key),this);
				ordersHBox.getChildren().add(orderPane);
				orderPanes.put(key, orderPane);
			}

			//Filter orders based on current filter.
			filterOrders();
		}
	}

	/**
	 * Used to hide and show specific orders based on the filter option. This does not create any new orderPanes,
	 * but gets them from the orderPanes map of already created orderPanes.
	 * @param filter String: Keyword to indicate the the type of orders that should be shown based on status.
	 */
	public void filterOrders() {

		System.out.println("filter Orders method called. Filter is: " + filter);

		//Clear display of currently shown orderPanes.
		ordersHBox.getChildren().clear();

		//If filter is 'Cook', add that cook's current in-progress orders.
		//Creates a treeMap from the currentOrders map so that it is ordered by the keys (order ID).
		if (currentOrders != null && filter.equals("Cook")) {

			Map<Integer, Order> sortedCurrentOrders = new TreeMap<Integer, Order>(currentOrders);
			
			for (int key: sortedCurrentOrders.keySet()) {
				Order order = sortedCurrentOrders.get(key);
				
				System.out.println("STATUS: " + order.getStatus() );
				System.out.println("currentOrder: "+ order.getId());
				
				if (order.getStatus().equals(Order.IN_PROGRESS)) {
					OrderPane orderPane = orderPanes.get(sortedCurrentOrders.get(key).getId());
					orderPane.updateActionButton();
					ordersHBox.getChildren().add(orderPane);
				}

			}
		}

		if (orderPanes != null) {
			//Creates a treeMap from the orderPanes map so that it is ordered by the keys (order ID).
			Map<Integer, OrderPane> sortedTreeMap = new TreeMap<Integer, OrderPane>(orderPanes);
			
			//Iterate through the TreeMap and creates 4 separate TreeMaps for each status type.
			Map<Integer, OrderPane> pendingTreeMap = new TreeMap<Integer, OrderPane>();
			Map<Integer, OrderPane> inProgressTreeMap = new TreeMap<Integer, OrderPane>();
			Map<Integer, OrderPane> completeTreeMap = new TreeMap<Integer, OrderPane>();
			Map<Integer, OrderPane> collectedTreeMap = new TreeMap<Integer, OrderPane>();


			for (int key : sortedTreeMap.keySet()) {

				String status = orders.get(key).getStatus();
				OrderPane orderPane = sortedTreeMap.get(key);

				switch(status) {
				case Order.PENDING: pendingTreeMap.put(key,orderPane);

				break;
				case Order.IN_PROGRESS: inProgressTreeMap.put(key,orderPane);
				break;

				case Order.COMPLETE: completeTreeMap.put(key,orderPane);
				break;

				case Order.COLLECTED: collectedTreeMap.put(key,orderPane);
				break;
				}	
			}

			//Put the contents of the four tree maps into a single list, ordered depending on the current filter.
			List<OrderPane> sortedList = new ArrayList<OrderPane>();

			switch (filter) {
			case "Cashier": 
				sortedList.addAll(completeTreeMap.values());
				sortedList.addAll(inProgressTreeMap.values());
				sortedList.addAll(pendingTreeMap.values());
				break;

			case "Cook": 
				sortedList.addAll(pendingTreeMap.values());
				break;

			case "Manager": 
				sortedList.addAll(sortedTreeMap.values());
				break;
			}
			
			//Check for orders that are from the day before, record their index position.
			List<Integer> indexesToMove = new ArrayList<Integer>();
			
			String now = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
			
			for (int i = 0; i < sortedList.size(); i++) {
				OrderPane pane = (OrderPane) sortedList.get(i);
				String orderTimeStamp = pane.getOrder().getTimestamp();
				
				if (orderTimeStamp.compareTo(now) < 0 && orderTimeStamp.compareTo(now) > -1) {
		        	
					indexesToMove.add(i);
		        }
			}
			
			//Move orders from the previous day to the front.
			for (int i = indexesToMove.size()-1; i >= 0; i--) {
				
				int indexToRemove = indexesToMove.get(i);
				sortedList.add(0,sortedList.remove(indexToRemove));
			}

			//Add sortedList to the ordersHBox. Updates pane's action button based on filter.
			for (OrderPane pane: sortedList ) {
				
				pane.updateActionButton();
				ordersHBox.getChildren().add(pane);
			}

		}

	}
	


}
