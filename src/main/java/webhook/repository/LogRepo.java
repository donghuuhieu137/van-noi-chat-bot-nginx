package webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import webhook.entity.Log;

public interface LogRepo extends JpaRepository<Log, Integer>{

}
