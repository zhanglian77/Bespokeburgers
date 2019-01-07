package client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


public class OrderPane extends VBox {

	//Attributes
	private Order order;
	private VBox header;
	private VBox ingredients;
	private OrdersUI parent;
	private Label statusLabel;
	private Button actionButton;

	/**
	 * Constructor
	 * @param order Order: order from customer that needs to be displayed
	 */
	public OrderPane(Order order, OrdersUI parent) {

		this.order = order;
		this.parent = parent;

		//Add VBoxes to the pane.
		this.header = new VBox();
		this.ingredients = new VBox();
		this.getChildren().addAll(this.header,this.ingredients);

		setupOrderPane(order);
	}

	/**
	 * Updates the header to match the order's current status. This is reflected in colour and statusLabel text.
	 */
	public void updateHeader() {

		String status = order.getStatus();

		//List of the possible header style classes and then removes them from the header.
		List<String> styles = Arrays.asList("headerPending","headerInProgress","headerComplete","headerCollected");
		header.getStyleClass().removeAll(styles);

		//Set the text on the status label.
		statusLabel.setText("Status: " + status);

		switch (status) {
		case Order.PENDING: header.getStyleClass().add("headerPending"); setHeaderLabelColourDark(true); 
		break;
		case Order.IN_PROGRESS: header.getStyleClass().add("headerInProgress"); setHeaderLabelColourDark(false);
		break;
		case Order.COMPLETE: header.getStyleClass().add("headerComplete");setHeaderLabelColourDark(true);
		break;
		case Order.COLLECTED: header.getStyleClass().add("headerCollected");setHeaderLabelColourDark(false);
		break;
		}		
	}

	/**
	 * Sets the header labels text to a dark colour when true, or a light colour when false.
	 * @param dark Boolean: whether text should use a dark or light coloured styling.
	 */
	public void setHeaderLabelColourDark(Boolean dark) {

		//List of the possible headerLabel style classes which are then removed from the label.
		List<String> styles = Arrays.asList("headerLabelDark","headerLabelLight");

		List<Node> headerLabels = header.getChildren();

		for (Node child : headerLabels) {
			try {
				Label label = (Label) child;
				label.getStyleClass().removeAll(styles);				
				if (dark) {
					label.getStyleClass().add("headerLabelDark");
				} else {
					label.getStyleClass().add("headerLabelLight");
				}
			}
			catch(ClassCastException exc) {}
		}
	}

