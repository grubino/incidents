package com.ibm.neteng.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.ibm.neteng.actor.IncidentPersistentBehavior.State

object IncidentsPersistentBehavior {

  // events
  sealed trait IncidentEvent extends SerializationMarker
  final case class IncidentReported(id: Int) extends IncidentEvent
  final case class IncidentUpdated(id: Int, data: String) extends IncidentEvent
  final case class IncidentHeld(id: Int, data: String) extends IncidentEvent
  final case class IncidentReleased(id: Int, data: String) extends IncidentEvent
  final case class IncidentResolved(id: Int, data: String) extends IncidentEvent

  // commands
  sealed trait IncidentCommand
  final case class ReportIncident(id: Int, data: String) extends IncidentCommand
  final case class UpdateIncident(id: Int, data: String) extends IncidentCommand
  final case class HoldIncident(id: Int, data: String) extends IncidentCommand
  final case class ReleaseIncident(id: Int, data: String) extends IncidentCommand
  final case class ResolveIncident(id: Int, data: String) extends IncidentCommand

  // queries
  sealed trait IncidentQuery extends IncidentCommand
  final case class GetAllIncidents(replyTo: ActorRef[IncidentsHistory]) extends IncidentQuery
  final case class GetIncident(replyTo: ActorRef[State]) extends IncidentQuery

  case class IncidentsHistory(history: List[IncidentReported] = Nil) extends SerializationMarker {
    def updated(event: IncidentReported): IncidentsHistory = copy(event :: history)
    def size: Int = history.size
    override def toString: String = history.reverse.toString
  }

  def apply(entityId: String): Behavior[IncidentCommand] =
    Behaviors.setup { context =>

      EventSourcedBehavior[IncidentCommand, IncidentEvent, IncidentsHistory](
        persistenceId = PersistenceId.ofUniqueId(entityId),
        emptyState = IncidentsHistory(),
        (state, command) => {
          command match {
            case ReportIncident(id, data) =>
              val incidentServiceKey = ServiceKey[IncidentCommand](s"incident-$id")
              val incidentActor = context.spawn(IncidentPersistentBehavior(id, data), s"incident-$id")
              context.system.receptionist ! Receptionist.Register(incidentServiceKey, incidentActor)
              Effect.persist(IncidentReported(id))
            case update @ UpdateIncident(id, data) =>
              val incidentServiceKey = ServiceKey[IncidentCommand](s"incident-$id")
              context.system.receptionist ! Receptionist.Find(
                incidentServiceKey,
                context.spawnAnonymous(Behaviors.receiveMessage[Receptionist.Listing] {
                  case incidentServiceKey.Listing(listing) =>
                    listing.foreach(actor => actor ! update)
                    Behaviors.stopped
                }))
              Effect.none
            case GetAllIncidents(actorRef) =>
              actorRef ! state
              Effect.none
          }
        }, (state, event) => {
          event match {
            case inc: IncidentReported =>
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
