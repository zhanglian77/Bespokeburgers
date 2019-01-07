//use ingredients passed from php to alter price etc
console.log(ingredients);

// fired when any dropdown changes and updates the corresponding price field
function onDropdownChange(oldValue, dropdown) {
	let dropdownElement = document.getElementById(dropdown+"Type");
	let ingredient = document.getElementById(dropdown+"Type").value;
	let priceField = document.getElementById(dropdown+"Cost");
	if (dropdown === 'bun') dropdown = 'bread';
	if (ingredient === ""){
		var price = 0.0;
	} else {
		var price = ingredients[dropdown][ingredient].price;
	}
	priceField.value = formatCurrency(price);
	dropdownElement.oldValue = dropdownElement.value;

	let totalPriceField = document.getElementById("totalCost");
	console.log(totalPriceField); 
	
		
	if (oldValue === "") {
		var oldPrice = "$0.00";
	} else {
		var oldPrice = formatCurrency(ingredients[dropdown][oldValue].price.toString());
	}
	totalPriceField.value = getNewTotal(totalPriceField.value, priceField.value, oldPrice);
}

// fired when any quantity changes and updates the corresponding price field
function onQuantityChange(oldValue, category, ingredient){
	if (typeof oldValue != "string" || !oldValue.startsWith("$")){oldValue = formatCurrency(oldValue.toString());}
	let priceField = document.getElementById(ingredient+"Cost");
	let quantity = parseInt(document.getElementById(ingredient+"_qty").value);
	if (category === null){
		var price = parseFloat(findIngredient(ingredient).price) * quantity;
	} else { 
		var price = parseFloat(ingredients[category][ingredient].price) * quantity;
	}
	priceField.value = formatCurrency(price.toString());

	let totalPriceField = document.getElementById("totalCost");
	totalPriceField.value = getNewTotal(totalPriceField.value, priceField.value, oldValue);
	priceField.oldValue = priceField.value;
}

// helper function for the above
function getNewTotal(totalValue, newValue, oldValue){
	totalPrice = parseFloat(totalValue.slice(1, totalValue.length));
	newPrice = parseFloat(newValue.slice(1, totalValue.length));
	oldPrice = parseFloat(oldValue.slice(1, totalValue.length));
// console.log(totalPrice);
// console.log(oldPrice);
// console.log(newPrice);
	return formatCurrency((totalPrice + (newPrice - oldPrice)).toString());
}

// validates characters in namefield as data entered or pasted
function onNameChange() {
	let nameField = document.getElementById('name');
	let name = nameField.value;
	name = name.replace(/NONUM/g, "");// removes all instances of the string "NONUM"
	name = name.replace(/NOSTAMP/g, "");// removes all instances of the string "NOSTAMP"
	name = name.replace(/\W/g, ""); // removes all non alphanumeric characters
	nameField.value = name;
}

