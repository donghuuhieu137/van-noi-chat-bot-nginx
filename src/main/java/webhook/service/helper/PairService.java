package webhook.service.helper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.jws.soap.SOAPBinding.Use;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;

import webhook.controller.CallBackHandle;
import webhook.entity.Log;
import webhook.entity.Session;
import webhook.entity.User;
import webhook.repository.LogRepo;
import webhook.repository.UserRepo;
import webhook.service.WebhookService;
import webhook.service.database.SessionService;
import webhook.service.database.UserService;

@Service
public class PairService {

	@Autowired
	private LogRepo logRepo;
	
	@Autowired
	private SessionService sessionService;
	
	@Autowired
	private WebhookService webhookService;
	
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private Messenger messenger;
	
	public void recivedMatchReq(String id) throws MessengerApiException, MessengerIOException {
		System.out.println("recivedMatchReq");
		String statusUser = userService.findUser(id).get(0).getStatus();
		System.out.println(statusUser);
		switch(statusUser) {
			case "MATCHED":
				webhookService.sendTextMessage(id, "Bạn đã được ghép đôi\nVui lòng hủy ghép đôi trước khi tìm đối mới");
				break;
			case "FINDING":
				webhookService.sendTextMessage(id, "Bot hiện vẫn đang tìm đối cho bạn");
				break;
			case "FREE":
				sendChooseMessage(id);
				break;
			default:
				System.out.println(statusUser+" is invalid");
		}
	}
	
	private void sendChooseMessage(String recipientId) throws MessengerApiException, MessengerIOException {
		System.out.println("sendChooseMessage");
        List<QuickReply> quickReplies = new ArrayList<>();

        quickReplies.add(TextQuickReply.create("Nam", "male"));
        quickReplies.add(TextQuickReply.create("Nữ", "female"));

        TextMessage message = TextMessage.create("Bạn muốn tìm kiếm đối phương nam hay nữ ?", Optional.of(quickReplies), Optional.empty());
        this.messenger.send(MessagePayload.create(recipientId, MessagingType.RESPONSE, message));
    }

	public void matchUser(QuickReplyMessageEvent event) {
		System.out.println("matchUser");
		String partnerGender = event.payload().toString();
		User user = userService.findUser(event.senderId()).get(0);
		user.setPartnerGender(partnerGender);
		List<User> listUser = userService.findPartner(partnerGender);
		if(listUser.isEmpty()) {
			user.setStatus("FINDING");
			userRepo.save(user);
			System.out.println("isEmpty");
		}
		else {
			System.out.println("MATCHED");
			sessionService.addSession(user.getId(),listUser.get(0).getId());
			String partnerId = listUser.get(0).getId().toString();
			User partner = userService.findUser(partnerId).get(0);
			user.setStatus("MATCHED");
			partner.setStatus("MATCHED");
			userRepo.save(user);
			userRepo.save(partner);
			webhookService.sendTextMessage(partnerId,"Matched\nGhép cặp thành công !!");
			webhookService.sendTextMessage(event.senderId(),"Matched\nGhép cặp thành công !!");
		}
	}

	public void recivedEndReq(String senderId) {
		System.out.println("recivedEndReq");
		User user = userService.findUser(senderId).get(0);
		String partnerId = sessionService.findPartner(senderId);
		if(partnerId == null)
			webhookService.sendTextMessage(senderId, "Bạn chưa được kết đôi");
		else {
			User partner = userService.findUser(partnerId).get(0);
			user.setStatus("FREE");
			partner.setStatus("FREE");
			userRepo.save(user);
			userRepo.save(partner);
			Session session = sessionService.findUserSession(senderId).get(0);
			Log log = new Log(session.getL_partner(), session.getR_partner(), session.getCreatedDate(),LocalDateTime.now());
			logRepo.save(log);
			sessionService.deleteSession(session);
		}
	}
}
