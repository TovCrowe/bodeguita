package com.family.Bodeguita.user.mapper;

import com.family.Bodeguita.user.domain.User;
import com.family.Bodeguita.user.dto.UserResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.getHousehold() == null ? null : user.getHousehold().getId(),
                user.getRole(),
                user.getCreatedAt());
    }

    public List<UserResponse> toResponses(List<User> users) {
        return users.stream().map(this::toResponse).toList();
    }
}
