package client;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * 
 * @author Bespoke Burgers
 *
 */
public class IngredientRow {
    public static final int INGREDIENT_COL = 0;
    public static final int CURRENT_STOCK_COL = 1;
    public static final int UPDATE_STOCK_COL = 2;
    
    private IngredientsUI ui;
    private Ingredient ingredient;
    private int rowIndex;
    private Pane ingredientCell;
    private Pane currentStockCell;
    private Pane updateStockCell;
    private Pane orderAndSettingCell;
    private Text currentStock;
    private ToggleGroup toggleGroup;
    private Toggle addToggle;
    private Toggle remToggle;
    private TextField addOrRemField;
    private Button orderBtn;
    private boolean isSelected;
    
    public IngredientRow(IngredientsUI ui, Ingredient ingredient, int rowIndex) {
        this.ui = ui;
        this.ingredient = ingredient;
        this.rowIndex = rowIndex;
        this.ingredientCell = createPane(ingredient.getName(), INGREDIENT_COL);
        this.currentStockCell = createPane(String.valueOf(ingredient.getQuantity()), CURRENT_STOCK_COL);
        this.updateStockCell = createOptionsPane();
        this.orderAndSettingCell = createButtonPane();
        
        if (ingredient.getQuantity() <= ingredient.getMinThreshold()) currentStock.setFill(Color.RED);
        
        ingredient.addQuantityListener(-rowIndex, new IngredientQuantityListener() {

            @Override
            public void onQuantityChange(Ingredient ingredient, int quantity, int threshold) {
                currentStock.setText(String.valueOf(quantity));
                if (quantity <= threshold) {
                    currentStock.setFill(Color.RED);
                } else {
                    currentStock.setFill(Color.BLACK);
                }
            }
            
        });
    }
    
    private Pane createPane(String contents, int row) {
        Text text = new Text(contents);
        VBox pane = new VBox(text);
        pane.setPadding(new Insets(5));
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.getStyleClass().add("normalBorder");
        pane.setOnMouseClicked(this::handleMouseEvent);
        if (row == CURRENT_STOCK_COL) currentStock = text;
        return pane;
    }
    
    private Pane createOptionsPane() {
        VBox pane = new VBox();
        pane.setPadding(new Insets(5));
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.getStyleClass().add("normalBorder");
        pane.getStyleClass().add("updateStockPane");
        
        HBox options = new HBox(5);
        options.setAlignment(Pos.CENTER_LEFT);
        toggleGroup = new ToggleGroup();
        Pane addPane = createSelectionPane(toggleGroup, true);
        Pane remPane = createSelectionPane(toggleGroup, false);
        
        addOrRemField = new IntegerTextField(4);
        addOrRemField.setMaxWidth(50);
        addOrRemField.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("ok");
        okBtn.setOnAction(this::onOkay);
        addOrRemField.setOnAction(event -> {okBtn.fire();});
        options.getChildren().addAll(addPane, remPane, addOrRemField, okBtn);
        pane.getChildren().add(options);
        return pane;
    }
    
    private Pane createSelectionPane(ToggleGroup group, boolean isAdd) {
        RadioButton selector = new RadioButton();
        selector.setToggleGroup(group);
        ImageView img = new ImageView();
        if (isAdd) {
            selector.setSelected(true);
            addToggle = selector;
            img.setImage(new Image("/baseline-add-black-18/2x/baseline_add_black_18dp.png"));
        } else {
            remToggle = selector;
            img.setImage(new Image("/baseline-remove-black-18/2x/baseline_remove_black_18dp.png"));
        }
        
        Pane pane = new VBox();
        pane.getChildren().addAll(img, selector);
        img.setFitWidth(22);
        img.setPreserveRatio(true);
        pane.setOnMouseClicked(event -> {
            selector.fire();
        });
        return pane;
    }
    
