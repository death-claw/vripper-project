package me.vripper.web.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import me.vripper.utilities.LoggerDelegate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class GrpcServer(
    @Value("\${grpc.enabled}") private val enabled: Boolean,
    @Value("\${grpc.port}") private val port: Int,
    grpcServerAppEndpointService: GrpcServerAppEndpointService
) {

    private val log by LoggerDelegate()

    private val server: Server =
        ServerBuilder.forPort(port).addService(grpcServerAppEndpointService).build()

    @PostConstruct
    fun start() {
        if (enabled) {
            log.info("gRPC is enabled, starting gRPC server on port $port")
            server.start()
        } else {
            log.info("gRPC is disabled, remote connection to this instance is not possible")
        }
    }
}