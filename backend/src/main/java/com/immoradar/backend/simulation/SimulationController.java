package com.immoradar.backend.simulation;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final FinancialSimulationService simulationService;
    private final StressTestService stressTestService;

    public SimulationController(FinancialSimulationService simulationService, StressTestService stressTestService) {
        this.simulationService = simulationService;
        this.stressTestService = stressTestService;
    }

    @PostMapping
    public SimulationResponse simulate(@RequestBody @Valid SimulationRequest request) {
        return simulationService.simulate(request);
    }

    @PostMapping("/stress-test")
    public StressTestResponse stressTest(@RequestBody @Valid StressTestRequest request) {
        return stressTestService.compare(request);
    }
}
