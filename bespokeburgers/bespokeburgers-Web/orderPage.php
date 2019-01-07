<!DOCTYPE html>

<?php

	// <!--create socket connect-->
	$host    = "127.0.0.1";
	$port    = 9090;
	$socket = socket_create(AF_INET, SOCK_STREAM, 0);
	$result = socket_connect($socket, $host, $port);
	if ($result === true) {
    	$message1 = "RGSTR,WEB\r\n";
    	socket_write($socket, $message1, strlen($message1)) or die("Could not send data to server\n");
    	//<!--request categories & ingredients, call getIngredients and parse into array(5), for each item check quantity greater than 0 and display if so-->
    
    
    	$message2 = "REQ_CAT\r\n";
    	socket_write($socket, $message2, strlen($message2)) or die("Could not send data to server\n");
    	$categoriesRaw = rtrim(socket_read ($socket, 1024, PHP_NORMAL_READ)) or die("Could not read server response\n");
    	$categoriesRaw = str_replace("SEND_CAT,", "", $categoriesRaw);
    	$categories = explode(",", $categoriesRaw);
    	
    	$message3 = "DERGSTR\r\n";
    	socket_write($socket, $message3, strlen($message3));
    	socket_close($socket);
    	$socket = socket_create(AF_INET, SOCK_STREAM, 0);
		$result = socket_connect($socket, $host, $port);
    	socket_write($socket, $message1, strlen($message1)) or die("Could not send data to server\n");
    	//<!--request categories & ingredients, call getIngredients and parse into array(5), for each item check quantity greater than 0 and display if so-->
    
    
    	$message3 = "REQ_INGR\r\n";
    	socket_write($socket, $message3, strlen($message3)) or die("Could not send data to server\n");
    	$ingredientsRaw = rtrim(socket_read($socket, 1024, PHP_NORMAL_READ)) or die("Could not read server response\n");
    	$ingredientsRaw = str_replace("SEND_INGR,", "", $ingredientsRaw);
    	$ingredients1D = explode(",", "$ingredientsRaw");
    	
    	//parse ingredients string to multidimensional associative array
    	$ingredients = array();
    	$category = "NONE";
    	$ingredient = "NONE";
    	for ($i = 0; $i < sizeof($ingredients1D); $i++){
    		switch ($i % 5) {
    		    case 0:
        	        $category = $ingredients1D[$i];
        	        break;
            	case 1:
            	    $ingredient = str_replace(" ", "_", $ingredients1D[$i]);
            	    $ingredients[$category][$ingredient] = array();
            	    $ingredients[$category][$ingredient]["name"] = $ingredient;
                	break;
        		case 2:
        		    $ingredients[$category][$ingredient]["price"] = doubleval($ingredients1D[$i]);
        			break;
        		case 3:
        		    $ingredients[$category][$ingredient]["quantity"] = intval($ingredients1D[$i]);
        			break;
    		}
    	}
    	
    	//send ingredients to JS
    	echo "<script>";
    	   echo"var ingredients = "; echo json_encode($ingredients, JSON_HEX_TAG);
    	echo "</script>";
    	
    	//for debug purposes only
//     	print_r($categoriesRaw);
//     	echo"<br>";
//     	print_r($categories);
//     	echo"<br>";
//     	print_r($ingredientsRaw);
//     	echo"<br><br>";
//     	print_r($ingredients);
//     	echo"<br>";
//
    
    
    	//<!--DRGSTR and close the socket. -->
    	$message3 = "DERGSTR\r\n";
    	socket_write($socket, $message3, strlen($message3));
    	socket_close($socket);
	} else {
	    echo "Error: Could not connect to ingredients server";
	}

?>

