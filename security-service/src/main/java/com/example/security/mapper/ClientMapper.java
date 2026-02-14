package com.rapidalert.security.mapper;

import com.rapidalert.security.dto.request.SecurityRequest;
import com.rapidalert.security.entity.Client;
import com.rapidalert.security.model.Role;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(
        componentModel = "spring",
        imports = {
                Role.class
        }
)
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", expression = "java(Role.USER)")
    @Mapping(target = "password", expression = "java(encoder.encode(request.password()))")
    Client mapToEntity(SecurityRequest request, @Context PasswordEncoder encoder);
}
