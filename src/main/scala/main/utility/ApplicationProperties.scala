package main.utility

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Utility class to load data from application.conf file
 */

object ApplicationProperties {
  private val app = ConfigFactory.load()
  val deadlockData : Config = app.getConfig("BrachaToueg.TestData.DeadlockTestData")
  val deadlockFreeData:Config = app.getConfig("BrachaToueg.TestData.DeadlockFreeTestData")
}