<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="style.css">
<link href="https://fonts.googleapis.com/css?family=Fira+Sans" rel="stylesheet">
</head>
<body>
	<div class="container">
	<div class="header">
		<h1>BESPOKE BURGERS</h1>
	</div>
	<div class="content">
				<h2>THE ULTIMATE BURGER</h2>
				<form name="orderform" onsubmit="event.preventDefault(); validate();">
					<div class="row">
						<div class="column"><label for="bun" id="mainlabel">Choose Bun: </label></div>
    						<div class="column"><select id="bunType" name="bun_type" onchange="onDropdownChange(this.oldValue, 'bun');" onfocus="this.oldValue = this.value;">
    							<option value=""></option>
    							<?php
    							foreach ($ingredients["bread"] as $ingredient) {
    							    if ($ingredient["quantity"] > 0){
    							      echo("<option value = \"$ingredient[name]\">$ingredient[name]</option>");
    							     }
    							}
    							?>
    						</select></div>
						<div class="column"><input type="text" id="bunCost" class="cost" name="bunCost" value="$0.00" disabled></input></div>
						</div>
						<div class="row">
						<div class="column"><label for="ingredients" id="mainlabel">Choose Ingredients:</label></div>
						</div>
						<div class="row">
						<?php 
						//NOT patty, bread, sauce
						foreach ($categories as $category){
						    if ($category != "patty" && $category != "bread" && $category != "sauce"){
						        if (array_key_exists($category, $ingredients)){
    						        foreach($ingredients["$category"] as $ingredient){
    						            if ($ingredient["quantity"] > 0){
    						            ?>
    						              <div class="input-group" id="<?=$ingredient["name"]?>">
    						                  <div class="column"><label for="<?=$ingredient["name"]?>"><?=$ingredient["name"]?>:</label></div>
    						                  <div class="column"><input type="button" value="-" class="button-minus" data-field="quantity">
    						                  <input type="number" step="1" max="" value="0" name="quantity" id="<?=$ingredient["name"]?>_qty" class="quantity-field");" disabled>
    						                  		 <!--  onChange="onQuantityChange('<?=$category?>', '<?=$ingredient["name"]?>' onfocus="this.oldValue = this.value;" -->
    						                  <input type="button" value="+" class="button-plus" data-field="quantity">
    						                  <span class="mustard">$<?=doubleval($ingredient["price"])?></span></div>
    						                  <div class="column"><input type="text" class="cost" name="<?=$ingredient["name"]?>Cost" id="<?=$ingredient["name"]?>Cost" value="$0.00" disabled></input></div>
    						              </div>
    						            <?php
    						            }
    						        }
						        }
						    }
						}
						
						?>
						
						<div class="row">
						<div class="column"><label for="sauce" id="mainlabel">Choose Sauce:</label></div> 
							<div class="column"><select id="sauceType" name="sauce_type" onchange="onDropdownChange(this.oldValue, 'sauce');" onfocus="this.oldValue = this.value;">
								<option value=""></option>
								<?php
    							foreach ($ingredients["sauce"] as $ingredient) {
    							    if ($ingredient["quantity"] > 0){
    							     echo("<option value = \"$ingredient[name]\">$ingredient[name]</option>");
    							    }
    							}
    							?>

							</select></div>
							<div class="column"><input type="text" class="cost" id="sauceCost" name="sauceCost" value="$0.00" disabled onchange="onDropdownChange(this.oldValue, 'sauce');" onfocus="this.oldValue = this.value;"></input></div>
						</div>

						<div class="row">
							<div class="column"><label for="patty" id="mainlabel">Choose Patty:</label></div>
							<div class="column"><select id="pattyType" name="patty_type" onchange="onDropdownChange(this.oldValue, 'patty');" onfocus="this.oldValue = this.value;">

								<option value=""></option>
								<?php
    							foreach ($ingredients["patty"] as $ingredient) {
    							    if ($ingredient["quantity"] > 0){
    							     echo("<option value = \"$ingredient[name]\">$ingredient[name]</option>");
    							    }
    							}
    							?>

							</select></div>
							<div class="column"><input type="text" class="cost" id="pattyCost" name="pattyCost" value="$0.00" disabled onchange="onDropdownChange(this.oldValue, 'patty');" onfocus="this.oldValue = this.value;"></input></div>

						</div>
						<div class="row">
							<div class="column"><label for="name" id="mainlabel">Customer Name:</label></div> 
							<div class="column"><input type="text" id="name" name="order_name" onkeydown="onNameChange(this.oldValue);" onpaste="onNameChange();" oninput="onNameChange();"></div>
						</div>
						<div class="row">
						<div class="column"><label for="cost" id="totalCostLabel">Total Cost:</label></div>
							<div class="column"><label></label></div>
						<div class="column"><input type="text" class="cost" name="totalCost" id="totalCost" value="$0.00" disabled></input></div>
						</div>
					<div class="row">
					<td colspan="3"><div class="button">
						<button type="submit" id="orderButton">Submit Order</button>
					</div></div>
					</div>
					
				</form>
			</div>
			<div class="footer">
			<div class="navbar">
				<a href="index.html">Homepage</a> 
				<a class="active" href="orderPage.php">Order</a> 
				<a href="menuPage.html">Menu</a>
				<a href="contactPage.html">Contact Us</a>
			</div>
		</div>
			</div>
			<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
<script src="script.js"></script>
</body>
</html>
