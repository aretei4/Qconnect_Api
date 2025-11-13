package com.api.distr.docs.sales.dto;


import java.time.LocalDate;

public class DeliveryAgent{
    private Long id;
    private String name;
    private String contact;
    private LocalDate updatedDate;
    private Boolean active;
    private Integer buId;

    

    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getContact() {
		return contact;
	}
	public void setContact(String contact) {
		this.contact = contact;
	}
	public LocalDate getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDate updatedDate) { this.updatedDate = updatedDate; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getBuId() { return buId; }
    public void setBuId(Integer buId) { this.buId = buId; }
}
