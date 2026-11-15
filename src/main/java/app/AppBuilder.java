package app;

import data_access.OpenaiClient;
import data_access.VectorDataAccess;
import data_access.XweatherWeatherClient;
import interface_adapter.agent.AgentController;
import interface_adapter.agent.AgentPresenter;
import interface_adapter.agent.AgentViewModel;
import interface_adapter.weather.WeatherController;
import interface_adapter.weather.WeatherPresenter;
import interface_adapter.weather.WeatherViewModel;
import use_case.agent.AgentInteractor;
import use_case.weather.GetWeatherInputBoundary;
import use_case.weather.GetWeatherInteractor;
import view.AgentPanel;
import view.MainFrame;
import view.WeatherPanel;

/**
 * Creates and connects application components.
 */
public class AppBuilder {
    public MainFrame build() {
        WeatherPanel weatherPanel = buildWeatherPanel();
        AgentPanel agentPanel = buildAgentPanel();

        return new MainFrame(weatherPanel, agentPanel);
    }

    private WeatherPanel buildWeatherPanel() {
        WeatherViewModel viewModel = new WeatherViewModel();
        WeatherPresenter presenter = new WeatherPresenter(viewModel);

        GetWeatherInputBoundary interactor = new GetWeatherInteractor(
                XweatherWeatherClient.fromEnvironment(),
                presenter);

        WeatherController controller = new WeatherController(interactor);

        return new WeatherPanel(controller, viewModel);
    }

    private AgentPanel buildAgentPanel() {
        AgentViewModel viewModel = new AgentViewModel();
        AgentPresenter presenter = new AgentPresenter(viewModel);

        AgentInteractor interactor = new AgentInteractor(
                OpenaiClient.fromEnvironment(),
                presenter,
                VectorDataAccess.fromEnvironment());

        AgentController controller = new AgentController(interactor);

        return new AgentPanel(controller, viewModel);
    }
}