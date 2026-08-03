package com.family.Bodeguita.household.mapper;

import com.family.Bodeguita.household.domain.Invitation;
import com.family.Bodeguita.household.dto.InvitationResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InvitationMapper {

    /** No expone {@code invitedBy} ni {@code household}: son relaciones LAZY y nadie las pide. */
    public InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }

    public List<InvitationResponse> toResponses(List<Invitation> invitations) {
        return invitations.stream().map(this::toResponse).toList();
    }
}
