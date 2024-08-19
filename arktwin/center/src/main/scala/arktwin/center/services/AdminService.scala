// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.actors.{Clock, Register}
import com.google.protobuf.empty.Empty
import org.apache.pekko.actor.typed.ActorRef

import scala.concurrent.Future

class AdminService(clock: ActorRef[Clock.Message], register: ActorRef[Register.Message])
    extends Admin:
  override def updateClockSpeed(in: UpdateClockSpeedRequest): Future[Empty] =
    clock ! Clock.UpdateSpeed(in.clockSpeed)
    Future.successful(Empty())

  override def deleteAgents(in: AgentSelectorMessage): Future[Empty] =
    register ! Register.DeleteAgents(in.toAgentSelector)
    Future.successful(Empty())
