package com.rapidalert.shortener.mapper;

import com.rapidalert.shortener.entity.Response;
import com.rapidalert.shortener.dto.request.NotificationOptionsRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResponseMapper {

    Response mapToResponse(NotificationOptionsRequest request);
}