    private Pane createButtonPane() {
    	HBox pane = new HBox(5);
//    	pane.setPadding(new Insets(5));
    	pane.getStyleClass().add("normalBorder");
    	orderBtn = new Button("ORDER");
    	orderBtn.setOnAction(this::onOrder);
    	ImageView setting = new ImageView();
    	setting.setImage(new Image("/baseline-settings-black-18/2x/baseline_settings_black_18dp.png"));
    	HBox imageBox = new HBox(5);
    	imageBox.setOnMouseClicked(this::onSetting);
    	imageBox.getChildren().add(setting);
    	pane.getChildren().addAll(orderBtn, imageBox);
    	pane.setVisible(false);
    	return pane;
    }
    
    /**
     * Handles mouse events on ingredient rows
     * @param event MouseEvent: the event to be handled
     */
    private void handleMouseEvent(MouseEvent event) {
        if (isSelected) ui.select(null);
        else ui.select(ingredient.getName());
    }
    
    private void onOrder(ActionEvent event) {
    	Stage parentStage = ui.getParentStage();
    	OrderModal orderModal = new OrderModal(parentStage,ingredient);
    	orderModal.show();
    }
    
    private void onSetting(MouseEvent event) {
    	Stage parentStage = ui.getParentStage();
    	SettingsModal settingsModal = new SettingsModal(parentStage, ui, ingredient);
    	settingsModal.show();
    	
    }
    
    private void onOkay(ActionEvent event) {
        if (!addOrRemField.getText().equals("")) {
            int value = Integer.parseInt(addOrRemField.getText());
            boolean isAdd = toggleGroup.getSelectedToggle().equals(addToggle);
            if (isAdd) ui.increaseQty(ingredient.getCategory().getName(), ingredient.getName(), value, false);
            else ui.decreaseQty(ingredient.getCategory().getName(), ingredient.getName(), value, false);
            addOrRemField.clear();
        }
    }
    
    public void select() {
        ingredientCell.getStyleClass().add("selectedPane");
        currentStockCell.getStyleClass().add("selectedPane");
        updateStockCell.getStyleClass().add("selectedPane");
        orderAndSettingCell.setVisible(true);
        isSelected = true;
    }
    
    public void deselect() {
        ingredientCell.getStyleClass().remove("selectedPane");
        currentStockCell.getStyleClass().remove("selectedPane");
        updateStockCell.getStyleClass().remove("selectedPane");
        orderAndSettingCell.setVisible(false);
        isSelected = false;
    }
    

    /**
     * Returns the Ingredient corresponding with this row
     * @return Ingredient: the Ingredient corresponding with this row
     */
    public Ingredient getIngredient() {
        return ingredient;
    }

    /**
     * Returns the ingredientCell for use in IngredientsUI
     * @return Pane: the ingredientCell for use in IngredientsUI
     */
    public Pane getIngredientCell() {
        return ingredientCell;
    }

    /**
     * Returns the currentStockCell for use in IngredientsUI
     * @return Pane: the currentStockCell for use in IngredientsUI
     */
    public Pane getCurrentStockCell() {
        return currentStockCell;
    }

    /**
     * Returns the updateStockCell for use in IngredientsUI
     * @return Pane: the updateStockCell for use in IngredientsUI
     */
    public Pane getUpdateStockCell() {
        return updateStockCell;
    }
    
    public Pane getOrderAndSettingCell() {
    	return orderAndSettingCell;
    }
    
    public Button getOrderButton() {
    	return orderBtn;
    }
    
    /**
     * Returns the index for this row in its parent gridpane
     * @return int: the index for this row in its parent gridpane
     */
    public int getRowIndex() {
        return rowIndex;
    }
    
    public void setRowIndex(int index) {
        rowIndex = index;
        GridPane.setRowIndex(ingredientCell, index);
        GridPane.setRowIndex(currentStockCell, index);
        GridPane.setRowIndex(updateStockCell, index);
        GridPane.setRowIndex(orderAndSettingCell, index);
    }
}
