package client;

/**
 * Listener to invoke UI changes when the quantity of an ingredient is altered
 * @author Bespoke Burgers
 *
 */
public interface IngredientQuantityListener {
    
    /**
     * Invoked when the quantity of an ingredient is changed.
     * @param ingredient Ingredient: the ingredient whose quantity was changed
     * @param quantity int: the new quantity of the ingredient
     * @param threshold int: the minimum acceptable quantity of the ingredient
     */
    public void onQuantityChange(Ingredient ingredient, int quantity, int threshold);
    
}
