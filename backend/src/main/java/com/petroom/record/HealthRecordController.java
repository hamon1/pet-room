package com.petroom.record;

import com.petroom.user.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthRecordController {

    private final HealthRecordService service;

    public HealthRecordController(HealthRecordService service) {
        this.service = service;
    }

    @GetMapping("/api/pets/{petId}/health-records")
    public List<HealthRecordResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(CurrentUser.id(jwt), petId, from, to);
    }

    @PutMapping("/api/pets/{petId}/health-records/{date}")
    public HealthRecordResponse put(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @Valid @RequestBody HealthRecordRequest request) {
        return service.put(CurrentUser.id(jwt), petId, date, request);
    }

    @DeleteMapping("/api/pets/{petId}/health-records/{date}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.delete(CurrentUser.id(jwt), petId, date);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/health-records/batch")
    public List<HealthRecordResponse> batch(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody BatchHealthRequest request) {
        return service.batch(CurrentUser.id(jwt), request);
    }
}
