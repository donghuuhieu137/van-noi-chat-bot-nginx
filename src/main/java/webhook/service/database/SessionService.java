package webhook.service.database;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import webhook.entity.Session;
import webhook.entity.User;
import webhook.repository.SessionRepo;

@Service
public class SessionService {
	
	@PersistenceContext protected EntityManager entityManager;
	
	@Autowired
	private SessionRepo sessionRepo;
	
	@Autowired
	private UserService userService;
	
	public void addSession(Integer l_partner, Integer r_partner) {
		Session session = new Session();
		session.setL_partner(l_partner);
		session.setR_partner(r_partner);
		session.setCreatedDate(LocalDateTime.now());
		sessionRepo.save(session);
	}

	public Integer findPartner(Integer id) {
		List<Session> sessions = findUserSession(id);
		if(sessions.get(0).getL_partner() == id)
			return sessions.get(0).getR_partner();
		return sessions.get(0).getL_partner();
	}
	
	public List<Session> findUserSession(Integer id) {
		String strId = id.toString();
		String sql = "SELECT * FROM vannoichatbot.tbl_session WHERE l_partner = "+ strId +" or l_partner = "+ strId +";";
		Query query = entityManager.createNativeQuery(sql, User.class);
		return query.getResultList();
	}
	
	public void deleteSession(Session session) {
		sessionRepo.delete(session);
	}
}
