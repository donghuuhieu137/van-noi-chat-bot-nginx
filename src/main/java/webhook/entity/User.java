package webhook.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "tbl_user")
public class User extends BaseEntity{
	@Id
	@Column(name = "id", nullable = false)
	private String id;
	
	@Column(name = "first_name", length = 60, nullable = false)
	private String first_name;
	
	@Column(name = "last_name", length = 60, nullable = false)
	private String last_name;
	
	@Column(name = "gender", length = 10, nullable = true)
	private String gender = null;
	
	@Column(name = "partner_gender", length = 10, nullable = true)
	private String partnerGender = null;
	
	@Column(name = "locale", length = 60, nullable = true)
	private String locale = null;
	
	@Column(name = "profile_pic", length = 200, nullable = true)
	private String profile_pic = null;

	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirst_name() {
		return first_name;
	}

	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}

	public String getLast_name() {
		return last_name;
	}

	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getProfile_pic() {
		return profile_pic;
	}

	public void setProfile_pic(String profile_pic) {
		this.profile_pic = profile_pic;
	}

	public String getPartnerGender() {
		return partnerGender;
	}

	public void setPartnerGender(String partnerGender) {
		this.partnerGender = partnerGender;
	}

	public User(String id, String first_name, String last_name, String gender, String partnerGender, String locale,
			String profile_pic) {
		super();
		this.id = id;
		this.first_name = first_name;
		this.last_name = last_name;
		this.gender = gender;
		this.partnerGender = partnerGender;
		this.locale = locale;
		this.profile_pic = profile_pic;
	}

	public User() {
		super();
	}
	
	
}
