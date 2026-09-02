package com.workhive.module.user.controller;

import com.workhive.module.auth.dto.AuthDtos.AuthResponse;
import com.workhive.module.user.dto.UserDtos.*;
import com.workhive.module.user.entity.Invitation;
import com.workhive.module.user.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Page<Invitation>> getInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(invitationService.getInvitations(PageRequest.of(page, size)));
    }

    @PostMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<InviteUserResponse> inviteUser(@Valid @RequestBody InviteUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.inviteUser(request));
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<InvitationDetailsResponse> getInvitationDetails(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.getInvitationDetails(token));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<InviteUserResponse> resendInvitation(@PathVariable UUID id) {
        return ResponseEntity.ok(invitationService.retryInvitationEmail(id));
    }

    @PostMapping("/accept")
    public ResponseEntity<AuthResponse> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        return ResponseEntity.ok(invitationService.acceptInvitation(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, String>> revokeInvitation(@PathVariable UUID id) {
        invitationService.revokeInvitation(id);
        return ResponseEntity.ok(Map.of("message", "Invitation revoked"));
    }
}
