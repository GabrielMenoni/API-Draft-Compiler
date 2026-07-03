package br.ufscar.dc.compiladores.apidraft.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Utilitário compartilhado pelos geradores para preparar o diretório de saída. */
final class GeneratedOutput {
    private GeneratedOutput() {
    }

    /**
     * Apaga {@code directory} (se existir) e a recria vazia, garantindo que uma
     * geração não deixe para trás arquivos de uma execução anterior com entidades
     * ou rotas removidas.
     */
    static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> paths = Files.walk(directory)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(directory);
    }
}
