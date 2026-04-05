# Hello World gRPC Example

This is a simple gRPC example application demonstrating the Blackan framework's gRPC extension.

## Building

```bash
./gradlew clean build
```

## Running

```bash
./gradlew blackanCreateRunJar
java -jar build/blackan-app/blackan-run.jar
```

The gRPC server will start on port 9090.

## Testing with grpcurl

You can test the gRPC service using grpcurl:

```bash
grpcurl -plaintext -d '{"name": "World"}' localhost:9090 helloworld.Greeter/SayHello
```

Expected response:
```json
{
  "message": "Hello, World!"
}
```

## Project Structure

- `src/main/proto/helloworld.proto` - gRPC service definition
- `src/main/java/io/github/fungrim/blackan/test/GreeterServiceImpl.java` - gRPC service implementation
- `src/main/java/io/github/fungrim/blackan/test/HelloWorldService.java` - Application lifecycle service
- `src/main/resources/application.properties` - gRPC server configuration

## Configuration

The gRPC server can be configured in `application.properties`:

- `blackan.grpc.server.port` - Server port (default: 9090)
- `blackan.grpc.server.ip` - Server IP address (default: 0.0.0.0)
