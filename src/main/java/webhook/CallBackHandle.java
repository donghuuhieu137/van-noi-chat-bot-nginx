package webhook;
import static com.github.messenger4j.Messenger.CHALLENGE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.MODE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.SIGNATURE_HEADER_NAME;
import static com.github.messenger4j.Messenger.VERIFY_TOKEN_REQUEST_PARAM_NAME;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.common.WebviewHeightRatio;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.CallButton;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@RestController
@CrossOrigin("*")
@RequestMapping("/webhook")
public class CallBackHandle {
    private static final Logger logger = LoggerFactory.getLogger(CallBackHandle.class);

    private Messenger messenger;

	private String o;

    public CallBackHandle(Messenger messenger) {
        this.messenger = messenger;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebHook(@RequestParam(MODE_REQUEST_PARAM_NAME) final String mode,
                                                @RequestParam(VERIFY_TOKEN_REQUEST_PARAM_NAME) final String verifyToken,
                                                @RequestParam(CHALLENGE_REQUEST_PARAM_NAME) final String challenge){
        logger.debug("Received Webhook verification request - mode: {} | verifyToken: {} | challenge: {}", mode, verifyToken, challenge);
        try {
            this.messenger.verifyWebhook(mode, verifyToken);
            return ResponseEntity.ok(challenge);
        } catch (MessengerVerificationException e) {
            logger.warn("Webhook verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Void> sendMessenger(@RequestBody final String payload, @RequestHeader(SIGNATURE_HEADER_NAME) String signature) throws MessengerVerificationException{
    	
		this.messenger.onReceiveEvents(payload, Optional.of(signature), event -> {
		    try {
		    	String senderId = event.senderId();
		    	if (event.isTextMessageEvent()) {
		    		if(event.asTextMessageEvent().text().equalsIgnoreCase("Bắt đầu")) {
				    	sendTextMessage(senderId, "started");
		    		}
		    		else
		    			sendTextMessage(senderId, event.asTextMessageEvent().text());
			    	sendButtonMessage(senderId);
	    			sendQuickReplyMessage(senderId);
			    }
			    else if(event.isAttachmentMessageEvent()) {
			    	sendAttachmentMessage(event.asAttachmentMessageEvent());
			    }
			    else if(event.isQuickReplyMessageEvent()) {
			    	sendQuickReplyMessage(event.asQuickReplyMessageEvent());
			    }
			    else if(event.isPostbackEvent()) {
			    	String text = event.asPostbackEvent().payload().get().toString();
			    	if(text.equalsIgnoreCase("Bắt đầu")==true)
			    		sendTextMessage(senderId, "started");
			    	sendTextMessage(senderId, text);
			    }
			    else {
			    	handleException(senderId, "ERROR!!");
			    }
			} catch (MessengerApiException | MessengerIOException | MalformedURLException e) {
				logger.debug(e.getMessage());
				e.printStackTrace();
			}
		});
		
        return ResponseEntity.status(HttpStatus.OK).build();
    }

	private void sendAttachmentMessage(AttachmentMessageEvent event) {
		try {
			final String senderId = event.senderId();
			
			for (Attachment attachment : event.attachments()) {
				if(attachment.isRichMediaAttachment()) {
					final RichMediaAttachment richMediaAttachment = attachment.asRichMediaAttachment();
					final RichMediaAttachment.Type type = richMediaAttachment.type();
					sendTextMessage(senderId, type.toString());
					final URL url = richMediaAttachment.url();
					if(type.toString() == "IMAGE")
						sendMediaMessage(senderId, Type.IMAGE , url);
					else if(type.toString() == "VIDEO")
						sendMediaMessage(senderId, Type.VIDEO , url);
					else if(type.toString() == "FILE")
						sendMediaMessage(senderId, Type.FILE , url);
					else if(type.toString() == "AUDIO")
						sendMediaMessage(senderId, Type.AUDIO , url);
				}
			}
		} catch (Exception e) {
			logger.debug(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void sendTextMessage(String recipientId, String text) {
		try {
			final IdRecipient idRecipient = IdRecipient.create(recipientId);
			
			final TextMessage textMessage = TextMessage.create(text);
			final MessagePayload messagePayload = MessagePayload.create(idRecipient, MessagingType.RESPONSE, textMessage);
			this.messenger.send(messagePayload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.debug(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
//	private void sendUserDetails(String recipientId) throws MessengerApiException, MessengerIOException {
//        final UserProfile userProfile = this.messenger.queryUserProfile(recipientId);
//        sendTextMessage(recipientId, String.format("Your name is %s and you are %s", userProfile.firstName(), userProfile.gender()));
//        logger.info("User Profile Picture: {}", userProfile.profilePicture());
//    }

    private void sendMediaMessage(String recipientId, Type type, URL url) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(type, new URL(url.toString()));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendRichMediaMessage(String recipientId, UrlRichMediaAsset richMediaAsset) throws MessengerApiException, MessengerIOException {
        final RichMediaMessage richMediaMessage = RichMediaMessage.create(richMediaAsset);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, richMediaMessage);
        this.messenger.send(messagePayload);
    }

	
	private void handleException(String senderId, String text) throws MessengerIOException, MessengerApiException{
		final TextMessage textMessage = TextMessage.create(text);
		final MessagePayload payload = MessagePayload.create(senderId,MessagingType.RESPONSE, textMessage);
		this.messenger.send(payload);
	}
	
	private void sendQuickReplyMessage(QuickReplyMessageEvent event) {
		String text = event.payload().toString();
		sendTextMessage(event.senderId(),text);
		if(text.equalsIgnoreCase("Đang tìm đối phương nam . . .")==true)
			sendTextMessage(event.senderId(),"Nam");
		else if (text.equalsIgnoreCase("Đang tìm đối phương nữ . . .")==true)
			sendTextMessage(event.senderId(),"Nữ");
	}	
	
	private void sendQuickReplyMessage(String recipientId) throws MessengerApiException, MessengerIOException {
        List<QuickReply> quickReplies = new ArrayList<>();

        quickReplies.add(TextQuickReply.create("Nam", "Đang tìm đối phương nam . . ."));
        quickReplies.add(TextQuickReply.create("Nữ", "Đang tìm đối phương nữ . . ."));

        TextMessage message = TextMessage.create("Bạn muốn tìm kiếm đối phương là nam hay nữ ?", Optional.of(quickReplies), Optional.empty());
        this.messenger.send(MessagePayload.create(recipientId, MessagingType.RESPONSE, message));
    }
	
	private void sendButtonMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final List<Button> buttons = Arrays.asList(
        		PostbackButton.create("Bắt đầu","GET START"),
                UrlButton.create("Fanpage", new URL("https://www.facebook.com/Vân-Nội-Chatbot-102546638613653/"), Optional.of(WebviewHeightRatio.COMPACT), Optional.of(false), Optional.empty(), Optional.empty())
                
        );

        final ButtonTemplate buttonTemplate = ButtonTemplate.create("Chat với người lạ\r\n" + 
        		"Click \"Bắt đầu\" để chat với người lạ, gõ #pp để kết thúc cuộc trò chuyện", buttons);
        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }
}
