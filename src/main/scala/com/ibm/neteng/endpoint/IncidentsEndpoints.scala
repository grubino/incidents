package com.ibm.neteng.endpoint

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.util.Timeout
import com.ibm.neteng.actor.IncidentsPersistentBehavior._
import com.ibm.neteng.endpoint.IncidentsAPI._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

class IncidentsEndpoints(system: ActorSystem[Nothing], psCommandActor: ActorRef[IncidentCommand]) {

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))
  private implicit val ec: ExecutionContextExecutor = system.executionContext

  implicit val scheduler: Scheduler = system.scheduler
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonSupport._

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case ex: Exception =>
        extractUri { uri =>
          val msg = s"Request to $uri could not be handled normally: Exception: ${ex.getCause} : ${ex.getMessage}"
          system.log.error(msg)
          complete(HttpResponse(StatusCodes.InternalServerError, entity = msg))
        }
    }

  lazy val psRoutes: Route =
    pathPrefix("incidents") {
      concat(
        post {
          entity(as[ReportIncident]) { req =>
            psCommandActor ! req
            complete(StatusCodes.OK, ExtResponse(ok = true, err = None))
          }
        },
        get {
          val incidents = psCommandActor.ask {
            replyTo: ActorRef[IncidentsHistory] => GetAllIncidents(replyTo)
          }.mapTo[IncidentsHistory]
          complete(incidents)
        },
        pathPrefix(IntNumber) { incidentId =>
          put {
            entity(as[UpdateIncident]) { req =>
              psCommandActor ! req
              complete(StatusCodes.OK, ExtResponse(ok = true, err = None))
            }
          }
        }
      )

    }
}