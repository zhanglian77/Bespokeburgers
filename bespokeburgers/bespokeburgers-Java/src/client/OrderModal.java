package client;

import java.util.Arrays;
import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * A modal pane that allows the user to make an order for a quantity of an ingredient from a stockist. In a real-world
 * environment, the 'place order' button would be linked to whatever system their wholesalers use. For now it does
 * nothing.
 * @author Bespoke Burgers
 *
 */
public class OrderModal extends Stage {
	
	//Attributes
    private Stage parentStage;
	private VBox layout;
	private Ingredient ingredient;
	
	
	/**
	 * Constructor
	 */
	public OrderModal(Stage parentStage, Ingredient ingredient) {
		this.setTitle("Order Ingredient");
		this.parentStage = parentStage;
		
		layout = new VBox();
		layout.setSpacing(10);
		this.ingredient = ingredient;

        Scene orderModalScene = new Scene(layout, 400, 400);
        orderModalScene.getStylesheets().add("/styleModal.css");
        this.setScene(orderModalScene);

        // Specifies the modality for new window.
        this.initModality(Modality.WINDOW_MODAL);

        // Specifies the owner Window (parent) for new window
        this.initOwner(parentStage);
        
        //Centers the modal
    	this.centerOnScreen();
    	
    	setupModal();

	}
	
	public void setupModal() {
		
		//Header and ingredient name.
		Label header = new Label("Order");
		header.getStyleClass().add("headerLabel");
		
		Label ingredientName = new Label("Ingredient: " + ingredient.getName());
		
		//Quantity controls.
		HBox quantityLayout = new HBox();
		Label quantityTitle = new Label("Order Quantity:");
		IntegerTextField quantityField = new IntegerTextField();
		quantityField.setText("0");
		quantityLayout.getChildren().addAll(quantityTitle,quantityField);
		
		Double price = ingredient.getPrice()/2; //assumes 100% markup on each ingredient.
		Label priceLabel = new Label("Wholesale Price/unit: $" + Double.toString(price));
		Label cost = new Label("Total cost: $" + Double.toString(price * (Double.parseDouble(quantityField.getText()))));
		
		//Add label style and set height of each normal label.
		List<Label> labels = Arrays.asList(ingredientName,quantityTitle,priceLabel,cost);
		for (Label l: labels) {
			l.getStyleClass().add("normalLabel");
			l.setMinHeight(40);
		}
		
		quantityField.setMinHeight(40);

		ingredientName.getStyleClass().add("normalLabel");
		quantityTitle.getStyleClass().add("normalLabel");
		priceLabel.getStyleClass().add("normalLabel");
		cost.getStyleClass().add("normalLabel");
		
		//Place order button in an HBox.
		Button orderButton = new Button("Place Order");
		orderButton.getStyleClass().add("orderButton");
		
		HBox buttonHBox = new HBox();
		buttonHBox.setAlignment(Pos.CENTER);
		buttonHBox.getChildren().add(orderButton);
		

		//Order button action event.
		orderButton.setOnAction((event) -> {
            	this.close();
         });

		
		//Add children to the main layout VBox.
		layout.getChildren().addAll(header,ingredientName,quantityLayout,priceLabel,cost,buttonHBox);
		
		
	}
	

}
