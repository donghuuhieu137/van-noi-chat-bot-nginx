package webhook.service.database;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.userprofile.UserProfile;

import webhook.entity.User;
import webhook.repository.UserRepo;

@Service
public class UserService {

	@PersistenceContext protected EntityManager entityManager;
	
	@Autowired
	private UserRepo userRepo;
	
	private Messenger messenger;
	
	public void newUser(String id) throws MessengerApiException, MessengerIOException {
		Integer userId = Integer.parseInt(id);
		UserProfile userProfile = this.messenger.queryUserProfile(id);
		User user = new User(userId, userProfile.firstName(), userProfile.lastName(), userProfile.gender().toString(), userProfile.locale(), userProfile.profilePicture());
		user.setStatus("FREE");
		userRepo.save(user);
	}
	
	public User findUser(String id) {
		Integer userId = Integer.parseInt(id);
		if(userRepo.findById(userId).get()!=null) {
			User user = userRepo.findById(userId).get();
			return user;
		}
		return null;
	}

	public List<User> findPartner(String partnerGender) {
		String sql = "SELECT * FROM vannoichatbot.tbl_user WHERE status = 'FINDING' and partner_gender != " + partnerGender + ";";
		Query query = entityManager.createNativeQuery(sql, User.class);
		return query.getResultList();
	}

}
