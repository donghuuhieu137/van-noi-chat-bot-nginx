package webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import webhook.entity.Session;

public interface SessionRepo extends JpaRepository<Session, Integer> {

}
