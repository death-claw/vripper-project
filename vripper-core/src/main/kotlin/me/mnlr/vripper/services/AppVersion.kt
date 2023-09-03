package me.mnlr.vripper.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AppVersion(@param:Value("\${build.version}") val version: String, @param:Value("\${build.timestamp}") val timestamp: String)