package rittenhouse.exceptions

case class RedisKeyDoesNotExistException(keyName: String, dbInfo: String) extends Exception   {

  override def getMessage = s"Redis key '$keyName' does not exist in database at $dbInfo"

}

case class RedisTypeDoesNotMatchException(keyName: String, expectedType: String, actualType: String, dbInfo: String) extends Exception   {

  override def getMessage = s"Type of Redis key '$keyName' is $actualType, does not match expectation of $expectedType in database at $dbInfo"

}

case class RedisClientsNonMatchingException extends Exception {

  override def getMessage = "All clients in a BLPOP operation must be the same"

}
