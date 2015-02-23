package controllers

import play.api.mvc.Controller
import core.{ScalaBlogMan, TweetOperations, WordpressClient}

import play.api.data.Forms._
import play.api.data.Form
import play.libs.Akka
import akka.pattern.ask
import akka.util.Timeout
import actors.messsages.FindTweetsInRangeWithRank
import org.joda.time.Interval
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import domain.Tweet
import securesocial.core.SecureSocial
import akka.actor.ActorRef


class Wordpress(tweetReader: ActorRef) extends Controller with TweetOperations with SecureSocial {

  private val wordpressClient = new WordpressClient
  private val jodaDateMapping = jodaDate("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  implicit val timeout: Timeout = 5 second

  val createForm = Form(
    tuple(
      "title" -> text, "from" -> jodaDateMapping, "to" -> jodaDateMapping, "minRank" -> bigDecimal(3, 2)
    )
  )

  def create = SecuredAction(ajaxCall = true, authorize = ScalaBlogMan) { implicit request =>
    createForm.bindFromRequest.fold (
      formWithErrors => UnprocessableEntity,
      values => Async {
        (tweetReader ? FindTweetsInRangeWithRank(values._2, values._3, values._4.doubleValue)).mapTo[List[Tweet]].map ( tweets => {
          val interval = new Interval(values._2, values._3)
          val tweetsWithLinks = tweets.map { t => (t, readLinks(t.id)) }
          val postContent = views.html.wordpress.tweets(tweetsWithLinks, interval).body
          wordpressClient.createPost(values._1, postContent)
          Ok
        })
      }
    )
  }

}
