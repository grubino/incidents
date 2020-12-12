package com.ibm.neteng.actor

import akka.Done
import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import com.ibm.neteng.actor.IncidentPersistentBehavior.Incident

import scala.concurrent.{ExecutionContext, Future}

object IncidentProjectionHandler {
}

class IncidentProjectionHandler(tag: String, system: ActorSystem[_], repo: IncidentsRepository)
  extends Handler[EventEnvelope[IncidentsPersistentBehavior.IncidentEvent]]() {
  import IncidentsPersistentBehavior.{IncidentReported, IncidentHeld, IncidentReleased, IncidentResolved, IncidentUpdated}

  private implicit val ec: ExecutionContext = system.executionContext

  override def process(envelope: EventEnvelope[IncidentsPersistentBehavior.IncidentEvent]): Future[Done] = {
    val processed = envelope.event match {
      case IncidentReported(id, data) => repo.update(Incident(id, data))
    }
    processed
  }
}
