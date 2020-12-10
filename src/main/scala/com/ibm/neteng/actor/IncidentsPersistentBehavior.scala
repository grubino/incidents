package com.ibm.neteng.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.ibm.neteng.actor.IncidentPersistentBehavior.Incident
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider


object IncidentsPersistentBehavior {

  // events
  sealed trait IncidentEvent extends SerializationMarker
  final case class IncidentReported(id: Long, data: String) extends IncidentEvent
  final case class IncidentUpdated(id: Long, data: String) extends IncidentEvent
  final case class IncidentHeld(id: Long, data: String) extends IncidentEvent
  final case class IncidentReleased(id: Long, data: String) extends IncidentEvent
  final case class IncidentResolved(id: Long, data: String) extends IncidentEvent

  // commands
  sealed trait IncidentCommand
  final case class ReportIncident(id: Long, data: String) extends IncidentCommand
  final case class UpdateIncident(id: Long, data: String) extends IncidentCommand
  final case class HoldIncident(id: Long, data: String) extends IncidentCommand
  final case class ReleaseIncident(id: Long, data: String) extends IncidentCommand
  final case class ResolveIncident(id: Long, data: String) extends IncidentCommand

  // queries
  sealed trait IncidentQuery extends IncidentCommand
  final case class GetAllIncidents(replyTo: ActorRef[IncidentsHistory]) extends IncidentQuery
  final case class GetIncident(replyTo: ActorRef[Incident], id: Long) extends IncidentQuery

  implicit private val incidentOrdering: Ordering[IncidentReported] = Ordering.fromLessThan[IncidentReported](_.id < _.id)
  case class IncidentsHistory(history: List[IncidentReported] = Nil) extends SerializationMarker {
    def updated(event: IncidentReported): IncidentsHistory = copy(event :: history)
    def size: Int = history.size
    override def toString: String = history.toString
  }

  def apply(entityId: String): Behavior[IncidentCommand] =
    Behaviors.setup { context =>

      EventSourcedBehavior[IncidentCommand, IncidentEvent, IncidentsHistory](
        persistenceId = PersistenceId.ofUniqueId(entityId),
        emptyState = IncidentsHistory(),
        (state, command) => {
          command match {
            case ReportIncident(id, data) =>
              val ev = IncidentReported(id, data)
              val historySet = Set.from(state.history.map(_.id))
              if (historySet contains ev.id) Effect.none else Effect.persist(ev)
            case update @ UpdateIncident(id, data) =>
              val incidentServiceKey = ServiceKey[IncidentCommand](s"incident-$id")
              context.system.receptionist ! Receptionist.Find(
                incidentServiceKey,
                context.spawnAnonymous(Behaviors.receiveMessage[Receptionist.Listing] {
                  case incidentServiceKey.Listing(listing) =>
                    context.system.log.info(s"found listing: $listing")
                    listing.foreach(actor => actor ! update)
                    Behaviors.stopped
                }))
              Effect.none
            case GetAllIncidents(actorRef) =>
              actorRef ! state
              Effect.none
            case get @ GetIncident(actorRef, id) =>
              val incidentServiceKey = ServiceKey[IncidentCommand](s"incident-$id")
              context.system.receptionist ! Receptionist.Find(
                incidentServiceKey,
                context.spawnAnonymous(Behaviors.receiveMessage[Receptionist.Listing] {
                  case incidentServiceKey.Listing(listing) =>
                    context.system.log.info(s"found listing: $listing")
                    listing.foreach(actor => actor ! get)
                    Behaviors.stopped
                }))
              Effect.none
          }
        }, (state, event) => {
          event match {
            case inc @ IncidentReported(id, data) =>
              val incidentServiceKey = ServiceKey[IncidentCommand](s"incident-$id")
              val incidentActor = context.spawn(IncidentPersistentBehavior(id, data), s"incident-$id")
              incidentActor ! ReportIncident(id, data)
              context.system.receptionist ! Receptionist.Register(incidentServiceKey, incidentActor)
              state.updated(inc)

          }
        }
      )
    }

  var state = IncidentsHistory()

  def updatedState(event: IncidentReported): Unit = state.updated(event)
  def numIncidents: Int = state.size
  val snapshotInterval = 1000

}
