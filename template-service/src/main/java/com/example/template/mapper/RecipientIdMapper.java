package com.rapidalert.template.mapper;

import com.rapidalert.template.dto.kafka.TemplateRecipientKafka;
import com.rapidalert.template.entity.RecipientId;
import com.rapidalert.template.entity.Template;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        imports = {
                Template.class
        }
)
public interface RecipientIdMapper {

    @Mapping(target = "template", expression = "java(Template.builder().id(kafka.templateId()).build())")
    RecipientId mapToEntity(TemplateRecipientKafka kafka);
}
