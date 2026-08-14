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

    public SimulationController(FinancialSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    public SimulationResponse simulate(@RequestBody @Valid SimulationRequest request) {
        return simulationService.simulate(request);
    }
}
