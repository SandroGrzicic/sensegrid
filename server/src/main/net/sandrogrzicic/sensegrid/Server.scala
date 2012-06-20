package net.sandrogrzicic.sensegrid


/**
 * Server entry point.
 *
 * @author Sandro Gržičić
 */
object Server extends App {

	val HTTP_PORT = 7766

	val plan = new SenseGridPlan()

	val beforeStop = () => plan.beforeStop()

	unfiltered.netty.Http(HTTP_PORT, "0.0.0.0", Nil, beforeStop).plan(plan).run()

}
