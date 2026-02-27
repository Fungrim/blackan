package io.github.fungrim.blackan.bootstrap.layout;

import java.nio.file.Path;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationLayout {

    @Builder.Default
    private Path bootLibrariesPath = Path.of("boot");
    
    @Builder.Default
    private Path applicationLibrariesPath = Path.of("runtime");

    @Builder.Default
    private String runtimeClassName = "io.github.fungrim.blackan.runtime.RuntimeBootstrap";

}
