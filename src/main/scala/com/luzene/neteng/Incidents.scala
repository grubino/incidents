package com.luzene

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.{ProjectionBehavior, ProjectionId, cassandra}
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.scaladsl.SourceProvider
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import com.luzene.actor.{IncidentProjectionHandler, IncidentsPersistentBehavior, IncidentsRepositoryImpl, IncidentsTags}
import com.typesafe.config.ConfigFactory
import com.luzene.endpoint.IncidentsEndpoints


object Incidents {

  private val appConfig = ConfigFactory.load()

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Behaviors.setup[Nothing] { context =>
      import akka.actor.typed.scaladsl.adapter._
      implicit val system = context.system
      implicit val ec = context.system.executionContext
      val entityId = "incidents"

      val cluster = Cluster(system)
      context.log.info("Started [" + system + "], cluster.selfAddress = " + cluster.selfMember.address + ")")

      if (cluster.selfMember.hasRole("endpoint")) {
        val incidentCommandActor = context.spawn(IncidentsPersistentBehavior(entityId), "incident")
        lazy val routes = new IncidentsEndpoints(system, incidentCommandActor)
        Http().newServerAt("0.0.0.0", 8080).bindFlow(routes.psRoutes)
      } else if (cluster.selfMember.hasRole("cluster")) {
        context.spawn(IncidentsPersistentBehavior(entityId), "incidents")

        val sourceProvider: SourceProvider[Offset, EventEnvelope[IncidentsPersistentBehavior.IncidentEvent]] =
          EventSourcedProvider.eventsByTag[IncidentsPersistentBehavior.IncidentEvent](
            system,
            readJournalPluginId = CassandraReadJournal.Identifier,
            tag = IncidentsTags.Single
          )

        val session = CassandraSessionRegistry(system).sessionFor("akka.projection.cassandra.session-config")
        val repo = new IncidentsRepositoryImpl(session)
        val projection = CassandraProjection.atLeastOnce(
          projectionId = ProjectionId(entityId, IncidentsTags.Single),
          sourceProvider,
          handler = () => new IncidentProjectionHandler(IncidentsTags.Single, system, repo))
        context.spawn(ProjectionBehavior(projection), projection.projectionId.id)

      }

      // Create an actor that handles cluster domain events
      val listener = context.spawn(Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
        ctx.log.info("MemberEvent: {}", event)
        Behaviors.same
      }), "listener")

      Cluster(system).subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

      AkkaManagement(system).start()
      ClusterBootstrap(system).start()
      Behaviors.empty
    }, "incidents")
  }

}
