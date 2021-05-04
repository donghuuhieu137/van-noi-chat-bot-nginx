package webhook.service;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;

import webhook.entity.User;
import webhook.service.database.SessionService;
import webhook.service.database.UserService;
import webhook.service.helper.PairService;

public class WebhookService {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private SessionService sessionService;
	
	private Messenger messenger;
	
	@Autowired
	private PairService pairService;
	
	public void receivedTextMessage(TextMessageEvent event) throws MessengerApiException, MessengerIOException {
		String text = event.text();
		System.out.println("receivedTextMessage");
		switch(text.toLowerCase()) {
			case "/find":
					pairService.recivedMatchReq(event.senderId());
				break;
			case "/end":
					pairService.recivedEndReq(event.senderId());
				break;
			default:
				User user = userService.findUser(event.senderId()).get(0);
				if(user.getStatus()=="MATCHED") {
					String partnerId = sessionService.findPartner(user.getId());
					sendTextMessage(partnerId, text);
				}
		}
			
	}
	
	public void sendTextMessage(String recipientId, String text) {
		try {
			System.out.println("sendTextMessage");
			final IdRecipient idRecipient = IdRecipient.create(recipientId);
			
			final TextMessage textMessage = TextMessage.create(text);
			final MessagePayload messagePayload = MessagePayload.create(idRecipient, MessagingType.RESPONSE, textMessage);
			this.messenger.send(messagePayload);
		} catch (MessengerApiException | MessengerIOException e) {
			e.printStackTrace();
		}
		
	}

	public void receivedQuickReplyMessage(QuickReplyMessageEvent event) {
		System.out.println("receivedQuickReplyMessage");
		String text = event.payload().toString();
		if(text.equalsIgnoreCase("male")==true)
			sendTextMessage(event.senderId(),"Đang tìm đối phương nam");
		else if (text.equalsIgnoreCase("female")==true)
			sendTextMessage(event.senderId(),"Đang tìm đối phương nữ . . .");
		pairService.matchUser(event);
	}

	public void receivedAttachmentMessage(AttachmentMessageEvent event) throws MalformedURLException, MessengerApiException, MessengerIOException {
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
		
	}
	private void sendMediaMessage(String recipientId, Type type, URL url) throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset = UrlRichMediaAsset.create(type, new URL(url.toString()));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendRichMediaMessage(String recipientId, UrlRichMediaAsset richMediaAsset) throws MessengerApiException, MessengerIOException {
        final RichMediaMessage richMediaMessage = RichMediaMessage.create(richMediaAsset);
        final MessagePayload messagePayload = MessagePayload.create(recipientId, MessagingType.RESPONSE, richMediaMessage);
        this.messenger.send(messagePayload);
    }

	public void receivedPostBackMessage(PostbackEvent event) throws MessengerApiException, MessengerIOException {
		System.out.println("receivedPostBackMessage");
		String text = event.payload().get().toString();
    	if(text.equalsIgnoreCase("GET_START")==true)
    		pairService.recivedMatchReq(event.senderId());
    	else if(text.equalsIgnoreCase("ABOUT_US")==true)
    		sendTextMessage(event.senderId(),". . .");
		
	}
}
