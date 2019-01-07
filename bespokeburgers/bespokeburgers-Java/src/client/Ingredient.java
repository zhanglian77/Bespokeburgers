package client;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds specific information about ingredient state (e.g. quantity and price) for display.
 * @author Bespoke Burgers
 *
 */
public class Ingredient implements Comparable<Ingredient> {
    private Category category;
    private String name;
    private double price;
    private int quantity;
    private int minThreshold;
    private Map<Integer, IngredientQuantityListener> quantityListeners;

    /**
     * Constructor
     * @param category String: category of the ingredient (e.g. salad)
     * @param name String: name of the ingredient (e.g. lettuce)
     * @param quantity int: number of ingredient in stock
     * @param minThreshold int: minimum number in stock before shop is notified to restock
     * @param price double: cost to customer per unit
     */
    public Ingredient(Category category, String name, int quantity, int minThreshold, double price) {
        this.category = category;
        this.name = name;
        this.quantity = quantity;
        this.minThreshold = minThreshold;
        this.price = price;
        this.quantityListeners = new HashMap<Integer, IngredientQuantityListener>();
    }
    
    /**
     * Returns the current quantity of this ingredient
     * @return int: the current quantity of this ingredient
     */
    public int getQuantity() {
        return this.quantity;
    }
    
    /**
     * Sets the quantity of this ingredient
     * @param quantity int: the new quantity of this ingredient
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        for (IngredientQuantityListener listener : quantityListeners.values()) {
            listener.onQuantityChange(this, this.quantity, this.minThreshold);
        }
    }
    
    /**
     * Returns the cost to customer per unit of this ingredient
     * @return double: cost to customer per unit of this ingredient
     */
    public double getPrice() {
        return this.price;
    }
    
    /**
     * Sets the cost to customer per unit of this ingredient
     * @param price double: new cost to customer per unit of this ingredient
     */
    public void setPrice(double price) {
        this.price = price;
    }
    
    /**
     * Returns the category to which this ingredient belongs
     * @return Category: the category to which this ingredient belongs
     */
    public Category getCategory() {
        return this.category;
    }
    
    public void setCategory(Category category) {
    	this.category = category;
    }
    
    /**
     * Returns the name of this ingredient
     * @return String: the name of this ingredient
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Returns the minimum acceptable quantity of this ingredient
     * @return int: the minimum acceptable quantity of this ingredient
     */
    public int getMinThreshold() {
        return this.minThreshold;
    }
    
    /**
     * Sets the minimum acceptable quantity of this ingredient
     * @param threshold int: the new minimum acceptable quantity of this ingredient
     */
    public void setMinThreshold(int threshold) {
        this.minThreshold = threshold;
    }
    
    /**
     * Adds a listener to the quantity property of this ingredient.<br>
     * The listener has one method onQuantityChange which accepts the Ingredient object, its new quantity,
     * and its minimum threshold as arguments. 
     * @param orderID int: The identifier for the order to which this listener belongs
     * @param listener IngredientQuantityListener: the listener being added
     */
    public void addQuantityListener(int orderID, IngredientQuantityListener listener) {
        quantityListeners.put(orderID, listener);
    }
    
    /**
     * Removes a listener from the quantity property of this ingredient.
     * @param orderID int: The identifier for the order to which this listener belongs
     */
    public void removeQuantityListener(int orderID) {
        quantityListeners.remove(orderID);
    }
    
    @Override
    public int compareTo(Ingredient other) {
        int categoryComparison = this.category.compareTo(other.category);
        return (categoryComparison == 0) ? this.name.compareTo(other.name) : categoryComparison;
    }

}
