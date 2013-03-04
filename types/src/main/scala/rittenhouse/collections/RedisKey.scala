package rittenhouse.collections

import rittenhouse.exceptions._

import com.redis._
import serialization._

abstract class RedisKey[A](val key: String)(implicit val parse: Parse[A], implicit val client: RedisClient)   {

  /* Check that if key exists, type matches declared type */
  client.getType(key) match {
    case Some("string") => if(this.getClass.getSimpleName != "RedisKey") throw new RedisTypeDoesNotMatchException(key, "string", this.getClass.getSimpleName, client.toString)
    case Some("list")   => if(this.getClass.getSimpleName != "RedisList") throw new RedisTypeDoesNotMatchException(key, "list", this.getClass.getSimpleName, client.toString)
    case Some("set")    => if(this.getClass.getSimpleName != "RedisSet") throw new RedisTypeDoesNotMatchException(key, "set", this.getClass.getSimpleName, client.toString)
    case Some("zset")   => if(this.getClass.getSimpleName != "RedisSortedSet") throw new RedisTypeDoesNotMatchException(key, "zset", this.getClass.getSimpleName, client.toString)
    case Some("hash")   => if(this.getClass.getSimpleName != "RedisHash") throw new RedisTypeDoesNotMatchException(key, "hash", this.getClass.getSimpleName, client.toString)
    case _              => { } // do nothing
  }

  /* Check if key has an expiry set */
  protected[this] lazy val expiry: Option[Expiry] = client.ttl(key) match {
    case Some(timeout) if (timeout > 0) => Some(Expiry(timeout))
    case _ => None
  }

  def del: Long = {
    client.del(key) match {
      case Some(numDeletedKeys) if (numDeletedKeys > 0) => numDeletedKeys
      case _ => throw new RedisKeyDoesNotExistException(key, client.toString)
    }
  }

  def dump = throw new UnsupportedOperationException

  def exists: Boolean = client.exists(key)

}
