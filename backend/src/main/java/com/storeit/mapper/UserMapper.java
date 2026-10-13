package com.storeit.mapper;

import com.storeit.dto.UserDto;
import com.storeit.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);
}
