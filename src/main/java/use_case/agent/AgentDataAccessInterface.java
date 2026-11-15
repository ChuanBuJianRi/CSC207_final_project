package use_case.agent;

import entity.Memory;

import java.util.List;

public interface AgentDataAccessInterface {
    void save(Memory memory);

    List<Memory> findRelevant(String query);
}
