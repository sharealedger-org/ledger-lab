package org.universalledger.foundation.va.ledger

/*
 *(c) Copyright IBM Corporation. 2018
 * SPDX-License-Identifier: Apache-2.0
 * By Kip Twitchell
 *  Created May 2019
 */
//______________________________________________________________________________________________
// Resets the univledger db
//______________________________________________________________________________________________


import java.sql.{Connection, DriverManager}



object dbReset {
  def apply(): Unit = {

    // Database settings are supplied by the runtime environment.
    val driver = "org.postgresql.Driver"
    val url = sys.env("UL_DB_URL")
    val username = sys.env("UL_DB_USERNAME")
    val password = sys.env("UL_DB_PASSWORD")

    // there's probably a better way to do this
    var connection:Connection = null

    println("connecting to database")

    try {
      // make the connection
//      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password )

      val statement0 = connection.createStatement()
      val resultSet0 = statement0.execute("CREATE TABLE test.test (coltest varchar(20))")
      println("Create Table Result: " + resultSet0)

      val statement1 = connection.createStatement()
      val resultSet1 = statement1.execute("insert into test.test (coltest) values ('It works!')")

      // create the statement, and run the select query
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("SELECT * from test.test")
      while ( resultSet.next() ) {
//        val host = resultSet.getString("host")
        val result = resultSet.getString("coltest")
        println("test value =  " + result)
      }
      val statement3 = connection.createStatement()
      val resultSet3 = statement3.execute("DROP TABLE test.test")

    } catch {
      case e : Throwable => e.printStackTrace
    }
    connection.close()

    System.exit(0)

  }

}
