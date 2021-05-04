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

import webhook.controller.CallBackHandle;
import webhook.entity.User;
import webhook.repository.UserRepo;

@Service
public class UserService {

	@PersistenceContext protected EntityManager entityManager;
	
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private Messenger messenger;
	
	public void newUser(String id) throws MessengerApiException, MessengerIOException {
		UserProfile userProfile = this.messenger.queryUserProfile(id);
		System.out.println(userProfile.firstName());
		User user = new User(id, userProfile.firstName(), userProfile.lastName(), userProfile.gender().toString(),null,userProfile.locale(), userProfile.profilePicture());
		user.setStatus("FREE");
		userRepo.save(user);
	}
	
	public List<User> findUser(String id) {
		String sql = "SELECT * FROM vannoichatbot.tbl_user WHERE id = "+id+";";
		Query query = entityManager.createNativeQuery(sql, User.class);
		return query.getResultList();
	}

	public List<User> findPartner(String partnerGender) {
		String sql = "SELECT * FROM vannoichatbot.tbl_user WHERE status = 'FINDING' and gender = '" + partnerGender + "';";
		Query query = entityManager.createNativeQuery(sql, User.class);
		return query.getResultList();
	}

}
