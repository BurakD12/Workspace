Create a base class named Vehicle with the following specifications:

    * A private field name of type String.
    * A constructor that accepts a String to initialize the name field.
    * A public method displayInfo() that prints "Vehicle name: [name]" where [name] is the value of the name field.

Create a derived class named Car that extends Vehicle with the following additional specifications:

    * A private field type of type String.
    * A constructor that accepts two String arguments. The first argument should be passed to the base class constructor, and the second should initialize the type field.
    * Override the displayInfo() method to include the type of the car. The overridden method should print "Vehicle name: [name], Type: [type]".

In a main method, create an instance of Car and call its displayInfo() method to demonstrate that your code is working as expected.

Sample Output:
If the Car object is created with the name "Tesla" and the type "Electric", the output should be:

Vehicle name: Tesla, Type: Electric
