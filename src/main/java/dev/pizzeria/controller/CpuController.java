package dev.pizzeria.controller;

import dev.pizzeria.model.Result;
import dev.pizzeria.service.CpuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 3. The dough station: CPU-bound work. Try http://localhost:8080/cpu/virtual */
@RestController
@RequestMapping("/cpu")
public class CpuController {

    private final CpuService cpuService;

    public CpuController(CpuService cpuService) {
        this.cpuService = cpuService;
    }

    @GetMapping("/traditional")
    public Result traditional() throws Exception {
        return cpuService.traditional();
    }

    @GetMapping("/virtual")
    public Result virtual() {
        return cpuService.virtual();
    }
}
