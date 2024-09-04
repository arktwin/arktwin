// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors

import arktwin.common.MailboxConfig
import arktwin.edge.config.{CoordinateConfig, CullingConfig, EdgeConfig}
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object EdgeConfigurator:
  type Message = Get | CoordinateConfig | CullingConfig | Receptionist.Listing
  case class Get(replyTo: ActorRef[EdgeConfig])

  val coordinateObserverKey: ServiceKey[CoordinateConfig] = ServiceKey(
    getClass.getName + "/" + CoordinateConfig.getClass.getName
  )
  val cullingObserverKey: ServiceKey[CullingConfig] = ServiceKey(
    getClass.getName + "/" + CullingConfig.getClass.getName
  )

  def spawn(initEdgeConfig: EdgeConfig): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initEdgeConfig),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(initEdgeConfig: EdgeConfig): Behavior[Message] = Behaviors.setup: context =>
    context.system.receptionist ! Receptionist.subscribe(coordinateObserverKey, context.self)
    context.system.receptionist ! Receptionist.subscribe(cullingObserverKey, context.self)

    var edgeConfig = initEdgeConfig
    var coordinateObservers = Set[ActorRef[CoordinateConfig]]()
    var cullingObservers = Set[ActorRef[CullingConfig]]()

    Behaviors.receiveMessage:
      case Get(replyTo) =>
        replyTo ! edgeConfig
        Behaviors.same

      case coordinateConfig: CoordinateConfig =>
        edgeConfig =
          edgeConfig.copy(dynamic = edgeConfig.dynamic.copy(coordinate = coordinateConfig))
        coordinateObservers.foreach(_ ! coordinateConfig)
        Behaviors.same

      case cullingConfig: CullingConfig =>
        edgeConfig = edgeConfig.copy(dynamic = edgeConfig.dynamic.copy(culling = cullingConfig))
        cullingObservers.foreach(_ ! cullingConfig)
        Behaviors.same

      case coordinateObserverKey.Listing(newObservers) =>
        (newObservers &~ coordinateObservers).foreach(_ ! edgeConfig.dynamic.coordinate)
        coordinateObservers = newObservers
        Behaviors.same

      case cullingObserverKey.Listing(newObservers) =>
        (newObservers &~ cullingObservers).foreach(_ ! edgeConfig.dynamic.culling)
        cullingObservers = newObservers
        Behaviors.same

      case _: Receptionist.Listing =>
        Behaviors.unhandled
