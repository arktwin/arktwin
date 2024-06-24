/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.services

import arktwin.center.actors.{Clock, Register}
import com.google.protobuf.empty.Empty
import org.apache.pekko.actor.typed.ActorRef

import scala.concurrent.Future

class AdminService(clock: ActorRef[Clock.Message], register: ActorRef[Register.Message]) extends Admin:
  override def centerClockSpeedPut(in: CenterClockSpeedPutRequest): Future[Empty] =
    clock ! Clock.SpeedUpdate(in.clockSpeed)
    Future.successful(Empty())

  override def centerAgentsDelete(in: AgentSelectorMessage): Future[Empty] =
    register ! Register.AgentsDelete(in.toAgentSelector)
    Future.successful(Empty())
