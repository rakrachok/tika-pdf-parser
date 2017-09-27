package org.test;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new RuntimeException("Wrong number of arguments. Please specify input and output directory.");
        }

        String input = args[0];
        if (input.endsWith("/") || input.endsWith("\\")) {
            input = input.substring(0, input.length() - 1);
        }

        String output = args[1];
        if (output.endsWith("/") || output.endsWith("\\")) {
            output = output.substring(0, output.length() - 1);
        }

        EmbeddedFilesExtractor extractor = new EmbeddedFilesExtractor();

        String finalInput = input;
        String finalOutput = output;
        log.info("Start");
        Files.walk(Paths.get(input))
                .filter(path -> FilenameUtils.isExtension(path.toString().toLowerCase(), "pdf")
                        && !Files.isDirectory(path))
                .forEach(path -> {
                    try (InputStream fis = new FileInputStream(path.toFile())) {
                        log.info("Processing: " + path);
                        String newOutput = path.toString().replace(finalInput, finalOutput);
                        extractor.extract(fis, Paths.get(newOutput));
                    } catch (TikaException | IOException | SAXException e) {
                        log.error("Error occurred during the file {" + path.toString() + "} being processed.", e);
                    }
                });
        log.info("Processed");
    }
}
