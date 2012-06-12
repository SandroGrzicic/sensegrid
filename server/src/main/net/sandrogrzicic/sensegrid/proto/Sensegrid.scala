// Generated by ScalaBuff, the Scala Protocol Buffers compiler. DO NOT EDIT!
// source: sensegrid.proto

package net.sandrogrzicic.sensegrid.proto

final case class Node (
	id: String = "",
	geoLat: Int = 0,
	geoLong: Int = 0,
	geoAccuracy: Float = 0.0f,
	timestamp: Long = 0L,
	sensors: Vector[Data] = Vector.empty[Data]
) extends com.google.protobuf.GeneratedMessageLite
	with net.sandrogrzicic.scalabuff.Message[Node] {


	def setSensors(_i: Int, _v: Data) = copy(sensors = sensors.updated(_i, _v))
	def addSensors(_f: Data) = copy(sensors = sensors :+ _f)
	def addAllSensors(_f: Data*) = copy(sensors = sensors ++ _f)
	def addAllSensors(_f: TraversableOnce[Data]) = copy(sensors = sensors ++ _f)

	def clearId = copy(id = "")
	def clearGeoLat = copy(geoLat = 0)
	def clearGeoLong = copy(geoLong = 0)
	def clearGeoAccuracy = copy(geoAccuracy = 0.0f)
	def clearTimestamp = copy(timestamp = 0L)
	def clearSensors = copy(sensors = Vector.empty[Data])

	def writeTo(output: com.google.protobuf.CodedOutputStream) {
		output.writeString(1, id)
		output.writeInt32(2, geoLat)
		output.writeInt32(3, geoLong)
		output.writeFloat(4, geoAccuracy)
		output.writeInt64(5, timestamp)
		for (_v <- sensors) output.writeMessage(10, _v)
	}

	lazy val getSerializedSize = {
		import com.google.protobuf.CodedOutputStream._
		var size = 0
		size += computeStringSize(1, id)
		size += computeInt32Size(2, geoLat)
		size += computeInt32Size(3, geoLong)
		size += computeFloatSize(4, geoAccuracy)
		size += computeInt64Size(5, timestamp)
		for (_v <- sensors) size += computeMessageSize(10, _v)

		size
	}

	def mergeFrom(in: com.google.protobuf.CodedInputStream, extensionRegistry: com.google.protobuf.ExtensionRegistryLite): Node = {
		import com.google.protobuf.ExtensionRegistryLite.{getEmptyRegistry => _emptyRegistry}
		var _id: String = ""
		var _geoLat: Int = 0
		var _geoLong: Int = 0
		var _geoAccuracy: Float = 0.0f
		var _timestamp: Long = 0L
		val _sensors: collection.mutable.Buffer[Data] = sensors.toBuffer

		def _newMerged = Node(
			_id,
			_geoLat,
			_geoLong,
			_geoAccuracy,
			_timestamp,
			Vector(_sensors: _*)
		)
		while (true) in.readTag match {
			case 0 => return _newMerged
			case 10 => _id = in.readString()
			case 16 => _geoLat = in.readInt32()
			case 24 => _geoLong = in.readInt32()
			case 37 => _geoAccuracy = in.readFloat()
			case 40 => _timestamp = in.readInt64()
			case 82 => _sensors += readMessage[Data](in, Data.defaultInstance, _emptyRegistry)
			case default => if (!in.skipField(default)) return _newMerged
		}
		null // compiler needs a return value
	}

	def mergeFrom(m: Node) = {
		Node(
			m.id,
			m.geoLat,
			m.geoLong,
			m.geoAccuracy,
			m.timestamp,
			sensors ++ m.sensors
		)
	}

	def getDefaultInstanceForType = Node.defaultInstance
	def clear = getDefaultInstanceForType
	def isInitialized = true
	def build = this
	def buildPartial = this
	def newBuilderForType = this
	def toBuilder = this
}

object Node {
	@reflect.BeanProperty val defaultInstance = new Node()

