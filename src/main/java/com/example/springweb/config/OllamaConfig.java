package com.example.springweb.config;

import com.example.springweb.service.Assistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class OllamaConfig {

    private final String baseUrl = "http://127.0.0.1:11434";

    private final String modelName = "llama3:latest";

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(){
        return OllamaStreamingChatModel.builder().baseUrl(baseUrl).modelName(modelName).build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel(){
        return OllamaChatModel.builder().baseUrl(baseUrl).modelName(modelName).build();
    }

    @Bean
    public EmbeddingModel embeddingModel(){
        return OllamaEmbeddingModel.builder().baseUrl(baseUrl).modelName(modelName).build();
    }

    @Bean
    public DataSource dataSource(){
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql:localhost:5432");
        dataSource.setDatabaseName("postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("password");
        return dataSource;
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource,
                                                      EmbeddingModel embeddingModel){

        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("tb_v_emb")
                .dimension(embeddingModel.dimension())
                .createTable(true)
                .build();
    }


    @Bean
    public ChatMemory chatMemory(){
        return new MessageWindowChatMemory.Builder()
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .maxMessages(50).build();
    }

    @Bean
    public Assistant assistant(ChatMemory chatMemory, EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore,
                               ChatLanguageModel chatLanguageModel,
                               StreamingChatLanguageModel streamingChatLanguageModel){
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore).embeddingModel(embeddingModel).maxResults(10).build();
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .chatMemory(chatMemory).contentRetriever(contentRetriever)
                .build();
    }

}
