package com.petroom.tank;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets/{petId}/fish")
public class TankFishController {

    private final TankFishService service;

    public TankFishController(TankFishService service) {
        this.service = service;
    }

    @GetMapping
    public List<TankFishResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId) {
        return service.list(CurrentUser.id(jwt), petId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TankFishResponse add(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                @Valid @RequestBody TankFishRequest request) {
        return service.add(CurrentUser.id(jwt), petId, request);
    }

    @DeleteMapping("/{fishId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long petId,
                                       @PathVariable Long fishId) {
        service.remove(CurrentUser.id(jwt), petId, fishId);
        return ResponseEntity.noContent().build();
    }
}
