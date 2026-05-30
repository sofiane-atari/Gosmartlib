package edu.ap.bib_backend.seed.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev-seed")
@Profile("!prod")
public class DevSeedController {

    private final DevSeedService devSeedService;

    public DevSeedController(DevSeedService devSeedService) {
        this.devSeedService = devSeedService;
    }

    @PostMapping
    public ResponseEntity<DevSeedResult> seed(Authentication authentication) {
        DevSeedResult result = devSeedService.seed(authentication);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/clear")
    public ResponseEntity<Void> clear(Authentication authentication) {
        devSeedService.clear(authentication);
        return ResponseEntity.ok().build();
    }
}