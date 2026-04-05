package io.github.fungrim.blackan.test;

import io.github.fungrim.blackan.extension.grcp.GrcpService;
import io.github.fungrim.blackan.test.grpc.GreeterGrpc;
import io.github.fungrim.blackan.test.grpc.HelloReply;
import io.github.fungrim.blackan.test.grpc.HelloRequest;
import io.grpc.stub.StreamObserver;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GrcpService
@ApplicationScoped
public class GreeterServiceImpl extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        log.info("Received gRPC request from: {}", request.getName());
        String message = "Hello, " + request.getName() + "!";
        HelloReply reply = HelloReply.newBuilder()
                .setMessage(message)
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
