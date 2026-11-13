package com.storeit.mapper;

import com.storeit.dto.FileDto;
import com.storeit.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMapper {

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "ownerName", ignore = true)
    @Mapping(target = "ownerEmail", ignore = true)
    @Mapping(target = "storageBackend", ignore = true)
    FileDto toDto(FileMetadata fileMetadata);
}