// validates and then posts the form on button click
function validate() {
	let isValid = true;
	let bun = document.getElementById('bunType').value;
	let name = document.getElementById('name').value;
	if (bun === "") {
		isValid = false;
		// alert user that it's invalid 'cause no bun
	}
	if (name === "") {
		isValid = false;
		// alert user that they must have a name
	}
	let sauce = document.getElementById('sauceType').value;
	let patty = document.getElementById('pattyType').value;

	// orderNumber,customerName,timestamp,ingredientCategory,ingredientName,num,ingredientCategory,ingredientName,num
	let order = "ORDER,NONUM,"+name+",NOSTAMP,bread,"+bun+",1";
	if (sauce.length > 0) order +=",sauce,"+sauce+",1";
	if (patty.length > 0) order +=",patty,"+patty+",1";	
		

	// ingredients.category.ingredient
	let notMiscCategories = ["sauce", "patty", "bread"];
	for (let category of Object.keys(ingredients)) {
		if (!notMiscCategories.includes(category)){
			for (let ingredient of Object.keys(ingredients[category])){
				console.log(ingredient);
				let quantity = document.getElementById(ingredients[category][ingredient].name+'_qty').value;
				if (quantity > 0){
					order += ","+category+","+ingredients[category][ingredient].name+","+quantity;
				}

			}

		}
	}

	
//	order = order.slice(0,-1);
	order += "\r\n";
	
	console.log(order);
	
	fetch("submitOrder.php", {
		method: 'POST',
		headers: { 'Content-type': 'application/x-www-form-urlencoded', },
		body: 'order=' + encodeURIComponent(order)
	}).then(function(response){
		console.log("fetching");
		return response;
	}).then(function(result) {
		return result.text();
	}).then(function(text){

		if (text.startsWith("0,")){
			console.log("Failed, order starts with 0");
			// failed. Display a message and refresh the ingredients
			window.alert("Your order didn't work. 1 or more of the ingredients you selected is no longer available. " +
					"Please refresh the page to load available ingredients.");
		} else if (text.startsWith("1,")){
			console.log("Yay, order starts with 1");
			// success. Forward to successfulOrderPage with the order number.
			// "1,263467"
			console.log("Text: "+ text);
			console.log(text);
			console.log("Order Global: " + order);
			let serverOrder = text.split(",");
			console.log("Split text: " + serverOrder[0] + "," + serverOrder[1] + "," + serverOrder[2]);
			let orderNum = serverOrder[1];
			
			orderSplit = order.split(",");
			order = orderSplit;
			console.log("userName is : " + name);
			
			let totalPriceField = document.getElementById("totalCost");
			let totalPrice = totalPriceField.value;
			

			
			window.location = "successfulOrderPage.php?orderNum=" + orderNum + "&order=" + order 
			+ "&name=" + name + "&totalPrice=" + totalPrice; 
		} 
	});



}

function incrementValue(e) {
	e.preventDefault();
	let fieldName = $(e.target).data('field');
	let parent = $(e.target).closest('div');
	let currentVal = parseInt(parent.find('input[name=' + fieldName + ']')
			.val(), 10);

	if (!isNaN(currentVal)) {
		let ingredient = parent.find('input[name=' + fieldName + ']').attr('id').replace("_qty", "");
		if (findIngredient(ingredient).quantity > currentVal){
			parent.find('input[name=' + fieldName + ']').val(currentVal + 1);
			let oldPrice = parseFloat(findIngredient(ingredient).price) * currentVal;
			onQuantityChange(oldPrice, null, ingredient);
		}
	} else {
		parent.find('input[name=' + fieldName + ']').val(0);
	}
}

function decrementValue(e) {
	e.preventDefault();
	let fieldName = $(e.target).data('field');
	let parent = $(e.target).closest('div');
	let currentVal = parseInt(parent.find('input[name=' + fieldName + ']')
			.val(), 10);

	if (!isNaN(currentVal) && currentVal > 0) {
		parent.find('input[name=' + fieldName + ']').val(currentVal - 1);
		let ingredient = parent.find('input[name=' + fieldName + ']').attr('id').replace("_qty", "");
		let oldPrice = parseFloat(findIngredient(ingredient).price) * currentVal;
		onQuantityChange(oldPrice, null, ingredient);
	} else {
		parent.find('input[name=' + fieldName + ']').val(0);
	}
}

// finds an ingredient object based on its name
function findIngredient(toFind){
	for (let category of Object.keys(ingredients)) {
		for (let ingredient of Object.keys(ingredients[category])){
			if (ingredients[category][ingredient].name === toFind) {
				return ingredients[category][ingredient];
			}
		}
	}
}

// formats a string representation of a double as a currency
function formatCurrency(price){
	price = "$"+price;
	let tokens = price.split(".");
	if (tokens.length < 0 || tokens.length > 2){
		// oh no!!
	} else if (tokens.length === 1) {
		price += ".00";
	} else {
		if (tokens[1].length > 2){
			// reduce to just 2 chars after decimal
			price = price.slice(0, price.length-(tokens[1].length-2));
		} else if (tokens[1].length === 1){
			price += "0";
		} else if (tokens[1].length === 0){
			price += "00";
		}
	}
	return price;
}

$('.input-group').on('click', '.button-plus', function(e) {
	incrementValue(e);
});

$('.input-group').on('click', '.button-minus', function(e) {
	decrementValue(e);
});