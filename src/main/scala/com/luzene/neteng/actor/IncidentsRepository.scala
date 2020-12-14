package com.luzene.actor

import akka.Done
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import com.luzene.actor.IncidentPersistentBehavior.Incident

import scala.concurrent.{ExecutionContext, Future}

trait IncidentsRepository {
  def update(incident: Incident): Future[Done]
  def getItem(id: Long): Future[Option[Incident]]
}

object IncidentsRepositoryImpl {
  val Keyspace = "akka_projection"
  val IncidentsTable = "incidents"
}

class IncidentsRepositoryImpl(session: CassandraSession)(implicit val ec: ExecutionContext) extends IncidentsRepository {
  import IncidentsRepositoryImpl._

  override def update(incident: Incident): Future[Done] = {
    session.executeWrite(s"UPDATE $Keyspace.$IncidentsTable SET data = ? WHERE incident_id = ?", incident.data, incident.id.toString)
  }

  override def getItem(id: Long): Future[Option[Incident]] = {
    session.selectOne(s"SELECT incident_id, data FROM $Keyspace.$IncidentsTable WHERE incident_id = ?", id.toString)
      .map(opt => opt.map(row => Incident(row.getLong("incident_id"), row.getString("data"))))
  }
}