	val ID_FIELD_NUMBER = 1
	val GEOLAT_FIELD_NUMBER = 2
	val GEOLONG_FIELD_NUMBER = 3
	val GEOACCURACY_FIELD_NUMBER = 4
	val TIMESTAMP_FIELD_NUMBER = 5
	val SENSORS_FIELD_NUMBER = 10

}
final case class Data (
	sensorType: Data.Type.EnumVal = Data.Type._UNINITIALIZED,
	accuracy: Int = 0,
	data: Vector[Float] = Vector.empty[Float]
) extends com.google.protobuf.GeneratedMessageLite
	with net.sandrogrzicic.scalabuff.Message[Data] {


	def setData(_i: Int, _v: Float) = copy(data = data.updated(_i, _v))
	def addData(_f: Float) = copy(data = data :+ _f)
	def addAllData(_f: Float*) = copy(data = data ++ _f)
	def addAllData(_f: TraversableOnce[Float]) = copy(data = data ++ _f)

	def clearSensorType = copy(sensorType = Data.Type._UNINITIALIZED)
	def clearAccuracy = copy(accuracy = 0)
	def clearData = copy(data = Vector.empty[Float])

	def writeTo(output: com.google.protobuf.CodedOutputStream) {
		output.writeEnum(1, sensorType)
		output.writeInt32(2, accuracy)
		for (_v <- data) output.writeFloat(3, _v)
	}

	lazy val getSerializedSize = {
		import com.google.protobuf.CodedOutputStream._
		var size = 0
		size += computeEnumSize(1, sensorType)
		size += computeInt32Size(2, accuracy)
		for (_v <- data) size += computeFloatSize(3, _v)

		size
	}

	def mergeFrom(in: com.google.protobuf.CodedInputStream, extensionRegistry: com.google.protobuf.ExtensionRegistryLite): Data = {
		import com.google.protobuf.ExtensionRegistryLite.{getEmptyRegistry => _emptyRegistry}
		var _sensorType: Data.Type.EnumVal = Data.Type._UNINITIALIZED
		var _accuracy: Int = 0
		val _data: collection.mutable.Buffer[Float] = data.toBuffer

		def _newMerged = Data(
			_sensorType,
			_accuracy,
			Vector(_data: _*)
		)
		while (true) in.readTag match {
			case 0 => return _newMerged
			case 8 => _sensorType = Data.Type.valueOf(in.readEnum())
			case 16 => _accuracy = in.readInt32()
			case 29 => _data += in.readFloat()
			case default => if (!in.skipField(default)) return _newMerged
		}
		null // compiler needs a return value
	}

	def mergeFrom(m: Data) = {
		Data(
			m.sensorType,
			m.accuracy,
			data ++ m.data
		)
	}

	def getDefaultInstanceForType = Data.defaultInstance
	def clear = getDefaultInstanceForType
	def isInitialized = true
	def build = this
	def buildPartial = this
	def newBuilderForType = this
	def toBuilder = this
}

object Data {
	@reflect.BeanProperty val defaultInstance = new Data()

	val SENSORTYPE_FIELD_NUMBER = 1
	val ACCURACY_FIELD_NUMBER = 2
	val DATA_FIELD_NUMBER = 3

	object Type extends net.sandrogrzicic.scalabuff.Enum {
		sealed trait EnumVal extends Value
		val _UNINITIALIZED = new EnumVal { val name = "UNINITIALIZED ENUM VALUE"; val id = -1 }

		val ACCELEROMETER = new EnumVal { val name = "ACCELEROMETER"; val id = 1 }
		val MAGNETIC_FIELD = new EnumVal { val name = "MAGNETIC_FIELD"; val id = 2 }
		val ORIENTATION = new EnumVal { val name = "ORIENTATION"; val id = 3 }
		val GYROSCOPE = new EnumVal { val name = "GYROSCOPE"; val id = 4 }
		val LIGHT = new EnumVal { val name = "LIGHT"; val id = 5 }
		val PRESSURE = new EnumVal { val name = "PRESSURE"; val id = 6 }
		val TEMPERATURE = new EnumVal { val name = "TEMPERATURE"; val id = 7 }
		val PROXIMITY = new EnumVal { val name = "PROXIMITY"; val id = 8 }
		val GRAVITY = new EnumVal { val name = "GRAVITY"; val id = 9 }
		val LINEAR_ACCELERATION = new EnumVal { val name = "LINEAR_ACCELERATION"; val id = 10 }
		val ROTATION_VECTOR = new EnumVal { val name = "ROTATION_VECTOR"; val id = 11 }
		val RELATIVE_HUMIDITY = new EnumVal { val name = "RELATIVE_HUMIDITY"; val id = 12 }
		val AMBIENT_TEMPERATURE = new EnumVal { val name = "AMBIENT_TEMPERATURE"; val id = 13 }

