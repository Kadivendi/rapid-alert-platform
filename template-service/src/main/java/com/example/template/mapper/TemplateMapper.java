package com.rapidalert.template.mapper;

import com.rapidalert.template.client.RecipientClient;
import com.rapidalert.template.dto.request.TemplateRequest;
import com.rapidalert.template.dto.response.TemplateHistoryResponse;
import com.rapidalert.template.dto.response.TemplateResponse;
import com.rapidalert.template.entity.Template;
import com.rapidalert.template.entity.TemplateHistory;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TemplateMapper extends EntityMapper<Template, TemplateRequest, TemplateResponse> {

    TemplateHistory mapToTemplateHistory(Template template);

    TemplateHistoryResponse mapToTemplateHistoryResponse(TemplateHistory templateHistory);

    @Mapping(
            target = "recipientIds",
            expression = "java(recipientClient.receiveRecipientResponseListByTemplateId(template.getClientId(), template.getId()).getBody())"

    )
    TemplateResponse mapToResponse(Template template, @Context RecipientClient recipientClient);
}
