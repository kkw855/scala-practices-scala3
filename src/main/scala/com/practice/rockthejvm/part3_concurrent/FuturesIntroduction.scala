package com.practice.rockthejvm.part3_concurrent

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

//noinspection ScalaWeakerAccess,TypeAnnotation
object FuturesIntroduction {

  def calculateMeaningOfLife(): Int = {
    // simulate long compute
    Thread.sleep(1000)
    42
  }

  // thread pool (Java-specific)
  val executor = Executors.newFixedThreadPool(4)
  // thread pool (Scala-specific)
  given executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executor)

  // a future = an async computation that will finish at some point
  // given executionContext will be passed here
  val aFuture: Future[Int] = Future.apply(calculateMeaningOfLife())

  // Option[Try[Int]], because
  // - we don't know if we have a value
  // - if we do, that can be a failed computation
  // inspect the value of the future RIGHT NOW
  val futureInstantResult: Option[Try[Int]] = aFuture.value

  // callbacks
  aFuture.onComplete {
    case Success(value) => println(s"I've completed with the meaning of life: $value")
    case Failure(ex)    => println(s"My async computation failed: $ex")
  } // on SOME other thread

  /*
    Functional composition
   */
  case class Profile(id: String, name: String) {
    def sendMessage(anotherProfile: Profile, message: String) =
      println(s"${this.name} sending message to ${anotherProfile.name}: $message")
  }

  object SocialNetwork {
    // "database"
    val names = Map(
      "rtjvm.id.1-daniel" -> "Daniel",
      "rtjvm.id.2-jane"   -> "Jane",
      "rtjvm.id.3-mark"   -> "Mark"
    )

    // friends "database"
    val friends = Map(
      "rtjvm.id.2-jane" -> "rtjvm.id.3-mark"
    )

    val random = new Random()

    // "API"
    def fetchProfile(id: String): Future[Profile] = Future {
      // fetch something from the database
      Thread.sleep(random.nextInt(300)) // simulate the time delay
      Profile(id, names(id))
    }

    def fetchBestFriend(profile: Profile): Future[Profile] = Future {
      Thread.sleep(random.nextInt(400))
      val bestFriendId = friends(profile.id)
      Profile(bestFriendId, names(bestFriendId))
    }
  }

  // problem: sending a message to my best friend
  def sendMessageToBestFriend(accountId: String, message: String): Unit = {
    // 1 - call fetchProfile
    // 2 - call fetchBestFriend
    // 3 - call profile.sendMessage(bestFriend)
    val profileFuture = SocialNetwork.fetchProfile(accountId)
    profileFuture.onComplete {
      case Success(profile) =>
        val friendProfileFuture = SocialNetwork.fetchBestFriend(profile)
        friendProfileFuture.onComplete {
          case Success(friendProfile) => profile.sendMessage(friendProfile, message)
          case Failure(ex)            => ex.printStackTrace()
        }
      case Failure(ex) => ex.printStackTrace()
    }
  }

  // onComplete is a hassle.
  // solution: functional composition

  def sendMessageToBestFriend_v2(accountId: String, message: String): Unit = {
    val profileFuture = SocialNetwork.fetchProfile(accountId)
    val action = profileFuture.flatMap { profile => // Future[Unit]
      SocialNetwork.fetchBestFriend(profile).map { bestFriend => // Future[Unit]
        profile.sendMessage(bestFriend, message)                 // Unit
      }
    }
  }

  def sendMessageToBestFriend_v3(accountId: String, message: String): Unit =
    for {
      profile    <- SocialNetwork.fetchProfile(accountId)
      bestFriend <- SocialNetwork.fetchBestFriend(profile)
    } yield profile.sendMessage(bestFriend, message) // identical to v2

  val janeProfileFuture = SocialNetwork.fetchProfile("rtjvm.id.2-jane")
  // map transforms value contained inside, ASYNCHRONOUSLY
  val janeFuture: Future[String] = janeProfileFuture.map(profile => profile.name)
  val janesBestFriend: Future[Profile] =
    janeProfileFuture.flatMap(profile => SocialNetwork.fetchBestFriend(profile))
  val janesBestFriendFilter: Future[Profile] =
    janesBestFriend.filter(profile => profile.name.startsWith("Z"))

  val profileNoMatterWhat: Future[Profile] = SocialNetwork.fetchProfile("unknown id").recover {
    case e: Throwable => Profile("rtjvm.id.0-dummy", "Forever alone")
  }

  // recoverWith: if both futures failed, the exception is from the second future
  val aFetchProfileNoMatterWhat: Future[Profile] =
    SocialNetwork.fetchProfile("unknown id").recoverWith { case e: Throwable =>
      SocialNetwork.fetchProfile("rtjvm.id.0-dummy")
    }

  // fallbackTo: if both futures failed, the exception is from the first future
  val fallBackProfile: Future[Profile] = SocialNetwork
    .fetchProfile("unknown id")
    .fallbackTo(SocialNetwork.fetchProfile("rtjvm.id.0-dummy"))

  def main(args: Array[String]): Unit = {
    sendMessageToBestFriend_v3("rtjvm.id.2-jane", "Hello best friend, nice to talk to you again!")
    Thread.sleep(2000)
    executor.shutdown()
  }
}
