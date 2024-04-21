package main

import scala.io.StdIn
import akka.event.slf4j.Logger
import main.algorithms.{BrachaTouegAlgorithm, TarrysAlgorithm, TreeAlgorithm, EchoAlgorithm}

object Main extends App {
    val logger = Logger("Main")

    def displayMenu(): Unit = {
        println("==== Distributed Algorithms Menu ====")
        println("1. Bracha Toueg Algorithm")
        println("2. Tarry's Algorithm")
        println("3. Tree Algorithm")
        println("4. Echo Algorithm")
        println("5. Chandy Lamport Algorithm")
        println("6. Lai Yang Algorithm")
        println("7. Exit")
        println("=====================================")
        print("Enter your choice (1-7): ")
    }

    def runAlgorithm(choice: Int): Unit = {
        choice match {
            case 1 =>
                logger.info("Running Bracha Toueg Algorithm")
                BrachaTouegAlgorithm.main()
            case 2 =>
                logger.info("Running Tarry's Algorithm")
                TarrysAlgorithm.main()
            case 3 =>
                logger.info("Running Tree Algorithm")
                TreeAlgorithm.main()
            case 4 =>
                logger.info("Running Echo Algorithm")
                EchoAlgorithm.main()
            case 5 =>
                logger.info("Running Chandy Lamport Algorithm")
//                ChandyLamport.main()
            case 6 =>
                logger.info("Running Lai Yang Algorithm")
//                LaiYang.main()
            case 7 =>
                logger.info("Exiting...")
            case _ =>
                logger.warn("Invalid choice. Please enter a number between 1 and 7.")
        }
    }

    var choice = 0
    do {
        displayMenu()
        try {
            choice = StdIn.readInt()
            runAlgorithm(choice)
            println()
        } catch {
            case _: NumberFormatException =>
                logger.warn("Invalid input. Please enter a valid integer.")
            case _: Exception =>
                logger.error("An error occurred. Please try again.")
        }
    } while (choice != 7)
}