package io.github.eruden24.fairshare.mapper;

import io.github.eruden24.fairshare.dto.user.UserResponse;
import io.github.eruden24.fairshare.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponse(List<User> users);
}
