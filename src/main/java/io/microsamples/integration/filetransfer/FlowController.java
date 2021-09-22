package io.microsamples.integration.filetransfer;

import io.microsamples.integration.filetransfer.config.FlowInvoker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("flow")
public class FlowController {

    private FlowInvoker sender;

    public FlowController(FlowInvoker sender) {
        this.sender = sender;
    }

    @GetMapping
    public ResponseEntity invoke() {

        sender.invoke();
        return new ResponseEntity(HttpStatus.ACCEPTED);
    }
}
