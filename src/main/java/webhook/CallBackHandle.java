package webhook;
import static com.github.messenger4j.Messenger.CHALLENGE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.MODE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.SIGNATURE_HEADER_NAME;
import static com.github.messenger4j.Messenger.VERIFY_TOKEN_REQUEST_PARAM_NAME;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
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
import java.util.Optional;


@RestController
@CrossOrigin("*")
@RequestMapping("/webhook")
public class CallBackHandle {
    private static final Logger logger = LoggerFactory.getLogger(CallBackHandle.class);

    private Messenger messenger;

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
			    	sendTextMessage(senderId,"Hi");
			    	sendTextMessage(senderId, senderId.toString());
			    }
			    else if(event.isAttachmentMessageEvent()) {
			    	sendAttachmentMessage(event.asAttachmentMessageEvent());
			    }
			    else {
			    	handleException(senderId, "Hank only can send text message !!");
			    }
			} catch (MessengerApiException | MessengerIOException e) {
				logger.debug(e.getMessage());
				e.printStackTrace();
			}
		});
		
        return ResponseEntity.status(HttpStatus.OK).build();
    }

	private void sendAttachmentMessage(AttachmentMessageEvent event) {
		try {
			final String senderId = event.senderId();
			final IdRecipient recipientId = IdRecipient.create(senderId);
			
			for (Attachment attachment : event.attachments()) {
				if(attachment.isRichMediaAttachment()) {
					final RichMediaAttachment richMediaAttachment = attachment.asRichMediaAttachment();
					final RichMediaAttachment.Type type = richMediaAttachment.type();
					sendTextMessage(recipientId.toString(), type.toString());
					final URL url = richMediaAttachment.url();
					
					sendMediaMessage(recipientId.toString(), Type.IMAGE, url);
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
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(Type.IMAGE, new URL(url.toString()));
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

}
