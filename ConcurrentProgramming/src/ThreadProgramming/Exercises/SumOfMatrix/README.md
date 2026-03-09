# Thread Programming Exercise: Summing a Large Matrix with Multiple Threads

## Problem Statement

You are tasked with writing a Java program that demonstrates the power of multithreading by summing the elements of a very large matrix. The goal is to compare the performance of different thread counts when processing a 10,000 x 10,000 matrix.

### Requirements

1. **Matrix Generation**
    - Generate a matrix of size **10,000 x 10,000** filled with random integer values (e.g., between 0 and 100).

2. **Multithreaded Summation**
    - Implement a solution that sums all elements of the matrix using multiple threads.
    - The program should run the summation with the following thread counts: **1, 2, 4, 10, 20**.
    - Each thread should be responsible for summing a portion of the matrix (e.g., a set of rows).

3. **Performance Measurement**
    - Measure and print the time taken for each thread count to complete the summation.
    - Print the average value of all elements in the matrix after summing.

4. **Output**
    - For each thread count, print:
        - The total sum of all elements
        - The average value
        - The time taken (in milliseconds)

## Example Output

```
Threads: 1 | Sum: 500012345 | Average: 50.00 | Time: 1200 ms
Threads: 2 | Sum: 500012345 | Average: 50.00 | Time: 700 ms
Threads: 4 | Sum: 500012345 | Average: 50.00 | Time: 400 ms
Threads: 10 | Sum: 500012345 | Average: 50.00 | Time: 250 ms
Threads: 20 | Sum: 500012345 | Average: 50.00 | Time: 180 ms
```

## Implementation Guidelines

- Use `java.util.Random` to fill the matrix with random integers.
- Use `Thread` or `Runnable` to implement the summing logic.
- Divide the matrix rows as evenly as possible among the threads.
- Use `System.currentTimeMillis()` or `System.nanoTime()` to measure execution time.
- Ensure thread safety when accumulating the total sum (e.g., use `AtomicLong` or proper synchronization).
- After each run, print the required output.

## Bonus

- Plot the results (thread count vs. time) using any Java plotting library or export the results for visualization.
- Discuss why increasing the number of threads does not always result in a linear speedup.

## Submission

- Submit your Java source code and a sample output.
- Optionally, include a brief explanation of your approach and observations about the performance results.

---

**Good luck! This exercise will help you understand the impact of multithreading on computational performance.**