		val ACCELEROMETER_VALUE = 1
		val MAGNETIC_FIELD_VALUE = 2
		val ORIENTATION_VALUE = 3
		val GYROSCOPE_VALUE = 4
		val LIGHT_VALUE = 5
		val PRESSURE_VALUE = 6
		val TEMPERATURE_VALUE = 7
		val PROXIMITY_VALUE = 8
		val GRAVITY_VALUE = 9
		val LINEAR_ACCELERATION_VALUE = 10
		val ROTATION_VECTOR_VALUE = 11
		val RELATIVE_HUMIDITY_VALUE = 12
		val AMBIENT_TEMPERATURE_VALUE = 13

		def valueOf(id: Int) = id match {
			case 1 => ACCELEROMETER
			case 2 => MAGNETIC_FIELD
			case 3 => ORIENTATION
			case 4 => GYROSCOPE
			case 5 => LIGHT
			case 6 => PRESSURE
			case 7 => TEMPERATURE
			case 8 => PROXIMITY
			case 9 => GRAVITY
			case 10 => LINEAR_ACCELERATION
			case 11 => ROTATION_VECTOR
			case 12 => RELATIVE_HUMIDITY
			case 13 => AMBIENT_TEMPERATURE
			case _default => throw new net.sandrogrzicic.scalabuff.UnknownEnumException(_default)
		}
		val internalGetValueMap = new com.google.protobuf.Internal.EnumLiteMap[EnumVal] {
			def findValueByNumber(id: Int): EnumVal = valueOf(id)
		}
	}

}
final case class Nodes (
	nodes: Vector[Node] = Vector.empty[Node],
	timestamp: Long = 0L
) extends com.google.protobuf.GeneratedMessageLite
	with net.sandrogrzicic.scalabuff.Message[Nodes] {


	def setNodes(_i: Int, _v: Node) = copy(nodes = nodes.updated(_i, _v))
	def addNodes(_f: Node) = copy(nodes = nodes :+ _f)
	def addAllNodes(_f: Node*) = copy(nodes = nodes ++ _f)
	def addAllNodes(_f: TraversableOnce[Node]) = copy(nodes = nodes ++ _f)

	def clearNodes = copy(nodes = Vector.empty[Node])
	def clearTimestamp = copy(timestamp = 0L)

	def writeTo(output: com.google.protobuf.CodedOutputStream) {
		for (_v <- nodes) output.writeMessage(1, _v)
		output.writeInt64(2, timestamp)
	}

	lazy val getSerializedSize = {
		import com.google.protobuf.CodedOutputStream._
		var size = 0
		for (_v <- nodes) size += computeMessageSize(1, _v)
		size += computeInt64Size(2, timestamp)

		size
	}

	def mergeFrom(in: com.google.protobuf.CodedInputStream, extensionRegistry: com.google.protobuf.ExtensionRegistryLite): Nodes = {
		import com.google.protobuf.ExtensionRegistryLite.{getEmptyRegistry => _emptyRegistry}
		val _nodes: collection.mutable.Buffer[Node] = nodes.toBuffer
		var _timestamp: Long = 0L

		def _newMerged = Nodes(
			Vector(_nodes: _*),
			_timestamp
		)
		while (true) in.readTag match {
			case 0 => return _newMerged
			case 10 => _nodes += readMessage[Node](in, Node.defaultInstance, _emptyRegistry)
			case 16 => _timestamp = in.readInt64()
			case default => if (!in.skipField(default)) return _newMerged
		}
		null // compiler needs a return value
	}

	def mergeFrom(m: Nodes) = {
		Nodes(
			nodes ++ m.nodes,
			m.timestamp
		)
	}

	def getDefaultInstanceForType = Nodes.defaultInstance
	def clear = getDefaultInstanceForType
	def isInitialized = true
	def build = this
	def buildPartial = this
	def newBuilderForType = this
	def toBuilder = this
}

object Nodes {
	@reflect.BeanProperty val defaultInstance = new Nodes()

	val NODES_FIELD_NUMBER = 1
	val TIMESTAMP_FIELD_NUMBER = 2

}

object Sensegrid {
	def registerAllExtensions(registry: com.google.protobuf.ExtensionRegistryLite) {
	}

}
