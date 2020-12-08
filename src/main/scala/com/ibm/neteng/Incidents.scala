package com.ibm.neteng

import akka.actor.{Props, TypedActor, TypedProps}
import akka.actor.typed.{ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.complete
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
import com.ibm.neteng.actor.IncidentsPersistentBehavior
import com.ibm.neteng.actor.IncidentsPersistentBehavior.IncidentCommand
import com.typesafe.config.ConfigFactory
import com.ibm.neteng.endpoint.IncidentsEndpoints

object Incidents {

  private val appConfig = ConfigFactory.load()

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Behaviors.setup[Nothing] { context =>
      import akka.actor.typed.scaladsl.adapter._
      implicit val classicSystem = context.system.toClassic
      implicit val ec = context.system.executionContext

      val cluster = Cluster(context.system)
      context.log.info("Started [" + context.system + "], cluster.selfAddress = " + cluster.selfMember.address + ")")

      if (cluster.selfMember.hasRole("endpoint")) {
        val incidentCommandActor = context.spawn(IncidentsPersistentBehavior("incident"), "incident")
        lazy val routes = new IncidentsEndpoints(context.system, incidentCommandActor)
        Http().newServerAt("0.0.0.0", 8080).bindFlow(routes.psRoutes)
      } else if (cluster.selfMember.hasRole("cluster")) {
        context.spawn(IncidentsPersistentBehavior("incident"), "incidents")
      }

      // Create an actor that handles cluster domain events
      val listener = context.spawn(Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
        ctx.log.info("MemberEvent: {}", event)
        Behaviors.same
      }), "listener")

      Cluster(context.system).subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

      AkkaManagement.get(classicSystem).start()
      ClusterBootstrap.get(classicSystem).start()
      Behaviors.empty
    }, "incidents")
  }

}
