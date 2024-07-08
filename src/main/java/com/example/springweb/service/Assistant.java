package com.example.springweb.service;

import dev.langchain4j.service.TokenStream;

public interface Assistant {

    TokenStream streamChat(String question);

    String chat(String question);
}
