package org.test;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class EmbeddedFilesExtractor {
    private Parser parser = new AutoDetectParser();

    public void extract(InputStream is, Path outputDir) throws SAXException, TikaException, IOException {
        Metadata m = new Metadata();
        ParseContext c = new ParseContext();
        ContentHandler h = new BodyContentHandler(-1);

        PDFParserConfig config = new PDFParserConfig();
        config.setExtractInlineImages(true);
        c.set(PDFParserConfig.class, config);

        c.set(Parser.class, parser);
        org.apache.tika.extractor.EmbeddedDocumentExtractor ex = new EmbeddedDocumentExtractor(outputDir, parser, c);
        c.set(org.apache.tika.extractor.EmbeddedDocumentExtractor.class, ex);

        parser.parse(is, h, m, c);
    }

    private class EmbeddedDocumentExtractor extends ParsingEmbeddedDocumentExtractor {
        private final Path outputDir;
        private int fileCount = 0;
        private Detector detector;
        private TikaConfig config;

        private EmbeddedDocumentExtractor(Path outputDir, Parser parser, ParseContext context) {
            super(context);
            this.outputDir = outputDir;
            detector = ((AutoDetectParser) parser).getDetector();
            config = TikaConfig.getDefaultConfig();
        }

        @Override
        public boolean shouldParseEmbedded(Metadata metadata) {
            return true;
        }

        @Override
        public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml)
                throws SAXException, IOException {

            String name = metadata.get(Metadata.RESOURCE_NAME_KEY);

            if (name == null) {
                name = "file_" + fileCount++;
            } else {
                name = FilenameUtils.normalize(FilenameUtils.getName(name));
            }

            MediaType contentType = detector.detect(stream, metadata);

            if (name.indexOf('.') == -1 && contentType != null) {
                try {
                    String extension = config.getMimeRepository().forName(contentType.toString()).getExtension();
                    name += extension;
                } catch (MimeTypeException e) {
                    e.printStackTrace();
                }
            }

            Path outputFile = outputDir.resolve(name);
            Files.createDirectories(outputFile.getParent());
            Files.copy(stream, outputFile);
        }
    }
}