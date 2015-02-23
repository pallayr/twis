package core

import org.specs2.mutable.Specification

class TwitterClientIntegrationTest extends Specification {

  val twitterClient = new TwitterClient
  val searchOperator = "#Scala"

  "The Twitter Client" should {

    "find index with hashtag #Scala" in {
      val result = twitterClient.search(searchOperator)
      result.getTweets must not be empty
      result.getCount must be equalTo 100
    }

    "find index since a previous search" in {
      val firstResults = twitterClient.search(searchOperator)
      val maxId = firstResults.getMaxId.asInstanceOf[Int]
      val secondResults = twitterClient.search(searchOperator, maxId)
      secondResults must not be empty
    }

  }

}
