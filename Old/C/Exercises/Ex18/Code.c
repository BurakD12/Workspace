#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct
{
    int ID;
    char name[50];
    int age;
} Animal;

typedef struct{
    char name[50];
    struct Habitat *habitat; // Forward decleration 
    Animal *animals;
    int animalCount;
}Species;

typedef struct{
    char name[50];
    int capacity;
    int currentOccupancy;
    Species *speciesList;
}Habitat;

Habitat *create_habitat(const char *name, int capacity)
{
    Habitat *habitat = (Habitat *)malloc(sizeof(Habitat));
    if (habitat == NULL){
        printf("Error allocating memory!");
        return NULL;
    }
    strcpy(habitat->name, name);
    habitat->capacity = capacity;
    habitat->currentOccupancy = 0;
    habitat->speciesList = NULL;
    return habitat;
}

Species *add_species_to_habitat(Habitat *habitat, const char *species_name)
{
    Species *new_species = (Species *)malloc(sizeof(Species));
    if (new_species == NULL)
    {
        printf("Error allocating memory!");
        return NULL;
    }
    strcpy(new_species->name, species_name);
    new_species->habitat = habitat;
    new_species->animalCount = 0;
    new_species->animals = NULL;
    habitat->speciesList = (Species *)realloc(habitat->speciesList, (habitat->currentOccupancy + 1) * sizeof(Species));
    habitat->speciesList[habitat->currentOccupancy] = *new_species;
    habitat->currentOccupancy++;
    return new_species;
}

void add_animal_to_species(Species *species, int ID, const char *animal_name, int age)
{
    Animal *new_animal = (Animal *)malloc(sizeof(Animal));
    if (new_animal == NULL)
    {
        printf("Memory Allocation Error!");
        return NULL;
    }
    new_animal->age = age;
    new_animal->ID = ID;
    strcpy(new_animal->name, animal_name);
    species->animals = realloc(species->animals, (species->animalCount + 1) * sizeof(Animal));
    species->animals[species->animalCount] = *new_animal;
    species->animalCount++;
}

void transfer_animal(Species *from_species, Species *to_species, int animal_id)
{
    // Check if the animal exists in from_species
    int animal_index = -1;
    for (int j = 0; j < from_species->animalCount; j++){
        if (from_species->animals[j].ID == animal_id){
            animal_index = j;
            break;
        }
    }
    if (animal_index == -1){
        printf("Animal does not exist!");
        return;
    }

    // Copy the animl to to_species

    Animal target_animal = from_species->animals[animal_index];
    to_species->animals = (Animal *)realloc(to_species->animals, (to_species->animalCount + 1) * sizeof(Animal));
    if (to_species->animals == NULL)
    {
        printf("Error in memory allocation!");
        return;
    }
    to_species->animals[to_species->animalCount] = target_animal;
    to_species->animalCount++;

    // Remove the animal from from_species

    for (int i = animal_index; i < from_species->animalCount; i++)
    {
        from_species->animals[i] = from_species->animals[i + 1];
    }
    from_species->animals = (Animal *)realloc(from_species->animals, (--from_species->animalCount) * sizeof(Animal));
    if (from_species->animals == NULL){
        printf("Error in memory allocation!");
    }
}

void print_habitat_info(const Habitat *habitat)
{
    printf("Name of the habitat: %s", habitat->name);
    printf("\nHabitat capacity: %d", habitat->capacity);
    printf("\nNumber of occupants: %d", habitat->currentOccupancy);
    for (int i = 0; i < habitat->currentOccupancy; i++)
    {
        printf("\n  Species name: %s", habitat->speciesList[i].name);
        for (int j = 0; j < habitat->speciesList->animalCount; j++)
        {
            Animal animal = habitat->speciesList[i].animals[j];
            printf("      AnimalID: %d\n",animal.ID);
            printf("      Name: %s\n",animal.name);
            printf("      Age: %d\n",animal.age);
        }
    }
}

void free_habitat(Habitat *habitat)
{
    for (int i = 0; i < habitat->currentOccupancy; i++)
    {
        free(habitat->speciesList[i].animals);
        habitat->speciesList[i].animals = NULL;
    }
    free(habitat->speciesList);
    habitat->speciesList = NULL;
    free(habitat);
}