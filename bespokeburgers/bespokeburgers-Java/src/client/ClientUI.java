package client;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import static protocol.Protocol.*;

import java.io.IOException;

import com.sun.javafx.css.StyleManager;

/**
 * Main entry point and application window for the client-side
 * @author Bespoke Burgers
 *
 */
public class ClientUI extends Application {

	//Attributes
	private ClientConnection client;
	private OrdersUI ordersUI;
	private IngredientsUI ingredientsUI;

	private TabPane tabPane;
	private OrdersUI ordersTab;
	private IngredientsUI ingredientsTab;
    private ComboBox<String> orderFilter;
    
    private AnchorPane root;
    private Stage stage;


	@Override
	public void start(Stage stage) throws Exception {
		
		//Closes connection when application is closed
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
		    @Override
		    public void handle(WindowEvent event) {
		        try {
					client.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
		});

        this.root = new AnchorPane();
		this.tabPane = new TabPane();
		this.stage = stage;
		this.client = new ClientConnection();

		setupTabPane();
		
		//Creates the scene with the root group. Sets the style sheet to use.
		final Scene scene = new Scene(root, 0, 0);
		String css = this.getClass().getResource("/styleIngredients.css").toExternalForm();
		scene.getStylesheets().add(css);
		
		scene.getStylesheets().add(ClientUI.class.getResource("/styleIngredients.css")
			    .toExternalForm());
		
		//Display the window.
		stage.setTitle("BespokeBurgers - Operator UI");
		stage.setScene(scene);
		stage.show();

		//Fit scene to width and height of the screen (from http://www.java2s.com/Code/Java/JavaFX/Setstagexandyaccordingtoscreensize.htm)
		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		stage.setWidth(primScreenBounds.getWidth());
		stage.setHeight(primScreenBounds.getHeight());
		stage.setX(0);
		stage.setY(0);
		
		//Connecting to server.
		client.connect();
		

	}
	
	public void setupTabPane() {
		
		//Creates the 2 tabs and adds them to the TabPane. 
		ordersTab = new OrdersUI(client);
		ingredientsTab = new IngredientsUI(client,stage);
		tabPane.getTabs().addAll(ordersTab,ingredientsTab);
		
		//Give a reference to the 2 UI's to the client connection.
		this.client.setUIs(ingredientsTab, ordersTab);
		
		//Set widths and heights of the tabs.
		int tabWidth = 183;
		int tabHeight = 100;
		
        //Setting up the filterOrders ComboBox
        ObservableList<String> values = FXCollections.observableArrayList("Cook","Cashier","Manager");
        orderFilter = new ComboBox<>(values);
        orderFilter.setMinWidth(tabWidth*2.2);
        orderFilter.setMinHeight(tabHeight/2);
        orderFilter.setValue("Cook");
        orderFilter.getStyleClass().add("orderFilter");
        
        //Add action to filterOrders ComboBox
        orderFilter.setOnAction(e -> {
        	
        	String filter = orderFilter.getValue();
        	ordersTab.setFilter(filter);
        	ordersTab.filterOrders();

        });
		
		tabPane.setTabMinHeight(tabHeight);
		tabPane.setTabMaxHeight(tabHeight);
		
		tabPane.setTabMinWidth(tabWidth);
		tabPane.setTabMaxWidth(tabWidth);
		
                
        //Adds the TabPane and the orderFilter ComboBox to the AnchorPane (root).
        //Adapted from: https://stackoverflow.com/questions/37721760/add-buttons-to-tabs-and-tab-area-javafx
        root.getChildren().addAll(tabPane, orderFilter);
        AnchorPane.setTopAnchor(orderFilter, 3.0);
        AnchorPane.setRightAnchor(orderFilter, 5.0);
        AnchorPane.setTopAnchor(tabPane, 1.0);
        AnchorPane.setRightAnchor(tabPane, 1.0);
        AnchorPane.setLeftAnchor(tabPane, 1.0);
        AnchorPane.setBottomAnchor(tabPane, 1.0);
		
		//Remove ability to close tabs.
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		tabPane.setStyle("-fx-padding: -6 0 -1 -6");

	}
	
	
	
    public static void main(String[] args) throws IOException {
        launch();
    }

}
