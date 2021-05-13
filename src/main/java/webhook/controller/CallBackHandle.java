package webhook.controller;
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
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.LocationQuickReply;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.ListTemplate;
import com.github.messenger4j.send.message.template.ReceiptTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.LogInButton;
import com.github.messenger4j.send.message.template.button.LogOutButton;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.message.template.common.Element;
import com.github.messenger4j.send.message.template.receipt.Address;
import com.github.messenger4j.send.message.template.receipt.Adjustment;
import com.github.messenger4j.send.message.template.receipt.Item;
import com.github.messenger4j.send.message.template.receipt.Summary;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.send.senderaction.SenderAction;
import com.github.messenger4j.webhook.Event;
import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
import com.github.messenger4j.webhook.event.MessageDeliveredEvent;
import com.github.messenger4j.webhook.event.MessageEchoEvent;
import com.github.messenger4j.webhook.event.MessageReadEvent;
import com.github.messenger4j.webhook.event.OptInEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.LocationAttachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;

import webhook.entity.User;
import webhook.service.WebhookService;
import webhook.service.database.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@RestController
@CrossOrigin("*")
@RequestMapping("/webhook")
public class CallBackHandle {
	
	private static final String RESOURCE_URL = "https://raw.githubusercontent.com/fbsamples/messenger-platform-samples/master/node/public";
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private WebhookService webhookService;
	
    private static final Logger logger = LoggerFactory.getLogger(CallBackHandle.class);

    @Autowired
    public Messenger messenger;

    @Autowired
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
//			if (event.isTextMessageEvent())
//	    	{
//			 handleTextMessageEvent(event.asTextMessageEvent());
//	    	}
//			else if(event.isQuickReplyMessageEvent()) {
//				handleQuickReplyMessageEventF(event.asQuickReplyMessageEvent());
//			}
			
