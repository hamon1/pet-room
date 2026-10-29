package com.petroom.pet;

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
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @PostMapping
    public ResponseEntity<PetResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PetRequest request) {
        PetResponse pet = petService.create(CurrentUser.id(jwt), request);
        return ResponseEntity.created(URI.create("/api/pets/" + pet.id())).body(pet);
    }

    @GetMapping
    public List<PetResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return petService.list(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public PetResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return petService.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/{id}")
    public PetResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                              @Valid @RequestBody PetRequest request) {
        return petService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        petService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
