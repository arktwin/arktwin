/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.edge

import arktwin.center.services.*
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.actors.adapters.*
import arktwin.edge.actors.sinks.{Chart, Clock, Register}
import arktwin.edge.connectors.{ChartConnector, ClockConnector, RegisterConnector}
import arktwin.edge.endpoints.*
import arktwin.edge.util.EdgeReporter
import buildinfo.BuildInfo
import io.grpc.StatusRuntimeException
import kamon.Kamon
import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.util.Timeout
import sttp.apispec.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter
import sttp.tapir.swagger.{SwaggerUI, SwaggerUIOptions}

import java.io.{File, FileWriter}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Using

object Edge:
  val centerYaml: String = OpenAPIDocsInterpreter()
    .toOpenAPI(
      Seq(CenterAgentsDelete.endpoint, CenterClockSpeedPut.endpoint),
      BuildInfo.name + "/center",
      BuildInfo.version
    )
    .toYaml

  val edgeYaml: String = OpenAPIDocsInterpreter()
    .toOpenAPI(
      Seq(
        EdgeAgentsPost.endpoint,
        EdgeAgentsPut.endpoint,
        EdgeConfigCoordinatePut.endpoint,
        EdgeConfigCullingPut.endpoint,
        EdgeConfigGet.endpoint,
        EdgeNeighborsQuery.endpoint
      ),
      BuildInfo.name + "/edge",
      BuildInfo.version
    )
    .toYaml

  def main(args: Array[String]): Unit =
    args.headOption match
      case Some("openapi-center") =>
        args.tail.headOption match
          case Some(pathname) =>
            val file = File(pathname)
            Option(file.getParentFile).foreach(_.mkdirs())
            Using(FileWriter(file))(_.write(centerYaml))
          case None =>
            println(centerYaml)
        sys.exit()
      case Some("openapi-edge") =>
        args.tail.headOption match
          case Some(pathname) =>
            val file = File(pathname)
            Option(file.getParentFile).foreach(_.mkdirs())
            Using(FileWriter(file))(_.write(edgeYaml))
          case None =>
            println(edgeYaml)
        sys.exit()
      case _ =>
        run()

  def run(): Unit =
    val config = EdgeConfig.loadOrThrow()
    val rawConfig = EdgeConfig.loadRawOrThrow()

    Kamon.init(rawConfig)

    given actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(
      Behaviors.setup[SpawnProtocol.Command](_ => SpawnProtocol()),
      BuildInfo.name,
      rawConfig
    )
    given ExecutionContextExecutor = actorSystem.executionContext
    given Scheduler = actorSystem.scheduler
    given Timeout = config.static.actorTimeout

    Kamon.addReporter(EdgeReporter.getClass.getSimpleName, EdgeReporter(actorSystem.log))

    actorSystem.log.info(BuildInfo.toString)
    actorSystem.log.info(config.toString)
    actorSystem.log.debug(rawConfig.toString)

    val grpcSettings = GrpcClientSettings.fromConfig("arktwin")
    val adminClient = AdminClient(grpcSettings)
    val registerClient = RegisterClient(grpcSettings)

    try
      val edgeId =
        Await.result(registerClient.edgeCreate(EdgeCreateRequest(config.static.edgeIdPrefix)), 1.minute).edgeId
      actorSystem.log.info(s"Edge ID: $edgeId")

      val chartConnector = ChartConnector(ChartClient(grpcSettings), config.static, edgeId)
      val clockConnector = ClockConnector(ClockClient(grpcSettings), edgeId)
      val registerConnector = RegisterConnector(registerClient, config.static, edgeId)

      for
        clock <- actorSystem ? Clock.spawn(config.static)
        register <- actorSystem ? Register.spawn()
        chart <- actorSystem ? Chart.spawn(config.dynamic.culling)
        configurator <- actorSystem ? EdgeConfigurator.spawn(config)
        chartPublish = chartConnector.publish()
        registerPublish = registerConnector.publish()
        edgeAgentsTransformAdapter <- actorSystem ?
          EdgeAgentsPutAdapter.spawn(
            chart,
            chartPublish,
            registerPublish,
            clock,
            edgeId,
            config.static,
            config.dynamic.coordinate
          )
        edgeNeighborsAdapter <- actorSystem ?
          EdgeNeighborsQueryAdapter.spawn(chart, clock, register, edgeId, config.static, config.dynamic.coordinate)
      do
        clockConnector.subscribe(clock)
        registerConnector.subscribe(register)
        chartConnector.subscribe(chart)

        Http()
          .newServerAt(config.static.host, config.static.port)
          .bind(
            path("")(getFromResource("root.html")) ~
              path("docs"./)(getFromResource("docs.html")) ~
              PekkoHttpServerInterpreter().toRoute(
                SwaggerUI[Future](centerYaml, SwaggerUIOptions.default.pathPrefix(List("docs", "center"))) ++
                  SwaggerUI[Future](edgeYaml, SwaggerUIOptions.default.pathPrefix(List("docs", "edge")))
              ) ~
              CenterAgentsDelete.route(adminClient, edgeId) ~
              CenterClockSpeedPut.route(adminClient, edgeId) ~
              EdgeAgentsPost.route(registerClient, edgeId) ~
              EdgeAgentsPut.route(edgeAgentsTransformAdapter, edgeId, config.static) ~
              EdgeConfigCoordinatePut.route(configurator, edgeId) ~
              EdgeConfigCullingPut.route(configurator, edgeId) ~
              EdgeConfigGet.route(configurator, edgeId, config.static) ~
              EdgeNeighborsQuery.route(edgeNeighborsAdapter, edgeId, config.static)
          )
          .foreach(server => actorSystem.log.info(server.localAddress.toString))
    catch
      case e: StatusRuntimeException =>
        Http()
          .newServerAt(config.static.host, config.static.port)
          .bind(
            path("")(getFromResource("root.html")) ~
              path("docs"./)(getFromResource("docs.html")) ~
              PekkoHttpServerInterpreter().toRoute(
                SwaggerUI[Future](centerYaml, SwaggerUIOptions.default.pathPrefix(List("docs", "center"))) ++
                  SwaggerUI[Future](edgeYaml, SwaggerUIOptions.default.pathPrefix(List("docs", "edge")))
              )
          )
        throw e
