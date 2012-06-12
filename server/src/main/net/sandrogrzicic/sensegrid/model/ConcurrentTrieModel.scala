package net.sandrogrzicic.sensegrid.model

import ctries2.ConcurrentTrie
import net.sandrogrzicic.sensegrid.proto.Node

/**
 * Implements the Model using a concurrent Hash Array Mapped Trie.
 */
class ConcurrentTrieModel extends Model {
	protected val map = new ConcurrentTrie[Key, Value]()

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
}

object ConcurrentTrieModel {
	def apply() = new ConcurrentTrieModel()
}
