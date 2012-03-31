package net.sandrogrzicic.sensegrid

import akka.config.Supervision._
import cc.spray._
import cc.spray.can._
import akka.actor.Supervisor
import akka.actor.Actor._
import org.slf4j.LoggerFactory

/**
 * Spray server entry point.
 * Starts the spray-can server with the spray-server framework.
 *
 * @author Sandro Gržičić
 */
object Main extends App {

	LoggerFactory.getLogger(getClass)

	val mainModule = new Routes {

	}

	val httpService = actorOf(new HttpService(mainModule.routes))
	val rootService = actorOf(new SprayCanRootService(httpService))
	val sprayCanServer = actorOf(new HttpServer()) // configuration is in resources/akka.conf

	Supervisor(
		SupervisorConfig(
			OneForOneStrategy(List(classOf[Exception]), 3, 100),
			List(
				Supervise(httpService, Permanent),
				Supervise(rootService, Permanent),
				Supervise(sprayCanServer, Permanent)
			)
		)
	)
}
