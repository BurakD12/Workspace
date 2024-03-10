Question: Product Inventory Management System

Task: Develop a Product Inventory Management System in Java. This system will consist of multiple classes to represent different types of products and a class to manage the inventory.

------------------------------------------------------------------------------------------

Product Class:

Create a Product class with the following specifications:
Attributes: productName (String), price (double), and quantity (int).
Constructor: Initialize the attributes.
Methods:
    restock(int quantity): Increases the quantity of the product.
    sell(int quantity): Decreases the quantity of the product if enough stock is available. Print a message if the stock is insufficient.
    displayInfo(): Prints the product's name, price, and available quantity.

------------------------------------------------------------------------------------------

Electronics Class:

Create an Electronics class with similar specifications to the Product class, but with an additional attribute and adjusted methods:
Attributes: productName, price, quantity (same as Product), and warrantyPeriod (int, representing months).
Constructor: Initialize all attributes.
Methods: Similar to Product but with displayInfo() also showing the warranty period.

------------------------------------------------------------------------------------------

FoodItem Class:

Create a FoodItem class with similar specifications to the Product class, but with an additional attribute and adjusted methods:
Attributes: productName, price, quantity (same as Product), and expirationDate (String).
Constructor: Initialize all attributes.
Methods: Similar to Product but with displayInfo() also showing the expiration date.

------------------------------------------------------------------------------------------
InventoryManager Class:

Create an InventoryManager class to manage the products:
Attributes: A list to store objects of Product, Electronics, and FoodItem.
Methods:
    addProduct(Object product): Adds a product to the inventory. The product can be an instance of Product, Electronics, or FoodItem.
    displayAllProducts(): Iterates over the list and calls displayInfo() for each product.
    getTotalInventoryValue(): Calculates and returns the total value of all products in the inventory.