	/**
	 *Sets up the layout of the whole orderPane object. Creates labels to match the order.
	 * @param order Order: The order object associated with this orderPane.
	 */
	public void setupOrderPane(Order order) {

		//Order number, customer name labels. 
		statusLabel = new Label("Status: " + order.getStatus());
		Label number = new Label("Order # " + order.getId());
		Label customer = new Label("Name: " + order.getCustomer());
		Label price = new Label("Price: " + order.getPrice());

		//Add header labels to the header VBox.
		header.getChildren().addAll(statusLabel,number,customer,price);

		//Add labels for each ingredient. The map is sorted using Comparable interface.
		Map<Ingredient,Integer> ingredientMap = order.getIngredients();
		List<Ingredient> sortedIngredientList = new ArrayList<Ingredient>(ingredientMap.keySet());
		Collections.sort(sortedIngredientList);

		//Change size of the orderPane..
		int width = 400;
		int height = 800;
		this.setMinWidth(width);
		this.setMaxWidth(width);
		this.setMinHeight(height);
		this.setMaxHeight(height);

		for (int i = 0; i < sortedIngredientList.size(); i++) {

			//Create label with the name of the ingredient and quantity required. 
			Ingredient item = sortedIngredientList.get(i);
			String ingredient = item.getName() + " x " + ingredientMap.get(item);
			Label ingredientLabel = new Label(ingredient);
			if (item.getQuantity() <= item.getMinThreshold()) ingredientLabel.setTextFill(Color.RED);

			//Add relevant style class.
			switch (item.getCategory().getName()) {
			case "bread" : ingredientLabel.getStyleClass().add("labelBun"); break;
			case "patty" : ingredientLabel.getStyleClass().add("labelPatty"); break;
			case "salad" : ingredientLabel.getStyleClass().add("labelSalad"); break;
			case "sauce" : ingredientLabel.getStyleClass().add("labelSauce"); break;
			case "cheese" : ingredientLabel.getStyleClass().add("labelCheese"); break;
			}

			//Add label to the ingredients VBox.
			ingredients.getChildren().add(ingredientLabel);

			//Set the width of the label to match the width of the order pane.
			ingredientLabel.setMinWidth(width);
			
			item.addQuantityListener(order.getId(), (ing, quantity, threshold) -> {
			    if (quantity <= threshold) {
			        ingredientLabel.getStyleClass().add("labelRedAlert");
			        ingredientLabel.setTextFill(Color.RED);
			    } else {
			        ingredientLabel.getStyleClass().remove("labelRedAlert");
                    ingredientLabel.setTextFill(Color.BLACK);
			    }
			});
		}

		//Add action button to the end. Add style class. Add it to an HBox in order to center it.
		actionButton = new Button();
		actionButton.getStyleClass().add("doneButton");
		updateActionButton();
		HBox buttonHBox = new HBox();
		buttonHBox.setAlignment(Pos.CENTER);
		buttonHBox.getChildren().add(actionButton);
		ingredients.getChildren().add(buttonHBox);


		//Change the background colour and status label of the header.
		updateHeader();

		//Add style class to the header,ingredients,and order panes. Refer to the CSS file styleIngredients..
		header.getStyleClass().add("headerPane");
		ingredients.getStyleClass().add("ingredientsPane");
		this.getStyleClass().add("orderPane");		


		//Add event handler to header. Got from: 
		//https://stackoverflow.com/questions/25550518/add-eventhandler-to-imageview-contained-in-tilepane-contained-in-vbox
		header.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {

			String currentFilter = parent.getFilter();
			String currentStatus = order.getStatus();

			if (currentFilter.equals("Cook")) {

				//Toggles between pending and in-progress. 
				if (currentStatus.equals(Order.PENDING)) {
					parent.addToCurrentOrders(order);
					parent.updateStatus(order.getId(), Order.IN_PROGRESS, false);
					//setOrderStatus(Order.IN_PROGRESS);

				}else if (currentStatus.equals(Order.IN_PROGRESS)) {
					parent.removeFromCurrentOrders(order);
					parent.updateStatus(order.getId(), Order.PENDING, false);
					//setOrderStatus(Order.PENDING);
				}

			}

			if (currentFilter.equals("Manager")) {

				//Allows manager to toggles between all four statuses.
				switch(currentStatus) {
				case Order.PENDING: parent.updateStatus(order.getId(), Order.IN_PROGRESS, false); break;
				case Order.IN_PROGRESS: parent.updateStatus(order.getId(), Order.COMPLETE, false); break;
				case Order.COMPLETE: parent.updateStatus(order.getId(), Order.COLLECTED, false); break;
				case Order.COLLECTED: parent.updateStatus(order.getId(), Order.PENDING, false); break;
				}

			}


			event.consume();
		});


	}
	
	
	/**
	 * Updates the state of the action button based on the status of the order.
	 */
	public void updateActionButton() {
			
		String filter = parent.getFilter();		

		switch(order.getStatus()) {
		case Order.IN_PROGRESS: 

			actionButton.setText("Done");
			actionButton.setVisible(true);

			//Only cooks and managers can set an in-progress order to COMPLETE.
			if (!filter.equals("Cashier")) {
				
				actionButton.setDisable(false);
				actionButton.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						
						parent.removeFromCurrentOrders(order);//
						parent.updateStatus(order.getId(), Order.COMPLETE, false);
					}
				});
			} else {
				actionButton.setDisable(true);
			}
			break;
		
		case Order.PENDING:
			
			actionButton.setVisible(false);
			actionButton.setOnAction(new EventHandler<ActionEvent>() {
	            
				@Override
	            public void handle(ActionEvent event) {
	            	
	                System.out.println("Order pending so button uninteractable.");

	            }
	        });
			break;
			
		case Order.COMPLETE: 
			
			actionButton.setText("Collected");
			actionButton.setVisible(true);
			actionButton.setDisable(false);
			actionButton.setOnAction(new EventHandler<ActionEvent>() {
	            
				@Override
	            public void handle(ActionEvent event) {

	            	parent.updateStatus(order.getId(), Order.COLLECTED, false);

	            }
			});
			break;
			
		case Order.COLLECTED: 

			actionButton.setVisible(false);
			actionButton.setOnAction(new EventHandler<ActionEvent>() {
				
				@Override
				public void handle(ActionEvent event) {

					System.out.println("Order collected so button uninteractable.");

				}
			});
			break;
		}

	}
	
	public Order getOrder() {
		return this.order;
	}
}



