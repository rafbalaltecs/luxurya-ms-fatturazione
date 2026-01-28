package it.fatturazione.controller;

import it.fatturazione.dto.FatturaRequestDTO;
import it.fatturazione.dto.FatturaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
@Slf4j
public class ActuactorController {


    @GetMapping("/health")
    public ResponseEntity<Boolean> health() {
        return ResponseEntity.ok(true);
    }
}
