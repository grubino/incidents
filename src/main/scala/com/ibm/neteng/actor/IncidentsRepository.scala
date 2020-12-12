package com.ibm.neteng.actor

import akka.Done
import akka.persistence.cassandra.session.scaladsl.CassandraSession
import com.ibm.neteng.actor.IncidentPersistentBehavior.Incident

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
    session.executeWrite(s"UPDATE $Keyspace.$IncidentsTable SET data = ? WHERE incident_id = ?", incident.data, incident.id)
  }

  override def getItem(id: Long): Future[Option[Incident]] = {
    session.selectOne(s"SELECT incident_id, data FROM $Keyspace.$IncidentsTable WHERE incident_id = ?", id)
      .map(opt => opt.map(row => Incident(row.getLong("incident_id"), row.getString("data"))))
  }
}