package com.rapidalert.recipient.mapper;

import com.rapidalert.recipient.dto.request.RecipientRequest;
import com.rapidalert.recipient.dto.response.RecipientResponse;
import com.rapidalert.recipient.entity.Recipient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecipientMapper extends EntityMapper<Recipient, RecipientRequest, RecipientResponse> {

}
