Question: Enhanced Vehicle Management System

Task: You are required to expand the vehicle management system using Java. Your system will consist of multiple classes representing different types of vehicles and a class to manage these vehicles. Instead of implementing an interface, you will directly incorporate methods within each vehicle class.

Car Class:

Create a Car class with the following specifications:
Attributes: make (String), model (String), and year (int).
Constructor: Initialize the attributes.
Methods:
    start(): Prints a message indicating the car is starting.
    stop(): Prints a message indicating the car is stopping.
    displayInfo(): Prints the car's make, model, and year.


Truck Class:

Create a Truck class with the following specifications:
Attributes: make (String), model (String), year (int), and payloadCapacity (double).
Constructor: Initialize the attributes.
Methods:
    start(): Prints a message indicating the truck is starting.
    stop(): Prints a message indicating the truck is stopping.
    displayInfo(): Prints the truck's make, model, year, and payload capacity.


VehicleManager Class:

Create a VehicleManager class to manage the vehicles:
Attributes: A list to store Car and Truck objects.
Methods:
    addVehicle(Object vehicle): Adds a vehicle (Car or Truck) to the management system.
    displayAllVehicles(): Iterates over the list and calls displayInfo() for each vehicle.
    startAllVehicles(): Iterates over the list and calls start() for each vehicle.
    stopAllVehicles(): Iterates over the list and calls stop() for each vehicle.
    findVehiclesByMake(String make): Iterates over the list and returns a list of all vehicles (both Cars and Trucks) of a specific make.

Your Task:

Implement the Car and Truck classes with their respective attributes and methods.
Implement the VehicleManager class with methods to manage the vehicles effectively.
Ensure that the methods in VehicleManager handle both Car and Truck objects correctly and perform the intended operations.
This task will test your ability to work with classes, objects, and basic collection management in Java.
