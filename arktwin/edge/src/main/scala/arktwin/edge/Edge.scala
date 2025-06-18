// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge

import arktwin.center.services.*
import arktwin.common.util.LoggerConfigurator
import arktwin.edge.actors.adapters.*
import arktwin.edge.actors.sinks.{Chart, Clock, Register}
import arktwin.edge.actors.{DeadLetterListener, EdgeConfigurator}
import arktwin.edge.configs.EdgeConfig
import arktwin.edge.connectors.{ChartConnector, ClockConnector, RegisterConnector}
import arktwin.edge.endpoints.*
import arktwin.edge.util.{EdgeKamon, EdgeReporter}
import buildinfo.BuildInfo
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.stream.BindFailedException
import org.apache.pekko.util.Timeout
import sttp.apispec.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter
import sttp.tapir.swagger.{SwaggerUI, SwaggerUIOptions}

import java.io.{File, FileWriter}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Using}

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

  def main(args: Array[String]): Unit = args.headOption match
    case Some("generate-openapi-center") =>
      args.tail.headOption match
        case Some(pathname) =>
          val file = File(pathname)
          Option(file.getParentFile).foreach(_.mkdirs())
          Using(FileWriter(file))(_.write(centerYaml))
        case None =>
          println(centerYaml)
      sys.exit()

    case Some("generate-openapi-edge") =>
      args.tail.headOption match
        case Some(pathname) =>
          val file = File(pathname)
          Option(file.getParentFile).foreach(_.mkdirs())
          Using(FileWriter(file))(_.write(edgeYaml))
        case None =>
          println(edgeYaml)
      sys.exit()

    case Some("docs") =>
      val config = EdgeConfig.loadOrThrow()
      val rawConfig = EdgeConfig.loadRawOrThrow()

      LoggerConfigurator.init(
        config.static.logLevel,
        config.static.logLevelColor,
        config.static.logSuppressionList
      )

      given actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(
        Behaviors.setup[SpawnProtocol.Command](_ => SpawnProtocol()),
        BuildInfo.name,
        rawConfig
      )
      given ExecutionContextExecutor = actorSystem.executionContext

      Http()
        .newServerAt(config.static.host, config.static.port)
        .bind(
          path("")(getFromResource("root.html")) ~
            path("docs"./)(getFromResource("docs.html")) ~
            PekkoHttpServerInterpreter().toRoute(
              SwaggerUI[Future](
                centerYaml,
                SwaggerUIOptions.default.pathPrefix(List("docs", "center"))
              ) ++
                SwaggerUI[Future](
                  edgeYaml,
                  SwaggerUIOptions.default.pathPrefix(List("docs", "edge"))
                )
            )
        )

    case None =>
      val config = EdgeConfig.loadOrThrow()
      val rawConfig = EdgeConfig.loadRawOrThrow()

      LoggerConfigurator.init(
        config.static.logLevel,
        config.static.logLevelColor,
        config.static.logSuppressionList
      )
      Kamon.init(rawConfig)

      given actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(
        Behaviors.setup[SpawnProtocol.Command](_ => SpawnProtocol()),
        BuildInfo.name,
        rawConfig
      )
      given ExecutionContextExecutor = actorSystem.executionContext
      given Scheduler = actorSystem.scheduler
      given Timeout = config.static.actorTimeout

      scribe.info(BuildInfo.toString)
      scribe.info(config.toJson)
      val grpcClientConfigPath = "pekko.grpc.client.arktwin"
      scribe.info(s"$grpcClientConfigPath: ${rawConfig.getConfig(grpcClientConfigPath)}")
      scribe.debug(rawConfig.toString)

      val grpcSettings = GrpcClientSettings.fromConfig("arktwin")
      val adminClient = AdminClient(grpcSettings)
      val registerClient = RegisterClient(grpcSettings)

      val CreateEdgeResponse(edgeId, runId) =
        Await.result(
          registerClient.createEdge(CreateEdgeRequest(config.static.edgeIdPrefix)),
          1.minute
        )
      scribe.info(s"Run ID: $runId")
      scribe.info(s"Edge ID: $edgeId")

      val kamon = EdgeKamon(runId, edgeId)
      val reporter = EdgeReporter()
      Kamon.addReporter(reporter.getClass.getSimpleName(), reporter)

      val chartConnector = ChartConnector(ChartClient(grpcSettings), config.static, edgeId, kamon)
      val clockConnector = ClockConnector(ClockClient(grpcSettings), edgeId)
      val registerConnector = RegisterConnector(registerClient, config.static, edgeId)

      actorSystem ? DeadLetterListener.spawn(kamon)
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
            config.static,
            config.dynamic.coordinate,
            kamon
          )
        edgeNeighborsAdapter <- actorSystem ?
          EdgeNeighborsQueryAdapter.spawn(
            chart,
            clock,
            register,
            config.static,
            config.dynamic.coordinate,
            kamon
          )
      do
        clockConnector.subscribe(clock)
        registerConnector.subscribe(register)
        chartConnector.subscribe(chart)

        val runServer = (port: Int) =>
          Http()
            .newServerAt(config.static.host, port)
            .bind(
              path("")(getFromResource("root.html")) ~
                path("docs"./)(getFromResource("docs.html")) ~
                path("viewer"./)(getFromResource("viewer/index.html")) ~
                pathPrefix("viewer")(getFromResourceDirectory("viewer/")) ~
                PekkoHttpServerInterpreter().toRoute(
                  SwaggerUI[Future](
                    centerYaml,
                    SwaggerUIOptions.default.pathPrefix(List("docs", "center"))
                  ) ++
                    SwaggerUI[Future](
                      edgeYaml,
                      SwaggerUIOptions.default.pathPrefix(List("docs", "edge"))
                    )
                ) ~
                CenterAgentsDelete.route(adminClient, kamon) ~
                CenterClockSpeedPut.route(adminClient, kamon) ~
                EdgeAgentsPost.route(registerClient, kamon) ~
                EdgeAgentsPut.route(edgeAgentsTransformAdapter, config.static, kamon) ~
                EdgeConfigCoordinatePut.route(configurator, kamon) ~
                EdgeConfigCullingPut.route(configurator, kamon) ~
                EdgeConfigGet.route(configurator, config.static, kamon) ~
                EdgeNeighborsQuery.route(edgeNeighborsAdapter, config.static, kamon) ~
                path("metrics")(complete(PrometheusReporter.latestScrapeData())) ~
                path("health")(complete("OK\n"))
            )

        def runServerRecursively(port: Int): Future[ServerBinding] =
          if config.static.portAutoIncrement
          then
            if port <= (config.static.port + config.static.portAutoIncrementMax)
            then
              runServer(port).recoverWith:
                case _: BindFailedException =>
                  runServerRecursively(port + 1)
            else
              Future(
                throw new BindFailedException:
                  override def getMessage: String =
                    s"Bind failed on ${config.static.host} with port ${config.static.port}-${config.static.port + config.static.portAutoIncrementMax}"
              )
          else runServer(port)

        runServerRecursively(config.static.port).onComplete:
          case Success(server) =>
            scribe.info(s"running on ${server.localAddress.toString}")
          case Failure(e) =>
            scribe.error(e.getMessage)
            LoggerConfigurator.flush()
            sys.exit(1)

    case _ =>
      scribe.error("invalid arguments")
