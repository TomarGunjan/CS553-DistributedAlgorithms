package main.utility

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Utility class to load data from application.conf file
 */
object ApplicationProperties {
  private val app = ConfigFactory.load()
  val deadlockData : Config = app.getConfig("BrachaToueg.TestData.DeadlockTestData")
  val deadlockFreeData:Config = app.getConfig("BrachaToueg.TestData.DeadlockFreeTestData")
  val tarryInputFile: String = app.getString("TarrysAlgorithm.InputFile")
  val echoInputFile: String = app.getString("EchoAlgorithm.InputFile")
  val treeInputFile: String = app.getString("TreeAlgorithm.InputFile")
  val snapshotInputFile: String = app.getString("SnapshotAlgorithm.InputFile")
  val changRobertsData: Config = app.getConfig("ChangRoberts.TestData")

}
