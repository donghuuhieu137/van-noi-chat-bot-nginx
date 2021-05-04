package webhook.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_session")
public class Session extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Integer id;
	
	@Column(name = "l_partner", nullable = false)
	private String l_partner;
	
	@JoinColumn(name = "r_partner", nullable = false)
	private String r_partner;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	
	
	
}
