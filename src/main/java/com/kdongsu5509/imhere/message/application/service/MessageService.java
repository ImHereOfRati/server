package com.kdongsu5509.imhere.message.application.service;

import com.kdongsu5509.imhere.message.adapter.dto.MessageSendRequest;
import com.kdongsu5509.imhere.message.adapter.dto.MultipleMessageSendRequest;
import com.kdongsu5509.imhere.message.application.port.MultipleMessageSendUseCasePort;
import com.kdongsu5509.imhere.message.application.port.SingleMessageSendUseCasePort;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableConfigurationProperties(SolapiProperties.class)
public class MessageService implements SingleMessageSendUseCasePort, MultipleMessageSendUseCasePort {

    private final SolapiProperties solapiProperties;
    private final DefaultMessageService solapiMessageService;

    public MessageService(SolapiProperties solapiProperties) {
        this.solapiProperties = solapiProperties;

        this.solapiMessageService = SolapiClient.INSTANCE.createInstance(
                solapiProperties.getApiKey(),
                solapiProperties.getApiSecret()
        );
    }

    @Override
    public void send(MessageSendRequest messageSendRequest, String email) {
        Message solapiMessage = buildSolapiMessage(messageSendRequest, email);
        sendMessageViaSolapi(solapiMessage);
    }

    @Override
    public void send(MultipleMessageSendRequest multipleMessageSendRequest, String email) {
        List<Message> solapiMessageList = multipleMessageSendRequest.getRequests().stream()
                .map(request -> buildSolapiMessage(request, email))
                .collect(Collectors.toList());

        sendMessageListViaSolapi(solapiMessageList);
    }

    private Message buildSolapiMessage(MessageSendRequest msg, String email) {
        Message solapiMessage = new Message();
        solapiMessage.setFrom(solapiProperties.getSender());
        solapiMessage.setTo(msg.getReceiverNumber());

        String messageText = String.format(
                "[ImHere, 위치 기반 문자 알림 서비스]\n" +
                        "---\n" +
                        "보낸 사람: %s\n" +
                        "내용:\n" +
                        "%s",
                email, msg.getMessage()
        );

        solapiMessage.setText(messageText);
        return solapiMessage;
    }

    private void sendMessageViaSolapi(Message msg) {
        ArrayList<Message> messageList = new ArrayList<>();
        messageList.add(msg);
        sendMessageListViaSolapi(messageList);
    }

    private void sendMessageListViaSolapi(List<Message> messageList) {
        try {
            solapiMessageService.send(messageList);
        } catch (SolapiMessageNotReceivedException exception) {
            log.info("전송 실패 메시지 목록: {}", exception.getFailedMessageList());
            log.info("예외 메시지: {}", exception.getMessage());
        } catch (Exception exception) {
            log.info("일반 예외 메시지: {}", exception.getMessage());
        }
    }
}