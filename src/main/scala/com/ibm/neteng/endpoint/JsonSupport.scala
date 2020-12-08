package com.ibm.neteng.endpoint

import IncidentsAPI._
import com.ibm.neteng.actor.IncidentPersistentBehavior.Incident
import com.ibm.neteng.actor.IncidentsPersistentBehavior.{IncidentHeld, IncidentResolved, IncidentUpdated, UpdateIncident}
import com.ibm.neteng.actor.IncidentsPersistentBehavior.{IncidentReported, IncidentsHistory, ReportIncident}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonSupport  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  implicit val incident: RootJsonFormat[Incident] = jsonFormat2(Incident)
  implicit val incidentReported: RootJsonFormat[IncidentReported] = jsonFormat1(IncidentReported)
  implicit val incidentsHistory: RootJsonFormat[IncidentsHistory] = jsonFormat1(IncidentsHistory)
  implicit val reportIncident: RootJsonFormat[ReportIncident] = jsonFormat2(ReportIncident)
  implicit val updateIncident: RootJsonFormat[UpdateIncident] = jsonFormat2(UpdateIncident)
  implicit val incidentUpdated: RootJsonFormat[IncidentUpdated] = jsonFormat2(IncidentUpdated)
  implicit val incidentHeld: RootJsonFormat[IncidentHeld] = jsonFormat2(IncidentHeld)
  implicit val incidentReleased: RootJsonFormat[IncidentHeld] = jsonFormat2(IncidentHeld)
  implicit val incidentResolved: RootJsonFormat[IncidentResolved] = jsonFormat2(IncidentResolved)
  implicit val psResponse: RootJsonFormat[ExtResponse] = jsonFormat2(ExtResponse)

}
