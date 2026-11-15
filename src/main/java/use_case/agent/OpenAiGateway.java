package use_case.agent;

public interface OpenAiGateway {
    OpenAiResponse complete(OpenAiRequest request);
}
