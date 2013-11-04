package com.udacity.h2fuzzer

object Query {
  val CREATE_TEST_DB = """
    CREATE DATABASE fuzzer
  """

  val CREATE_USER_TABLE = """
    CREATE TABLE user (
      `id` INT IDENTITY,
      `x` INT,
      `y` INT,
      `z` INT,
      `clicks` INT,
      `info` VARCHAR(255)
    )
  """

  val INSERT_USER = """
    INSERT INTO user (`x`, `y`, `z`, `clicks`, `info`)
    VALUES (?, ?, ?, ?, ?)
  """

  val SELECT_ALL_USERS = """
    SELECT * FROM user
  """
}
