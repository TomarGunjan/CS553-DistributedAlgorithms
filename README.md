## CS553 Distributed Agorithms Project
### Submitted By -
1. Aakash Dasgupta
2. Dhruv Agarwal
3. Gunjan Tomar - 674659382
## Overview
This repository showcases the implementation of various distributed algorithms, including Snapshot, Wave, and Deadlock Detection.
### Snapshot Algorithms 
The Snapshot Algorithm, refers to the process of capturing a consistent global state of the system at a specific point in time. It allows processes to record their local states and messages exchanged, facilitating the observation of the distributed system's behavior for debugging and analysis purposes.
#### 1. Lai-Yang Algorithm
The Lai-Yang Algorithm is a distributed algorithm used for taking consistent global snapshots of a distributed system. It ensures that each process captures a consistent snapshot of the system's state, which is crucial for various applications like checkpointing and debugging.

#### 2. Chandy-Lamport Algorithm
The Chandy-Lamport Algorithm is another method for capturing consistent global snapshots in a distributed system. It allows processes to record their local states and the messages they send and receive, facilitating the construction of a global snapshot.

### Wave Algorithm
The Wave Algorithm is a communication protocol used for message dissemination and synchronization in distributed systems. It facilitates the propagation of messages through the network in a coordinated manner, ensuring reliable communication and synchronization among processes.
#### 1. Echo Algorithm: A fundamental communication protocol where a message is sent through the network and echoed back by each recipient, confirming its receipt.
#### 2. Tree Algorithm: Structures the communication network in a hierarchical tree-like fashion, facilitating efficient message propagation and information dissemination.
#### 3. Tarry Algorithm: Coordinates process traversal in a distributed system, ensuring a predetermined order of visitation and enabling synchronization.

## Requirements fulfilled for the course project
1. Generated a dot file from NetGameSim containing information about node connections.
2. Used aforementioned graph to spawn a graph of Akka Actors.
3. 
## Clone Instructions
## Workflow
Diagram and Explanation
## Components of the Project
The Project has been divided into interconnected modules.
1. The algorithms package contains the overarching logic of the corresponding algorithm.
2. The processes package contains the processes pertaining to each algorithm.
3. The utility folder contains application wide and program specific dependencies.
## Features
## Discussion
