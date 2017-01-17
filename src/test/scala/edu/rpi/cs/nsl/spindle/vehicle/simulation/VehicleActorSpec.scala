package edu.rpi.cs.nsl.spindle.vehicle.simulation

import scala.concurrent.duration.DurationInt
import scala.util.Random

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.slf4j.LoggerFactory
import edu.rpi.cs.nsl.spindle.vehicle.Types._

import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import edu.rpi.cs.nsl.spindle.vehicle.simulation.event_store.CacheFactory
import edu.rpi.cs.nsl.spindle.vehicle.simulation.event_store.postgres.PgClient
import edu.rpi.cs.nsl.spindle.vehicle.kafka.ClientFactoryDockerFixtures
import edu.rpi.cs.nsl.spindle.vehicle.kafka.DockerHelper

trait VehicleExecutorFixtures {
  private type TimeSeq = List[Timestamp]
  private val random = new Random()

  val FIVE_SECONDS_MS: Double = 5 * 1000 toDouble
  val startTime: Double = System.currentTimeMillis() + FIVE_SECONDS_MS
  val randomTimings: TimeSeq = (0 to 500).map(_.toDouble * random.nextGaussian()).toList
  val clientFactory = ClientFactoryDockerFixtures.getFactory
  val cacheFactory = new CacheFactory(new PgClient()) {
    override def mkCaches(nodeId: NodeId) = {
      val (_, caches) = super.mkCaches(nodeId)
      (randomTimings, caches)
    }
  }
  def mkVehicle = new Vehicle(0, clientFactory, cacheFactory, Set(), Set(), false)
  def mkVehicleProps = Vehicle.props(0, clientFactory, cacheFactory, Set(), Set(), false)

  implicit class ComparableDouble(double: Double) {
    val THRESHOLD = 1E-5
    def approxEquals(other: Double): Boolean = {
      Math.abs(other) - Math.abs(double) < THRESHOLD
    }
  }
}

class VehicleActorSpecDocker extends TestKit(ActorSystem("VehicleActorSpec"))
    with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  private val logger = LoggerFactory.getLogger(this.getClass)
  override def beforeAll {
    super.beforeAll
    ClientFactoryDockerFixtures.waitReady
  }
  override def afterAll {
    shutdown()
    DockerHelper.stopCluster
  }
  "A Vehicle actor" should {
    "correctly generate timings" in new VehicleExecutorFixtures {
      val vExec = TestActorRef(mkVehicle).underlyingActor
      val timings = vExec.mkTimings(startTime)
      assert(timings.min == startTime)
      assert(timings.max approxEquals (randomTimings.max + startTime),
        s"${timings.max} != ${randomTimings.max + startTime}")
    }
    "spawn multiple copies" in new VehicleExecutorFixtures {
      val NUM_COPIES = 5000
      (0 to NUM_COPIES)
        .map { nodeId =>
          system.actorOf(mkVehicleProps)
        }
        .foreach { actor =>
          logger.info(s"Sending test message to $actor")
          within(5 minutes) {
            actor ! Ping()
            expectMsg(Ping())
          }
        }
    }
  }

  //TODO: create, run vehicle, ensure it generates expected outputs
  //TODO: test having lots of vehicles running at once
}