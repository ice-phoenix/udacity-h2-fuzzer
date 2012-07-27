package fuzzer

import java.io._
import java.sql.{PreparedStatement, DriverManager, Statement, Connection}

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.h2.jdbc.JdbcSQLException

import util.Random

object Main {
  val rand = new Random()

  val dbPath = "/tmp/test"
  val dbUri = "jdbc:h2:" + dbPath
  val dbFile = dbPath + ".h2.db"

  val dbs = List(dbFile)

  def backup(s: String) = s + ".backup"

  def main(args: Array[String]) = {
    println("Starting fuzzing...")

    deleteDatabase(dbs)
    createDatabase(dbUri)
    saveDatabase(dbs)

    Range(0,10000).foreach((i) => {
      restoreDatabase(dbs)
      mutate(dbFile)
      executeQuery(dbUri, Query.SELECT_ALL)
    })

    println("Done!")
  }

  def mutate(fileName: String) = {
    // println("Mutating db...")

    val file = new File(fileName)

    val headerSize = 16 * 1024

    val bytes = Files.toByteArray(file)
    val ins = rand.nextInt(bytes.length / 100)
    val del = rand.nextInt(bytes.length / 100)
    val mut = rand.nextInt(bytes.length)
    Range(0, mut).foreach((i) => {
      val ix = headerSize + rand.nextInt(bytes.length - headerSize)
      bytes(ix) = rand.nextInt().toByte
    })
    Files.write(bytes, file)
  }

  def executeQuery(path: String, query: String) = {
    var conn: Connection = null
    var stmt: Statement = null
    try {
      conn = DriverManager.getConnection(path, "sa", "")
      stmt = conn.createStatement()
      stmt.executeQuery(query)
    }
    catch {
      case ex: JdbcSQLException => println("Detected corruption: " + ex.getMessage())
      case ex: Exception => println("!!! Unexpected exception: " + ex.getMessage())
    }
    finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }

  def createDatabase(path: String) = {
    println("Creating reference db...")

    var conn: Connection = null
    var stmt: Statement = null
    var insertStmt: PreparedStatement = null
    try {
      conn = DriverManager.getConnection(path, "sa", "")
      stmt = conn.createStatement()
      stmt.executeUpdate(Query.CREATE_USER_TABLE)

      insertStmt = conn.prepareStatement(Query.INSERT_USER)
      Range(0, 10000).foreach((i) => {
        insertStmt.setInt(1, rand.nextInt())
        insertStmt.setInt(2, rand.nextInt())
        insertStmt.setInt(3, rand.nextInt())
        insertStmt.setInt(4, rand.nextInt())
        insertStmt.setString(5, rand.nextString(rand.nextInt(256)))
        insertStmt.execute()
      })
    }
    finally {
      if (insertStmt != null) insertStmt.close()
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }

  def saveDatabase(fileNames: List[String]) = {
    println("Saving reference database...")
    copyFiles(fileNames, identity[String], backup)
  }

  def restoreDatabase(fileNames: List[String]) = {
    // println("Restoring reference database...")
    copyFiles(fileNames, backup, identity[String])
  }

  def deleteDatabase(fileNames: List[String]) = {
    println("Deleting old database...")

    fileNames.foreach((name) => {
      new File(name).delete()
    })
  }

  def copyFiles(fileNames: List[String], from: String => String, to: String => String) = {
    fileNames.foreach((name) => {
      val ffrom = new File(from(name))
      val fto = new File(to(name))
      FileUtils.copyFile(ffrom, fto)
    })
  }
}
