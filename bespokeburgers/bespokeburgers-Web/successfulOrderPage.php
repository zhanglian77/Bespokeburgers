<!DOCTYPE html>

<!-- //
// //this handy function from https://stackoverflow.com/a/834355
// function startsWith($haystack, $needle)
// {
//     $length = strlen($needle);
//     return (substr($haystack, 0, $length) === $needle);
// }

// // $bun = ($_POST['bun_type']);
// // $sauce = ($_POST['sauce_type']);
// // $patty = ($_POST['patty_type']);
// //qty is ingredientName_qty
// $cost = ($_POST['totalCost']);
// $name = ($_POST['order_name']);


// $ingredients = ($_POST['ingredients']);


// $host = "127.0.0.1";
// $port = 9090;
// $socket = socket_create(AF_INET, SOCK_STREAM, 0) or die("Could not create socket\n");
// $result = socket_connect($socket, $host, $port) or die("Could not connect to server\n"); 

// $message1 = "RGSTR,WEB\r\n";
// socket_write($socket, $message1, strlen($message1)) or die("Could not send data to server\n");

// $message2 = "ORDER,NONUM,$name,$ingredients\r\n";
// socket_write($socket, $message2, strlen($message2)) or die("Could not send data to server\n");

// // get server response
// $result = socket_read ($socket, 1024) or die("Could not read server response\n");
// if (startsWith($result, "0,")){ //failed. Redirect to order page.
//     session_start();
//     $_SESSION['order_status'] = 'failed';
//     header('Location: orderPage.php', true, 303); die();
// } elseif (startsWith($result, "1,")){
//     $orderNumber = intval(explode($result)[1]);
// }

// $message3 = "DERGTR\r\n";
// socket_write($socket, $message3, strlen($message3));
// socket_close($socket);
//  -->


<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<link rel="stylesheet" href="style.css">
<link href="https://fonts.googleapis.com/css?family=Fira+Sans"
	rel="stylesheet">
</head>
<body>
	<div class="container">
		<div class="header">
			<h1>BESPOKE BURGERS</h1>
		</div>
		<div class="content">
			<h2>SUCCESSFUL ORDER</h2>
			<div class="textMain" id="successfulOrder">

				<p>CONGRATS:  <?php
    echo $_GET['name'];
    
    ?>
    </p>
				<p>
					<!--  ADD ORDER DETAILS BACK IN -->
					Your order for: <br> <?php
    
    if (! empty($_GET['order'])) {
        
        $ingredient = str_replace("ORDER,NONUM," . $_GET['name'] . ",NOSTAMP,", "", $_GET['order']);
        
        $ingredient1D = explode(",", $ingredient);
        
        for ($i = 0; $i < sizeof($ingredient1D); $i ++) {
            
            $i ++;
            $ingredientName = $ingredient1D[$i];
            $i ++;
            
            echo $ingredient1D[$i] . " " . $ingredientName;
            echo "<br>";
        }
    }
    ?> 
		</p>
				<p>Your order number is  # <?php
    
    if (! empty($_GET['orderNum'])) {
        echo $_GET['orderNum'];
    }
    
    ?>
       </p>
				<p> Price to pay: <?php echo $_GET["totalPrice"]; ?> </p>
				<p>We have received your order and it will be ready shortly.</p>
			</div>
		
	<?php

?>
</div>
		<div class="footer">
			<div class="navbar">
				<a href="index.html">Homepage</a> <a href="orderPage.php">Order</a>
				<a href="menuPage.html">Menu</a> <a href="contactPage.html">Contact
					Us</a>
			</div>
		</div>
	</div>
</body>
