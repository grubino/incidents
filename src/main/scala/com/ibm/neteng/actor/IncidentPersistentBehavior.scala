package com.ibm.neteng.actor

import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.ibm.neteng.actor.IncidentsPersistentBehavior._

object IncidentPersistentBehavior {

  sealed trait State extends SerializationMarker {
    def updated(event: IncidentEvent): State
  }
  case object Empty extends State {
    override def updated(event: IncidentEvent): State = event match {
      case IncidentReported(i, d) => Incident(i, d)
      case IncidentUpdated(i, d) => Incident(i, d)
      case IncidentHeld(i, d) => Incident(i, d)
      case IncidentReleased(i, d) => Incident(i, d)
      case IncidentResolved(i, d) => Incident(i, d)
    }
  }
  case class Incident(id: Long, data: String) extends State {
    override def updated(event: IncidentEvent): Incident = event match {
      case IncidentReported(i, d) => Incident(i, d)
      case IncidentUpdated(i, d) => copy(id = i, data = d)
      case IncidentHeld(i, d) => copy(id = i, data = d)
      case IncidentReleased(i, d) => copy(id = i, data = d)
      case IncidentResolved(i, d) => copy(id = i, data = d)
    }
  }

  def apply(id: Long, desc: String): Behavior[IncidentCommand] =
    EventSourcedBehavior[IncidentCommand, IncidentEvent, State](
      PersistenceId.ofUniqueId(s"incident-$id"),
      Empty,
      (state, command) => command match {
        case GetIncident(actorRef, _) =>
          state match {
            case i: Incident => actorRef ! i
            case Empty => Effect.unhandled
          }
          Effect.none
        case ReportIncident(i, data) => Effect.persist(IncidentReported(i, data))
        case UpdateIncident(i, data) => Effect.persist(IncidentUpdated(i, data))
        case _ => Effect.unhandled
      }, (state, event) => event match {
        case event: IncidentReported => state.updated(event)
        case event: IncidentResolved => state.updated(event)
        case event: IncidentEvent => state.updated(event)
        case _ => throw new NotImplementedError("event not handled")
      }
    )

  var state: State = Empty

}
