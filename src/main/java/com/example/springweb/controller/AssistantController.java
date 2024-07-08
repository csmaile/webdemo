package com.example.springweb.controller;

import cn.hutool.core.io.FileUtil;
import com.example.springweb.service.Assistant;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.source.FileSystemSource;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.spi.data.document.parser.DocumentParserFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/gpt")
public class AssistantController {

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private Assistant assistant;

    @RequestMapping("/embedding")
    public ResponseEntity<?> embedding(@RequestParam("file") MultipartFile file) {
        try {
            File tempFile = FileUtil.createTempFile();
            file.transferTo(tempFile);
            FileSystemSource source = FileSystemSource.from(tempFile);
            Document document = DocumentLoader.load(source, get("text").create());
            DocumentSplitter splitter = DocumentSplitters.recursive(2000, 200);
            List<TextSegment> segments = splitter.split(document);
            List<String> strings = embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);
            return ResponseEntity.ok(strings);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @RequestMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE, method = RequestMethod.GET)
    public Flux<String> streamChat(String question){
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        TokenStream ts = assistant.streamChat(question);
        ts.onNext(sink::tryEmitNext)
                .onError(sink::tryEmitError)
                .start();

        return sink.asFlux();
    }

    @RequestMapping(value = "/chat", method = RequestMethod.GET)
    public ResponseEntity<String> chat(String question){

        String ts = assistant.chat(question);

        return ResponseEntity.ok(ts);
    }

    public static DocumentParserFactory get(String type){
        if (Arrays.asList("doc", "xlsx", "xls", "csv").contains(type)) {
            return ApachePoiDocumentParser::new;
        }
        return TextDocumentParser::new;
    }
}
