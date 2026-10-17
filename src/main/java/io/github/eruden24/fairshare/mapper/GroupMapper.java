package io.github.eruden24.fairshare.mapper;

import io.github.eruden24.fairshare.dto.group.GroupResponse;
import io.github.eruden24.fairshare.dto.group.MemberResponse;
import io.github.eruden24.fairshare.entity.ExpenseGroup;
import io.github.eruden24.fairshare.entity.GroupMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(target="createdById", source="createdBy.id")
    GroupResponse toResponse(ExpenseGroup group);

    List<GroupResponse> toResponse(List<ExpenseGroup> groups);

    @Mapping(target="userId", source="user.id")
    @Mapping(target="displayName", source="user.displayName")
    MemberResponse toMemberResponse(GroupMember member);

    List<MemberResponse> toMemberResponse(List<GroupMember> members);
}
