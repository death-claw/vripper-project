package me.vripper.web.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import me.vripper.delegate.LoggerDelegate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class GrpcServer(
    @Value("\${grpc.port}") private val port: Int,
    grpcServerAppEndpointService: GrpcServerAppEndpointService
) {

    private val log by LoggerDelegate()

    private val server: Server =
        ServerBuilder.forPort(port).addService(grpcServerAppEndpointService).build()

    @PostConstruct
    fun start() {
        log.info("Starting gRPC server on port $port")
        server.start()
    }
}