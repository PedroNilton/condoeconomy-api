package com.condoeconomy.api.presentation.controller.push;

import com.condoeconomy.api.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPushSubscriptionRepository;
import com.condoeconomy.api.presentation.dto.push.PushSubscriptionRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final SpringDataPushSubscriptionRepository repository;

    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<Void> subscribe(Authentication auth, @RequestBody PushSubscriptionRequestDTO dto) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        
        // Remove old if exists
        repository.deleteByEndpoint(dto.getEndpoint());
        
        PushSubscriptionJpaEntity sub = new PushSubscriptionJpaEntity();
        sub.setUsuarioId(user.getId());
        sub.setEndpoint(dto.getEndpoint());
        sub.setP256dh(dto.getKeys().getP256dh());
        sub.setAuth(dto.getKeys().getAuth());
        
        repository.save(sub);
        return ResponseEntity.ok().build();
    }
}
