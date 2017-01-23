package edu.rpi.cs.nsl.spindle.vehicle.kafka.utils

trait TopicLookupService {
  type NodeId = Int
  private def getNodePrefix(node: NodeId): String = s"vehicle-stream-$node"
  private def mkTopic(node: NodeId, suffix: String): String = s"${getNodePrefix(node)}-$suffix"
  /**
   * Get output topic for a given vehicle (from which messages would be batched and transmitted)
   */
  private def getOutTopic(node: NodeId): String = s"${getNodePrefix(node)}-output"
  /**
   * Get output topic for a given mapper on a given vehicle
   */
  def getMapperOutput(node: NodeId, mapperId: String): String = mkTopic(node, s"mapper-$mapperId")
  def getReducerOutput(node: NodeId, reducerId: String): String = mkTopic(node, s"reducer-$reducerId")
  //def getSensorOutput(node: NodeId, sensorId: String) = mkTopic(node, s"sensor-$sensorId")
  def getVehicleStatus(node: NodeId): String = mkTopic(node, "vehicle-status")

  /**
   * Get topic for cluster head "received messages"
   */
  def getClusterInput(node: NodeId): String = mkTopic(node, s"-ch-input")
  def getClusterOutput(node: NodeId): String = mkTopic(node, s"-ch-output")
}

object TopicLookupService extends TopicLookupService {}