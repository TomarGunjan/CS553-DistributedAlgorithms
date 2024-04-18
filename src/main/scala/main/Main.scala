package main

import akka.event.slf4j.Logger
import main.algorithms.BrachaTouegAlgorithm

object Main extends App{
    val logger = Logger("Main")
    logger.info("Running Bracha Toueg Algorithm")
    BrachaTouegAlgorithm.main()
}
