package com.ibm.neteng.endpoint

object IncidentsAPI {

  sealed trait ExtResponses
  final case class ExtResponse(ok: Boolean, err: Option[String]) extends ExtResponses

}
