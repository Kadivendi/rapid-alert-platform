package com.rapidalert.template.integration;

import com.rapidalert.template.ITBase;
import com.rapidalert.template.builder.TemplateJson;
import com.rapidalert.template.client.RecipientClient;
import com.rapidalert.template.mocks.RecipientClientMock;
import com.rapidalert.template.service.MessageSourceService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.rapidalert.template.enums.Url.*;
import static com.rapidalert.template.integration.TemplateControllerIT.CLIENT_ID;
import static com.rapidalert.template.integration.TemplateControllerIT.TEMPLATE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
public class TemplateHistoryControllerIT extends ITBase {

    private final MockMvc mockMvc;
    private final MessageSourceService message;

    @MockBean
    private final RecipientClient recipientClient;

    @BeforeEach
    void setUp() {
        RecipientClientMock.setupMockRecipientClient(recipientClient);
    }

    @Test
    public void createTemplateHistoryTest() throws Exception {
        createTemplateHistoryNotFound(1L);
        Long templateId = createTemplateCreated(TEMPLATE);
        createTemplateHistoryCreated(templateId, TEMPLATE);
        createTemplateHistoryCreated(templateId, TEMPLATE);
    }

    @Test
    public void getTemplateHistoryTest() throws Exception {
        getTemplateHistoryNotFound(1L);
        Long templateId = createTemplateCreated(TEMPLATE);
        createTemplateHistoryCreated(templateId, TEMPLATE);
        getTemplateHistoryOk(templateId, TEMPLATE);
        getTemplateHistoryOk(templateId, TEMPLATE);
    }

    private void createTemplateHistoryCreated(Long templateId, TemplateJson template) throws Exception {
        mockMvc.perform(post(HISTORY_CREATE.getUrl().formatted(templateId))
                        .header("clientId", CLIENT_ID))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").exists(),
                        jsonPath("$.title").value(template.title()),
                        jsonPath("$.content").value(template.content()),
                        jsonPath("$.imageUrl").isEmpty()
                );
    }

    private void createTemplateHistoryNotFound(Long templateId) throws Exception {
        mockMvc.perform(post(HISTORY_CREATE.getUrl().formatted(templateId))
                        .header("clientId", CLIENT_ID))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value(
                                message.getProperty("template.not_found", templateId, CLIENT_ID)
                        )
                );
    }

    private void getTemplateHistoryOk(Long templateId, TemplateJson template) throws Exception {
        mockMvc.perform(get(HISTORY_GET.getUrl().formatted(templateId))
                        .header("clientId", CLIENT_ID))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").exists(),
                        jsonPath("$.title").value(template.title()),
                        jsonPath("$.content").value(template.content()),
                        jsonPath("$.imageUrl").isEmpty()
                );
    }

    private void getTemplateHistoryNotFound(Long historyId) throws Exception {
        mockMvc.perform(get(HISTORY_GET.getUrl().formatted(historyId))
                        .header("clientId", CLIENT_ID))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value(
                                message.getProperty("history.not_found", historyId, CLIENT_ID)
                        )
                );
    }

    private Long createTemplateCreated(TemplateJson template) throws Exception {
        ResultActions result = mockMvc.perform(post(CREATE.getUrl())
                        .header("clientId", CLIENT_ID)
                        .content(template.toJson())
                        .contentType(APPLICATION_JSON))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").exists(),
                        jsonPath("$.title").value(template.title()),
                        jsonPath("$.content").value(template.content()),
                        jsonPath("$.imageUrl").isEmpty(),
                        jsonPath("$.recipientIds").isEmpty()
                );
        return Long.valueOf(extractJsonValueByKey(result, "id"));
    }
}
