package net.sandrogrzicic.sensegrid.model

import net.sandrogrzicic.sensegrid.proto.Node

/**
 * Encapsulates the data structure.
 */
trait Model {
	type Key = String
	type Value = Node

	def apply(id: Key): Value

	def update(id: Key, data: Value)

	def remove(id: Key)

	def clear()

	def nodes: Iterable[Value]

	def size: Int

	def save()
}

object Model {
	/** Default implementation is a ConcurrentTrieModel. */
	def apply(): Model = ConcurrentTrieModel()
}
