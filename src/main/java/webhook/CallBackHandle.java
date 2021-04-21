package webhook;
import static com.github.messenger4j.Messenger.CHALLENGE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.MODE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.SIGNATURE_HEADER_NAME;
import static com.github.messenger4j.Messenger.VERIFY_TOKEN_REQUEST_PARAM_NAME;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerApiExceptionFactory;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import com.github.messenger4j.webhook.event.attachment.Attachment;

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
		    	if (event.isTextMessageEvent()) {
			    	sendTextMessage(event.asTextMessageEvent());
			    }
			    else if(event.isAttachmentMessageEvent()) {
			    	sendAttachmentMessage(event.asAttachmentMessageEvent());
			    }
			    else {
			    	String senderId = event.senderId();
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
		final String senderId = event.senderId();
		final IdRecipient idRecipient = IdRecipient.create(senderId);
		final NotificationType notificationType = NotificationType.REGULAR;
		final String metadata = "DEVELOPER_DEFINED_METADATA";

	}

	private void sendTextMessage(TextMessageEvent event) {
		try {
			final String senderId = event.senderId();
			final UserProfile userProfile = messenger.queryUserProfile(senderId);
			final IdRecipient idRecipient = IdRecipient.create(senderId);
			final NotificationType notificationType = NotificationType.REGULAR;
			final String metadata = "DEVELOPER_DEFINED_METADATA";
			final String text = String.format("Your name is %s and you are %s and picture is %s", userProfile.firstName(), userProfile.gender(), userProfile.profilePicture());
			try {
				sendImageMessage(senderId);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			final TextMessage textMessage = TextMessage.create(text, Optional.empty(), Optional.of(metadata));
			final MessagePayload messagePayload = MessagePayload.create(idRecipient, MessagingType.RESPONSE, textMessage, Optional.of(notificationType), Optional.empty());
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

    private void sendImageMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(Type.IMAGE, new URL(messenger.queryUserProfile(recipientId).profilePicture()));
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
