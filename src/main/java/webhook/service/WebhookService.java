package webhook.service;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

@Service
public class WebhookService {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private SessionService sessionService;
	
	@Autowired
	private Messenger messenger;
	
	@Autowired
	private PairService pairService;
	
	public void receivedTextMessage(TextMessageEvent event) throws MessengerApiException, MessengerIOException {
		String text = event.text();
		System.out.println("receivedTextMessage");
		if(userService.findUser(event.senderId()).get(0).getGender()==null && !text.equalsIgnoreCase("/setting")) {
    		System.out.println("null gender");
    		sendTextMessage(event.senderId(), "Để tiếp tục sử dụng Chatbot hãy cho bot biết giới tính của bạn bằng cách chat /setting");
    	}
		else {
			switch(text.toLowerCase()) {
			case "/find":
					pairService.recivedMatchReq(event.senderId());
				break;
			case "/stop":
					pairService.recivedStopReq(event.senderId());
				break;
			case "/end":
					pairService.recivedEndReq(event.senderId());
				break;
			case "/report":
					pairService.recivedReportReq(event.senderId());
				break;
			case "/setting":
				userService.sendSettingGender(event.senderId());
			break;
			case "/hd":
					sendTextMessage(event.senderId(), "Các câu lệnh:\n\n/find: Bot sẽ kết nối bạn với người lạ.\n\n/stop: Bot sẽ dừng tìm kiếm.\n\n/end: Bot sẽ kết thúc cuộc trò chuyện của bạn với người lạ.\n\n/setting: Cập nhật giới tính của bạn.\n\n/report: Tố cáo hành vi của đối phương.");
				break;
			default:
				System.out.println("Send text to partner");
				User user = userService.findUser(event.senderId()).get(0);
				if(user.getStatus().equalsIgnoreCase("MATCHED")) {
					String partnerId = sessionService.findPartner(user.getId());
					sendTextMessage(partnerId, text);
				}
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

	public void receivedQuickReplyMessage(QuickReplyMessageEvent event) throws MessengerApiException, MessengerIOException {
		System.out.println("receivedQuickReplyMessage");
		String text = event.payload().toString().toLowerCase();
		switch (text) {
		case "choosemale":
			sendTextMessage(event.senderId(),"Đang tìm đối phương nam . . .");
			pairService.matchUser(event);
			break;
		case "choosefemale":
			sendTextMessage(event.senderId(),"Đang tìm đối phương nữ . . .");
			pairService.matchUser(event);
			break;
		case "yes":
			pairService.sendChooseReport(event.senderId());
			break;
		case "no":
			sendTextMessage(event.senderId(), "Bot đã hủy thao tác tố cáo");
			break;
		case "toxic":
		case "clone":
		case "fakegender":
			pairService.confirmReportReq(event.senderId(),text);
			break;
		case "settingmale":
		case "settingfemale":
			userService.settingGender(event.senderId(),text);
			break;
		default:
			System.out.println("invalid quickreply");
			break;
		}
	}

	public void receivedAttachmentMessage(AttachmentMessageEvent event) throws MalformedURLException, MessengerApiException, MessengerIOException {
		if(userService.findUser(event.senderId()).get(0).getGender()==null) {
    		System.out.println("null gender");
    		sendTextMessage(event.senderId(), "Để tiếp tục sử dụng Chatbot hãy cho bot biết giới tính của bạn bằng cách chat /setting");
    	}
		else
			for (Attachment attachment : event.attachments()) {
				if(attachment.isRichMediaAttachment()) {
					final RichMediaAttachment richMediaAttachment = attachment.asRichMediaAttachment();
					final RichMediaAttachment.Type type = richMediaAttachment.type();
					final URL url = richMediaAttachment.url();
					System.out.println("Send text to partner");
					User user = userService.findUser(event.senderId()).get(0);
					final String partnerId = sessionService.findPartner(user.getId());
					if(type.toString() == "IMAGE")
						sendMediaMessage(partnerId, Type.IMAGE , url);
					else if(type.toString() == "VIDEO")
						sendMediaMessage(partnerId, Type.VIDEO , url);
					else if(type.toString() == "FILE")
						sendMediaMessage(partnerId, Type.FILE , url);
					else if(type.toString() == "AUDIO")
						sendMediaMessage(partnerId, Type.AUDIO , url);
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
