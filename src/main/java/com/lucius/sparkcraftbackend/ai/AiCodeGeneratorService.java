package com.lucius.sparkcraftbackend.ai;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    @SystemMessage(fromResource = "prompt/GetIdea.txt")
    Flux<String> getIdea(@MemoryId long imageProjectId, @UserMessage String userMessage);


}