package com.kn0de.test

import org.scalatest._
import com.redis._
import com.redis.serialization._

import com.kn0de._
 
class ListSpec extends FunSpec 
                  with OneInstancePerTest 
                  with BeforeAndAfterEach
                  with BeforeAndAfterAll
                  with GivenWhenThen {

  implicit val redisClient = new RedisClient
  val rList = new RedisList[String]("rittenhouse-redis-test-list")

  override def beforeEach = { }

  override def afterEach = {
    redisClient.flushdb
    redisClient.del("rittenhouse-redis-test-list")
  }

  override def afterAll = { }

  describe("A RedisList") {
    describe("when empty") {
      it("should have a length of 0") {
        assert(rList.length === 0)
      }

      it("should produce IndexOutOfBoundsException on any apply") {
        intercept[IndexOutOfBoundsException] {
          rList(0)
        }
      }

      it("should produce an empty sequence") {
        assert(rList.seq.isEmpty)
      }
    }

    it("should allow an element to be added to an empty list") {
      Given("an empty RedisList")
      assert(rList.isEmpty === true)

      When("an element is added")
      rList += "test"

      Then("the list should have length 1")
      assert(rList.length === 1)

      And("the element at index 0 should equal the new element")
      assert(rList(0) === "test")
    }

    it("should allow multiple elements to be appended to an empty list") {
      Given("an empty RedisList")
      assert(rList.isEmpty === true)

      When("some elements are added")
      rList.append("a", "b", "c")

      Then("the list should have length 3")
      assert(rList.length === 3)

      And("the seq of the list should equal Seq('a', 'b', 'c')")
      assert(rList.seq === Seq("a", "b", "c"))
    }



  }

}

