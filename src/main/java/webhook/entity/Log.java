package webhook.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "tbl_log")
public class Log {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Integer id;
	
	@Column(name = "l_partner", nullable = false)
	private String l_partner;
	
	@Column(name = "r_partner", nullable = false)
	private String r_partner;
	
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	@Column(name = "paired_date", nullable = true)
	private LocalDateTime pairedDate = null;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	@Column(name = "unpaired_date", nullable = true)
	private LocalDateTime unpairedDate = null;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LocalDateTime getPairedDate() {
		return pairedDate;
	}

	public void setPairedDate(LocalDateTime pairedDate) {
		this.pairedDate = pairedDate;
	}

	public LocalDateTime getUnpairedDate() {
		return unpairedDate;
	}

	public void setUnpairedDate(LocalDateTime unpairedDate) {
		this.unpairedDate = unpairedDate;
	}

	public String getL_partner() {
		return l_partner;
	}

	public void setL_partner(String l_partner) {
		this.l_partner = l_partner;
	}

	public String getR_partner() {
		return r_partner;
	}

	public void setR_partner(String r_partner) {
		this.r_partner = r_partner;
	}

	public Log(String l_partner, String r_partner, LocalDateTime pairedDate, LocalDateTime unpairedDate) {
		super();
		this.l_partner = l_partner;
		this.r_partner = r_partner;
		this.pairedDate = pairedDate;
		this.unpairedDate = unpairedDate;
	}

	

}
