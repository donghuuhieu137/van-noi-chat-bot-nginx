package webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import webhook.entity.Report;

public interface ReportRepo extends JpaRepository<Report, Integer>{

}
