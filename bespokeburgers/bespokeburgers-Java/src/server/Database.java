package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static protocol.Protocol.*;

/**
 * Handles all queries, updates, and table alterations to/from the database
 * @author Bespoke Burgers
 *
 */
public class Database {
	//JDBC constants
	private static final String USERNAME = "zhanglian4" ;
	private static final String PASSWORD = "123";
	private static final String DB_URL = "jdbc:postgresql://db.ecs.vuw.ac.nz/zhanglian4_jdbc";


	/**
     * Convenience method for connecting to the database per request 
     * @return Statement: a blank statement for querying or updating
     */
	private static Connection connect() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String url = DB_URL;
        Connection connection;
        try {
            connection = DriverManager.getConnection(url, USERNAME, PASSWORD);
            return connection;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null; 
        }
    }

	/**
	 * Convenience method to handle all SQL queries
	 * @param query String: contains the SQL structured query
	 * @return ResultSet containing response from database
	 */
	private static ResultSet query(String query) {
        Connection db = connect();
        if (db == null) return null;
        ResultSet rs;
        try {
            rs = db.createStatement().executeQuery(query);
            db.close();
            return rs;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }


	/**
	 * Convenience method to handle all SQL updates
	 * @param update String: contains the SQL structured update request
	 * @return String containing the response from database
	 */
	private static short update(String update) {
        Connection db = connect();
        if (db == null) return ERROR;
        short result;
        try {
            result = (short) db.createStatement().executeUpdate(update);
            db.close();
            return result;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return ERROR; 
        }
    }

	/**
	 * Get all orders from the database in the format "orderNumber,customerName,timestamp,status,ingredientCategory,ingredientName,num,ingredientCategory,ingredientName,num(etc),orderNumber2,customerName2,timestamp2 etc"
	 * @return String representing all orders in the database
	 */
	public static String getOrders() {
		ResultSet rs = query("select * from orders");

		try {
		    String result = "";
			while(rs.next()) {
			    String status = rs.getString("status").trim();
				int orderNumber = rs.getInt("order_number");
				String customerName = rs.getString("customer_name").trim();
				String orderDetails = rs.getString("order_details").trim();
				int cost =rs.getInt("cost");
				String timestamp = rs.getString("time_stamp").trim();
				result += String.valueOf(orderNumber) + DELIM + customerName + DELIM + timestamp + DELIM + status + DELIM + orderDetails;
//				System.out.println(result);
//				result = result.substring(0, result.length() - ORDER_DELIM.length());
				result += ORDER_DELIM;
			}
			return result;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (NullPointerException e1) { 
		    return null;
		}
	}

	/**
	 * Enter a new order into the database
	 * @param id unique identifier for this order
	 * @param ingredients list of all ingredients and their quantity
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short addOrder(int id, String customerName, String timestamp, String status, String ingredients) {
		short feedback = update("insert into orders(order_number, customer_name, order_details, cost, time_stamp, status) "
				+ "values (" + id + "," + "'" + customerName + "'," + " '" +ingredients + "',0.00, '" + timestamp + "'" + ", '" + status + "'" +")");
		return feedback;
	}
	
	
	public static short changeOrderStatus(int id, String timestamp, String status) {
	    short feedback = update("update orders set status = '"+ status + "' where order_number = " + id + " AND time_stamp = '" + timestamp + "'");
        return feedback;
	}
	
	public static void deleteme() {
	    update("ALTER TABLE orders\n" + 
	            "ALTER COLUMN order_details TYPE varchar(1024);");
	}



	/**
	 * Get all ingredients from the database in the format "category1,ingredient1,price,quantity,minThreshold,category2,ingredient2,price,num,minThreshold etc"
	 * @return String representing all ingredients in the database
	 */
	public static String getIngredients(){
		ResultSet rs = query("select * from ingredients");
		try {
		    String result = "";
			while(rs.next()) {
				String ingredientName = rs.getString("ingredient_name").trim();
				double price = rs.getDouble("price");
				int quantity =rs.getInt("quantity");
				String category = rs.getString("category").trim();
				int minThreshold = rs.getInt("minThreshold");
				result += category +DELIM+ ingredientName +DELIM+ String.valueOf(price) +DELIM+ 
						String.valueOf(quantity) +DELIM+ String.valueOf(minThreshold);
//				System.out.println(result);
                result += DELIM;
			}
			result = result.substring(0, result.length() - DELIM.length());//remove final DELIM
            return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}	
	}

	/**
	 * Add a new ingredient to the database
	 * @param ingredient String: name of the ingredient (e.g. lettuce)
	 * @param category String: category of the ingredient (e.g. salad)
	 * @param quantity int: number of ingredient in stock
	 * @param minThreshold int: minimum number in stock before shop is notified to restock
	 * @param price double: cost to customer per unit
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short addIngredient(String ingredient, double price, int quantity, String category,  int minThreshold) {
	    short feedback = update("insert into ingredients (ingredient_name, price, quantity, category, minThreshold) "
				+ "values (" + "'"+ ingredient +"'" + ", " + price + ", " + quantity +", " + "'" + category + "'" + ", " + minThreshold + ")");
		return feedback;
	}

	/**
	 * Remove an ingredient from the database
	 * @param ingredient String: name of the ingredient (e.g. lettuce)
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short removeIngredient(String ingredient){
		short feedback = FAILURE;
		
		try {
			ResultSet rs = query("select * from ingredients where ingredient_name = '" + ingredient + "'");
			if (!rs.next()) { 
				feedback = FAILURE;
				return feedback;
			}
			feedback = update("delete from ingredients where ingredient_name = '" + ingredient + "'");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return feedback;
	}
	
	/**
	 * Update the price of an ingredient in the database
	 * @param ingredient String: name of the ingredient (e.g. lettuce)
	 * @param price double: cost to customer per unit
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short updatePrice(String ingredient, double price) {
		short feedback = update("update ingredients set price = "+ price + " where ingredient_name = " + "'"
				+ingredient + "' ");
//		System.out.println(feedback);
		return feedback;
	}

	/**
	 * Increase the number of an ingredient in stock
	 * @param ingredient String: name of the ingredient (e.g. lettuce)
	 * @param byAmount int: number by which to increase the quantity
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short increaseQty(String ingredient, int byAmount){
		short feedback = update("update ingredients set quantity = quantity +"+ byAmount + " where ingredient_name = " + "'"
				+ingredient + "' ");
//		System.out.println(feedback);
		return feedback;
	}

	/**
	 * Decrease the number of an ingredient in stock
	 * @param ingredient String: name of the ingredient (e.g. lettuce)
	 * @param byAmount int: number by which to decrease the quantity
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short decreaseQty(String ingredient, int byAmount){
		short feedback = FAILURE;
		try {
			ResultSet rs = query(String.format("SELECT quantity FROM ingredients WHERE ingredient_name = '%s'", ingredient));
			rs.next();
			int quantity = rs.getInt("quantity");
			if (quantity <= 0 || quantity < byAmount) {
				feedback = FAILURE;
//				System.out.println(feedback);
				return feedback;
			} else {
			    feedback = update(String.format("update ingredients set quantity = quantity - %d where ingredient_name = '%s'", byAmount, ingredient));
			}
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return feedback;
	}

	/**
	 * Update the minimum allowed quantity of a specific ingredient
	 * @param ingredient String: name of the ingredient (e.g. lettuce)
	 * @param threshold int: minimum number in stock before shop is notified to restock
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short updateThreshold(String ingredient, int threshold){
		short feedback = FAILURE;
		
		try {
			ResultSet rs = query("select minthreshold from ingredients where ingredient_name = '" + ingredient + "'");
			int minthreshold = rs.getInt("minthreshold");
			if (threshold <= 0 || minthreshold <= 0) {
				feedback = FAILURE;
//				System.out.println(feedback);
				return feedback;
			}
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		feedback = update("update ingredients set minThreshold = "+ threshold + " where ingredient_name = " + "'"
				+ingredient + "' ");
//		System.out.println(feedback);
		
		return feedback;
	}

	/**
	 * Get all categories from the database in the format "category1,category2,category2 etc"
	 * Note that category order is the order in which they are to be displayed.
	 * @return String representing all categories in the database
	 */
	public static String getCategories() {
		String result = "";
		
		try {
			ResultSet rs = query("select category from category order by sequence");
			rs.next();
			result += rs.getString("category").trim();
			while(rs.next()) {
				String category = rs.getString("category").trim();
				result += DELIM + category;
			}
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return result;
	}

	/**
	 * Provide a new ordering for a category in the database
	 * @param category String: ingredient category (e.g. salad)
	 * @param newOrder int: number representing the order for this category (e.g 1 is the first category to be displayed)
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short reorderCategory(String category, int newOrder){
		//TODO
		short feedback = update("update category set sequence = " + newOrder +"where category = '" + category + "'");
//		System.out.println(feedback);
		return feedback;
	}

	/**
	 * Add a new category to the database
	 * @param category String: ingredient category (e.g. salad)
	 * @param order int: number representing the order for this category (e.g 1 is the first category to be displayed)
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short addCategory(String category, int order) {
		short feedback = update("insert into category (category, sequence) values ('"+ category + "', " + order +")");
//		System.out.println(feedback);
		return feedback;
	}

	/**
	 * Remove a category from the database
	 * @param category String: ingredient category (e.g. salad)
	 * @return short corresponding to Protocol.ERROR/FAILURE/SUCCESS
	 */
	public static short removeCategory(String category){
		short feedback = update("delete from category where category = '" + category +"'");
//		System.out.println(feedback);
		return feedback;

	}
	
	public static String getLatestOrder() {
	    //MAX(<numeric column>) FROM <table>;
	    ResultSet rs1 = query("select MAX(order_number) from orders where time_stamp = (select MAX(time_stamp) from orders)");
	    ResultSet rs2 = query("select MAX(time_stamp) from orders");
        
        try {
            rs1.next();
            rs2.next();
            String result = rs1.getInt(rs1.getRow()) + DELIM + rs2.getString(rs1.getRow()).trim();
            return result;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
	}

}
