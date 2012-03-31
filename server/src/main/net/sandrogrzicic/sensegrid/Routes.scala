package net.sandrogrzicic.sensegrid

import cc.spray._
import can.{Stats, GetStats}
import http.MediaTypes._
import akka.actor._
import java.util.concurrent.TimeUnit
import akka.util.Duration
import utils.Logging

/**
 * Routes used by spray-server.
 */
trait Routes extends Directives with Logging {

	val routes = {
		path("") {
			get {
				respondWithMediaType(`text/html`) {
					_.complete {
						<html>
							<body>
								<h1>Say hello to
									<em>spray</em>
									!</h1>
								<p>Defined resources:</p>
								<ul>
									<li>
										<a href="/ping">/ping</a>
									</li>
									<li>
										<a href="/stream">/stream</a>
										(push-mode)</li>
									<li>
										<a href="/stats">/stats</a>
									</li>
									<li>
										<a href="/timeout">/timeout</a>
									</li>
									<li>
										<a href="/cached">/cached</a>
									</li>
									<li>
										<a href="/crash-root-service?method=post">/crash-root-service</a>
									</li>
									<li>
										<a href="/crash-spray-can-server?method=post">/crash-spray-can-server</a>
									</li>
									<li>
										<a href="/stop?method=post">/stop</a>
									</li>
								</ul>
							</body>
						</html>
					}
				}
			}
		} ~
			path("ping") {
				content(as[Option[String]]) {
					body =>
						completeWith("PONG! " + body.getOrElse(""))
				}
			}
	}

	def showServerStats(ctx: RequestContext) {
		(sprayCanServerActor ? GetStats).mapTo[Stats].onComplete {
			_.value.get match {
				case Right(stats) => ctx.complete {
					"Uptime              : " + (stats.uptime / 1000.0) + " sec\n" +
						"Requests dispatched : " + stats.requestsDispatched + '\n' +
						"Requests timed out  : " + stats.requestsTimedOut + '\n' +
						"Requests open       : " + stats.requestsOpen + '\n' +
						"Open connections    : " + stats.connectionsOpen + '\n'
				}
				case Left(ex) => ctx.complete(500, "Couldn't get server stats due to " + ex)
			}
		}
	}

	def in[U](duration: Duration)(body: => U) {
		Scheduler.scheduleOnce(() => body, duration.toMillis, TimeUnit.MILLISECONDS)
	}

	lazy val sprayCanServerActor = Actor.registry.actorsFor("spray-can-server").head

}
