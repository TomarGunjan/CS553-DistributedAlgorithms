# CS553 Distributed Agorithms Project

### Submitted By -
1. Aakash Dasgupta
2. Dhruv Agarwal - 
3. Gunjan Tomar - 674659382

## Overview
This repository showcases the implementation of a variety distributed algorithms, such as Snapshot, Wave, and Deadlock Detection Algorithms. It executes a menu driven program for 6 distributed algorithms on graphs created by [NetGameSim](https://github.com/0x1DOCD00D/NetGameSim?tab=readme-ov-file) and some special topologies.

## About Framework
The project has been modularized for code reusability and better readability. The project structure is as following

**Project Structure**
```bash
├── README.md
├── build.sbt
├── project
│   └── build.properties
└── src
    ├── main
    │   ├── resources
    │   │   ├── NetGraph_21-04-24-18-24-58.ngs.perturbed.dot
    │   │   ├── NetGraph_30-03-24-18-54-55.ngs.dot
    │   │   ├── application.conf
    │   │   ├── graph_50_nodes.dot
    │   │   ├── inputEcho.dot
    │   │   └── inputTarry.dot
    │   └── scala
    │       └── main
    │           ├── Main.scala
    │           ├── algorithms
    │           │   ├── BrachaTouegAlgorithm.scala
    │           │   ├── ChandyLamportAlgorithm.scala
    │           │   ├── EchoAlgorithm.scala
    │           │   ├── LaiYangAlgorithm.scala
    │           │   ├── TarrysAlgorithm.scala
    │           │   └── TreeAlgorithm.scala
    │           ├── processes
    │           │   ├── BrachaTouegProcess.scala
    │           │   ├── ChandyLamportProcess.scala
    │           │   ├── EchoProcess.scala
    │           │   ├── LaiYangProcess.scala
    │           │   ├── TarryProcess.scala
    │           │   └── TreeProcess.scala
    │           └── utility
    │               ├── ApplicationProperties.scala
    │               ├── MessageTypes.scala
    │               ├── ProcessRecord.scala
    │               ├── SnapshotUtils.scala
    │               ├── Terminator.scala
    │               └── TopologyReader.scala
    └── test
        └── scala
            ├── EchoTest.scala
            ├── TarryTest.scala
            ├── TreeTest.scala
            ├── brachaTouegTest.scala
            ├── chandyLamportTest.scala
            └── laiYangTest.scala
```
### Description 

**Main.scala** - This is the entry point of the project. 

**algorithms package** - This package contains all the specific algorithm trigger files. These files prepare test data, create Actor classes and trigger the initiators for the algorithms.

**processes package** - This package contains all the Actor files required for algorithms. Algorithm package classes create instances of these Actor classes for running the algorithm.

**utility package** - This package contains utility and reusable code common for all algorithms

* ApplicationProperties.scala : This is a utility file to read properties from application.conf
* Message Types : This file has all the message types used by Actors.
* Terminator : A utility Actor class to help terminate Actor system when an algorithm has ended
* Topology Reader : A utility class to read network topology from test data files.

**Resources package** - This package containsstatic files containing Application.conf, and graph test data for creating network for running algorithms

**Application.conf** - This file has static values and data to be referenced by code

**test/src** - this package contains test unit tests for all algorithms

## How to run project
### From Intellij IDE
Pre Requisites :
1. Java 8 or higher
2. Scala Plugin should be present

Steps to Follow :

1. Clone this repo to your local machine.
2. Open the project in Intellij
3. Navigate to src/main/scala/main/Main.scala
4. Run Main.scala file

### From Terminal
Pre Requisite :
1. Should have scala installed

Steps to Follow :
1. Clone this repo to your local machine.
2. Navigate into the Project folder
3. Run following command "**sbt clean compile run**"
   

## Test Data
Example for Tree algo from NetGameSim

## Output
This a menu driven project. On running the project the user is presented with options to run any algorithm as below :
![image](https://github.com/TomarGunjan/CS553-DistributedAlgorithms/assets/26132783/eff9d140-8dc2-48d0-831d-0cf5673c7544)


User can provide the option for any algorithm to trigger it. We have used logs to describe progress of algorithm which can be followed on terminal.
![image](https://github.com/TomarGunjan/CS553-DistributedAlgorithms/assets/26132783/8d268912-a858-432d-85ad-a3dde822d2fc)

## References 
1. [NetGameSim](https://github.com/0x1DOCD00D/NetGameSim) by [Prof. Mark Grechanik](https://github.com/0x1DOCD00D)
2. [Distributed Algorithms, Second Edition - An Intuitive Approach By Wan Fokkink](https://mitpress.mit.edu/9780262037662/distributed-algorithms/)

## Algorithms
### 1. Snapshot Algorithms 
The Snapshot Algorithm, refers to the process of capturing a consistent global state of the system at a specific point in time. It allows processes to record their local states and messages exchanged, facilitating the observation of the distributed system's behavior for debugging and analysis purposes.
#### 1.1 Chandy-Lamport Algorithm
The Chandy–Lamport algorithm is a snapshot algorithm that is used in distributed systems for recording a consistent global state of an asynchronous system. 
#### 1.2 Lai-Yang Algorithm
The Lai-Yang Algorithm is a snapshot algorithm used for taking consistent global snapshots of a distributed system. This algorithm does not rely on the FIFO property of channels.


### 2. Wave Algorithm
A wave algorithm is a type of distributed algorithm used for propagating information within a distributed network of nodes.
#### 2.1 Tarry Algorithm
Coordinates process traversal in a distributed system, ensuring a predetermined order of visitation and enabling synchronization.
#### 2.2 Tree Algorithm
Structures the communication network in a hierarchical tree-like fashion, facilitating efficient message propagation and information dissemination.
#### 2.3 Echo Algorithm 
A fundamental communication protocol where a message is sent through the network and echoed back by each recipient, confirming its receipt.


### 3. Deadlock Detection
Deadlock detection is a fundamental problem in distributed computing, which requires determining a cyclic dependency within a running system.
#### 3.1 Bracha-Toueg Algorithm
The Bracha-Toueg Algorithm is employed for deadlock detection in distributed systems. It monitors resource allocation and process interactions to detect potential deadlocks and take corrective actions to resolve them. By proactively identifying and mitigating deadlocks, this algorithm enhances the reliability and availability of distributed systems.

