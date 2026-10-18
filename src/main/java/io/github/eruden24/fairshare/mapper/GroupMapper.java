package io.github.eruden24.fairshare.mapper;

import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.dto.group.MemberResponse;
import io.github.eruden24.fairshare.entity.Group;
import io.github.eruden24.fairshare.entity.UserGroup;
import io.github.eruden24.fairshare.enumeration.GroupRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(target="id", source="group.id")
    @Mapping(target="name", source="group.name")
    @Mapping(target="currency", source="group.currency")
    @Mapping(target="createdById", source="group.createdBy.id")
    @Mapping(target="createdAt", source="group.createdAt")
    @Mapping(target="memberCount", source="memberCount")
    @Mapping(target="myRole", source="myRole")
    GroupResponse toResponse(Group group, long memberCount, GroupRole myRole);

    @Mapping(target="userId", source="user.id")
    @Mapping(target="displayName", source="user.displayName")
    MemberResponse toMemberResponse(UserGroup member);

    List<MemberResponse> toMemberResponse(List<UserGroup> members);
}
