package com.luzene

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory

import scala.concurrent.Future

// Enabled in application.conf
class IncidentsHealthCheck(system: ActorSystem) extends (() => Future[Boolean]) {
  private val log = LoggerFactory.getLogger(getClass)

  override def apply(): Future[Boolean] = {
    log.info("IncidentsHealthCheck called")
    Future.successful(true)
  }
}
