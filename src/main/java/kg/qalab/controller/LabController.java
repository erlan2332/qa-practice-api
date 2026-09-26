package kg.qalab.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kg.qalab.dto.lab.EchoRequest;
import kg.qalab.dto.lab.EchoResponse;
import kg.qalab.service.LabService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @PostMapping("/echo")
    public EchoResponse echo(
        @Valid @RequestBody EchoRequest request,
        HttpServletRequest servletRequest
    ) {
        return labService.echo(
            servletRequest.getMethod(),
            servletRequest.getRequestURI(),
            request
        );
    }
}
