package net.sandrogrzicic.sensegrid


/**
 * Server entry point.
 *
 * @author Sandro Gržičić
 */
object Server extends App {

	val HTTP_PORT = 7766

	unfiltered.netty.Http(HTTP_PORT).plan(new SenseGridPlan()).run()

}
