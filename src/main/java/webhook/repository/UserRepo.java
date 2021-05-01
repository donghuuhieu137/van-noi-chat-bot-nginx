package webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import webhook.entity.User;

public interface UserRepo extends JpaRepository<User, Integer>{
	
}