		    try {
		    	if(userService.findUser(event.senderId()).isEmpty()) {
		    		System.out.println("new user");
		    		userService.newUser(event.senderId());
		    		webhookService.sendTextMessage(event.senderId(), "Hello there");
		    		userService.sendSettingGender(event.senderId());
		    		webhookService.sendTextMessage(event.senderId(), "Cài đặt giới tính thành công !!\nBạn đã sẵn sàng sử dụng chatbot\nChat /hd để được hướng dẫn ");
		    	}
		    	if (event.isTextMessageEvent())
		    	{
		    		System.out.println(event.asTextMessageEvent().text());
				    webhookService.receivedTextMessage(event.asTextMessageEvent());
		    	}
			    else if(event.isAttachmentMessageEvent())
			    	webhookService.receivedAttachmentMessage(event.asAttachmentMessageEvent());
			    else if(event.isQuickReplyMessageEvent())
			    	webhookService.receivedQuickReplyMessage(event.asQuickReplyMessageEvent());
			    else if(event.isPostbackEvent())
			    	webhookService.receivedPostBackMessage(event.asPostbackEvent());
			    else {
			    	handleException(event.senderId(), "ERROR!!");
			    }
			} catch (MessengerApiException | MessengerIOException | MalformedURLException e) {
				logger.debug(e.getMessage());
				e.printStackTrace();
			}
		});
		
        return ResponseEntity.status(HttpStatus.OK).build();
    }

	private void handleException(String senderId, String text) throws MessengerIOException, MessengerApiException{
		final TextMessage textMessage = TextMessage.create(text);
		final MessagePayload payload = MessagePayload.create(senderId,MessagingType.RESPONSE, textMessage);
		this.messenger.send(payload);
	}

	private void sendButtonMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final List<Button> buttons = Arrays.asList(
        		PostbackButton.create("Bắt đầu","GET_START"),
                UrlButton.create("Fanpage", new URL("https://www.facebook.com/Vân-Nội-Chatbot-102546638613653/"), Optional.of(WebviewHeightRatio.COMPACT), Optional.of(false), Optional.empty(), Optional.empty()),
                PostbackButton.create("Thông tin thêm","ABOUT_US")
        );

        final ButtonTemplate buttonTemplate = ButtonTemplate.create("Chat với người lạ\r\n" + 
        		"Click \"Bắt đầu\" để chat với người lạ, gõ /hd để được hướng dẫn", buttons);
        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }
	
	// facebook
	private void handleTextMessageEvent(TextMessageEvent event) {
        logger.debug("Received TextMessageEvent: {}", event);

        final String messageId = event.messageId();
        final String messageText = event.text();
        final String senderId = event.senderId();
        final Instant timestamp = event.timestamp();

        logger.info("Received message '{}' with text '{}' from user '{}' at '{}'", messageId, messageText, senderId, timestamp);

        try {
            switch (messageText.toLowerCase()) {
            	case "/help":
            		sendTextMessage(senderId, "Chat với người lạ\n/find: tìm người lạ\n/stop: dừng tìm kiếm\n/end: kết thúc cuộc trò chuyện");
            	break;
            	case "/find":
            		sendTextMessage(senderId, "Bắt đầu tìm kiếm người lạ");
            		sendChooseMessageF(senderId);
                    break;
            	case "/stop":
            		sendTextMessage(senderId, "Dừng tìm kiếm người lạ");
                    break;
            	case "/end":
            		sendTextMessage(senderId, "Kết thúc cuộc trò chuyện");
                    break;
                case "generic":
                    sendGenericMessage(senderId);
                    break;
                case "receipt":
                    sendReceiptMessage(senderId);
                    break;

                default:
                    sendTextMessage(senderId, "Chat /help để được hướng dẫn");
            }
        } catch (MessengerApiException | MessengerIOException | MalformedURLException e) {
            
        }  
    }
	private void sendTextMessage(String recipientId, String text) {
        try {
            final IdRecipient recipient = IdRecipient.create(recipientId);
            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "DEVELOPER_DEFINED_METADATA";

            final TextMessage textMessage = TextMessage.create(text, Optional.empty(), Optional.of(metadata));
            final MessagePayload messagePayload = MessagePayload.create(recipient, MessagingType.RESPONSE, textMessage,
                    Optional.of(notificationType), Optional.empty());
            this.messenger.send(messagePayload);
        } catch (MessengerApiException | MessengerIOException e) {
            
        }
    }


    private void sendGenericMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        List<Button> riftButtons = new ArrayList<>();
        riftButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/")));
        riftButtons.add(PostbackButton.create("Call Postback", "Payload for first bubble"));

        List<Button> touchButtons = new ArrayList<>();
        touchButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/touch/")));
        touchButtons.add(PostbackButton.create("Call Postback", "Payload for second bubble"));

        final List<Element> elements = new ArrayList<>();

        elements.add(
                Element.create("rift", Optional.of("Next-generation virtual reality"), Optional.of(new URL("https://www.oculus.com/en-us/rift/")), Optional.empty(), Optional.of(riftButtons)));
        elements.add(Element.create("touch", Optional.of("Your Hands, Now in VR"), Optional.of(new URL("https://www.oculus.com/en-us/touch/")), Optional.empty(), Optional.of(touchButtons)));

        final GenericTemplate genericTemplate = GenericTemplate.create(elements);
        final TemplateMessage templateMessage = TemplateMessage.create(genericTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void sendReceiptMessage(String recipientId) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final String uniqueReceiptId = "order-" + Math.floor(Math.random() * 1000);

        final List<Item> items = new ArrayList<>();

        items.add(Item.create("Oculus Rift", 599.00f, Optional.of("Includes: headset, sensor, remote"), Optional.of(1), Optional.of("USD"),
        		Optional.of(new URL(RESOURCE_URL + "/assets/riftsq.png"))));
        items.add(Item.create("Samsung Gear VR", 99.99f, Optional.of("Frost White"), Optional.of(1), Optional.of("USD"), Optional.of(new URL(RESOURCE_URL + "/assets/gearvrsq.png"))));

        final ReceiptTemplate receiptTemplate = ReceiptTemplate
                .create("Peter Chang", uniqueReceiptId, "Visa 1234", "USD", Summary.create(626.66f, Optional.of(698.99f), Optional.of(57.67f), Optional.of(20.00f)),
                		Optional.of(Address.create("1 Hacker Way", "Menlo Park", "94025", "CA", "US")), Optional.of(items),
                		Optional.of(Arrays.asList(Adjustment.create("New Customer Discount", -50f), Adjustment.create("$100 Off Coupon", -100f))),
                		Optional.of("The Boring Company"), Optional.of(new URL("https://www.boringcompany.com/")), Optional.of(true), Optional.of(Instant.ofEpochMilli(1428444852L)));

        final TemplateMessage templateMessage = TemplateMessage.create(receiptTemplate);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }
    private void sendChooseMessageF(String recipientId) throws MessengerApiException, MessengerIOException {
		System.out.println("sendChooseMessage");
        List<QuickReply> quickReplies = new ArrayList<>();

        quickReplies.add(TextQuickReply.create("Nam", "male"));
        quickReplies.add(TextQuickReply.create("Nữ", "female"));

        TextMessage message = TextMessage.create("Bạn muốn tìm kiếm đối phương nam hay nữ ?", Optional.of(quickReplies), Optional.empty());
        this.messenger.send(MessagePayload.create(recipientId, MessagingType.RESPONSE, message));
    }
    public void handleQuickReplyMessageEventF(QuickReplyMessageEvent event) {
		System.out.println("receivedQuickReplyMessage");
		String text = event.payload().toString();
		if(text.equalsIgnoreCase("male")==true)
			sendTextMessage(event.senderId(),"Đang tìm đối phương nam . . .");
		else if (text.equalsIgnoreCase("female")==true)
			sendTextMessage(event.senderId(),"Đang tìm đối phương nữ . . .");
	}
}
