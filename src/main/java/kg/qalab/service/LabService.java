package kg.qalab.service;

import kg.qalab.dto.lab.EchoRequest;
import kg.qalab.dto.lab.EchoResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LabService {

    public EchoResponse echo(
        String method,
        String path,
        EchoRequest request
    ) {
        return new EchoResponse(
            method,
            path,
            request,
            Instant.now()
        );
    }
}
