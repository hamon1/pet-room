package com.petroom.medical;

import com.petroom.user.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets/{petId}/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService service;

    public MedicalRecordController(MedicalRecordService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicalRecordResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId) {
        return service.list(CurrentUser.id(jwt), petId);
    }

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                                        @Valid @RequestBody MedicalRecordRequest request) {
        MedicalRecordResponse created = service.create(CurrentUser.id(jwt), petId, request);
        return ResponseEntity.created(URI.create("/api/pets/" + petId + "/medical-records/" + created.id()))
                .body(created);
    }

    @PutMapping("/{recordId}")
    public MedicalRecordResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                        @PathVariable Long recordId, @Valid @RequestBody MedicalRecordRequest request) {
        return service.update(CurrentUser.id(jwt), petId, recordId, request);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                       @PathVariable Long recordId) {
        service.delete(CurrentUser.id(jwt), petId, recordId);
        return ResponseEntity.noContent().build();
    }
}
