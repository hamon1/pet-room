package com.petroom.food;

import com.petroom.user.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService service;

    public FoodController(FoodService service) {
        this.service = service;
    }

    @GetMapping
    public List<FoodResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(CurrentUser.id(jwt));
    }

    @PostMapping
    public ResponseEntity<FoodResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FoodRequest request) {
        FoodResponse food = service.create(CurrentUser.id(jwt), request);
        return ResponseEntity.created(URI.create("/api/foods/" + food.id())).body(food);
    }

    @PutMapping("/{id}")
    public FoodResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody FoodRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @PatchMapping("/{id}/remaining")
    public FoodResponse remaining(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody RemainingRequest request) {
        return service.updateRemaining(CurrentUser.id(jwt), id, request);
    }

    @PostMapping("/{id}/feed")
    public FoodResponse feed(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.feed(CurrentUser.id(jwt), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
