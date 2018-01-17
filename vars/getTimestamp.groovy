#!/usr/bin/env groovy

def call() {
  TimeZone.setDefault(TimeZone.getTimeZone('UTC'))
  def now = new Date()
  return now.format("yyyyMMddHHmmss")
}
