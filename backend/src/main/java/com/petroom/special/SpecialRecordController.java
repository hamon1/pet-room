package com.petroom.special;

import com.petroom.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets/{petId}")
public class SpecialRecordController {

    private final SpecialRecordService service;

    public SpecialRecordController(SpecialRecordService service) {
        this.service = service;
    }

    @GetMapping("/special-items")
    public List<SpecialItemResponse> items(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId) {
        return service.items(CurrentUser.id(jwt), petId);
    }

    @GetMapping("/special-records")
    public List<SpecialRecordResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                            @RequestParam(required = false) SpecialItem item) {
        return service.list(CurrentUser.id(jwt), petId, item);
    }

    @PostMapping("/special-records")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialRecordResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                        @Valid @RequestBody SpecialRecordRequest request) {
        return service.create(CurrentUser.id(jwt), petId, request);
    }

    @DeleteMapping("/special-records/{recordId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                       @PathVariable Long recordId) {
        service.delete(CurrentUser.id(jwt), petId, recordId);
        return ResponseEntity.noContent().build();
    }
}
