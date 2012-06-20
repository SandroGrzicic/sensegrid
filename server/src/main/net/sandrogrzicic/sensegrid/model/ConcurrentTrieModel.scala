package net.sandrogrzicic.sensegrid.model

import ctries2.ConcurrentTrie
import net.sandrogrzicic.sensegrid.proto.Node
import java.io._
import scala.Some

/**
 * Implements the Model using a concurrent Hash Array Mapped Trie.
 */
class ConcurrentTrieModel extends Model {
	protected val map = new ConcurrentTrie[Key, Value]()

	load()

	def apply(id: Key) = map(id)

	def update(id: Key, data: Value) {
		map(id) = data
	}

	def remove(id: Key) {
		map.remove(id)
	}

	def clear() {
		map.clear()
	}

	def nodes = map.values

	def size = map.size

	def save() {
		val os = new BufferedOutputStream(new FileOutputStream(ConcurrentTrieModel.FILE))
		try {
			map.values.foreach { n =>
				n.writeDelimitedTo(os)
			}
		} finally {
			os.close()
		}
		println("Saved " + map.size + " nodes.")
	}



	def load() {
		if (!ConcurrentTrieModel.FILE.exists()) {
			return
		}

		val is = new BufferedInputStream(new FileInputStream(ConcurrentTrieModel.FILE))
		val default = Node.defaultInstance
		try {
			var moreMessages = true
			while (moreMessages) {
				default.mergeDelimitedFromStream(is) match {
					case Some(node) => map(node.id) = node
					case None       => moreMessages = false
				}
			}
		} finally {
			is.close()
		}
		println("Loaded " + map.size + " nodes.")
	}
}

object ConcurrentTrieModel {
	val FILE = new File("model.sg")

	def apply() = new ConcurrentTrieModel()
}
