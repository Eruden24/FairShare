package io.github.eruden24.fairshare.mapper;

import io.github.eruden24.fairshare.dto.user.UserResponse;
import io.github.eruden24.fairshare.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
