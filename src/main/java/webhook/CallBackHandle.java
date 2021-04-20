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
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.webhook.event.TextMessageEvent;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
		    if (event.isTextMessageEvent()) {
		        try {
					handleMessage(event.asTextMessageEvent());
				} catch (MessengerIOException | MessengerApiException e) {
					logger.debug(e.getMessage());
					e.printStackTrace();
				}
		    }
		    else {
		    	String senderId = event.senderId();
		    	sendTextMessageUser(senderId, "Hank only can send text message !!");
		    }
		});
		
        return ResponseEntity.status(HttpStatus.OK).build();
    }

	private void sendTextMessageUser(String senderId, String text) {
		try {
			final IdRecipient idRecipient = IdRecipient.create(senderId);
			final NotificationType notificationType = NotificationType.REGULAR;
			final String metadata = "DEVELOPER_DEFINED_METADATA";
			
			final TextMessage textMessage = TextMessage.create(text, Optional.empty(), Optional.of(metadata));
			final MessagePayload messagePayload = MessagePayload.create(idRecipient, MessagingType.RESPONSE, textMessage, Optional.of(notificationType), Optional.empty());
			this.messenger.send(messagePayload);
		} catch (MessengerApiException | MessengerIOException e) {
			logger.debug(e.getMessage());
			e.printStackTrace();
		}
		
	}

	private void handleMessage(TextMessageEvent event) throws MessengerIOException, MessengerApiException{
		final String senderId = event.senderId();
		sendTextMessageUser(senderId,"Hello, I'm Hank");
	}

}
