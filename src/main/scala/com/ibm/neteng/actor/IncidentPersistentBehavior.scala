package com.ibm.neteng.actor

import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.ibm.neteng.actor.IncidentsPersistentBehavior._

object IncidentPersistentBehavior {

  sealed trait State {
    def updated(event: IncidentEvent): State
  }
  case object Empty extends State {
    def updated(event: IncidentEvent): State = event match {
      case IncidentUpdated(i, d) => Incident(id = i, data = d)
      case IncidentHeld(i, d) => Incident(id = i, data = d)
      case IncidentReleased(i, d) => Incident(id = i, data = d)
      case IncidentResolved(i, d) => Incident(id = i, data = d)
    }
  }
  case class Incident(id: Int, data: String) extends State {
    def updated(event: IncidentEvent): Incident = event match {
      case IncidentUpdated(i, d) => copy(id = i, data = d)
      case IncidentHeld(i, d) => copy(id = i, data = d)
      case IncidentReleased(i, d) => copy(id = i, data = d)
      case IncidentResolved(i, d) => copy(id = i, data = d)
    }
  }

  def apply(id: Int, desc: String): Behavior[IncidentCommand] =
    EventSourcedBehavior[IncidentCommand, IncidentEvent, State](
      PersistenceId.ofUniqueId(s"incident-$id"),
      Empty,
      (state, command) => command match {
        case GetIncident(actorRef) =>
          actorRef ! state
          Effect.none
        case UpdateIncident(i, data) => Effect.persist(IncidentUpdated(i, data))
        case _ => Effect.unhandled
      }, (state, event) => event match {
        case event: IncidentResolved => state.updated(event)
        case event: IncidentEvent => state.updated(event)
        case _ => throw new NotImplementedError("event not handled")
      }
    )

  var state: State = Empty

}
