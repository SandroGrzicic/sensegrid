package net.sandrogrzicic.sensegrid

import model.Model
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.execution.MemoryAwareThreadPoolExecutor
import proto.{Nodes, Node}
import unfiltered.netty._
import unfiltered.netty.cycle._
import unfiltered.request.{POST, Path}
import unfiltered.response.{Pass, ResponseBytes}

/**
 * Unfiltered Plan.
 */
class SenseGridPlan extends Plan with ServerErrorResponse with DeferralExecutor with DeferredIntent {

	override def underlying = SenseGridPlan.underlying

	private val data = Model()

	def intent: Plan.Intent = {
		case req @ POST(Path("/s")) =>
            if (HttpHeaders.getContentLength(req.underlying.request) > 0) {
				val m = Node.defaultInstance.mergeFrom(req.underlying.request.getContent.array())
				data(m.id) = m
				/*println(m.id + "\t" + new Date(m.timestamp).toString + "\t" + m.geoLat + " " + m.geoLong + " (" + data.size + ") " +
				  math.sqrt(m.sensors(0).data.map(a => a * a).sum))
				*/
			}

			ResponseBytes(
				Nodes(
					Vector(data.nodes.toSeq: _*),
					System.currentTimeMillis()
				) toByteArray
			)
		case req =>
			//println(new Date().toString + "\t" + req.method + " from " + req.remoteAddr)
			Pass
	}
}

object SenseGridPlan {
	/** Executor koji se koristi za pridjeljivanje dretvi zahtjevima. */
	lazy val underlying = new MemoryAwareThreadPoolExecutor(16, 65536, 1048576)

}

