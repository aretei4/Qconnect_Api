package com.api.distr.docs.sales.dto;

import java.time.LocalDate;

public class DeliveryAgent {

    // ── Core ──────────────────────────────────────────────────────────────────
    private Long    id;
    private String  name;
    private String  contact;
    private String  altContact;
    private Boolean active;
    private Integer buId;
    private LocalDate updatedDate;

    // ── Address ───────────────────────────────────────────────────────────────
    private String  address1;
    private String  address2;
    private String  address3;
    private String  city;
    private String  pinCode;

    // ── Identity & Banking ────────────────────────────────────────────────────
    private String  fatherName;
    private String  aadharNo;
    private String  panCard;
    private String  bankAccount;
    private LocalDate dateOfJoining;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long    getId()                           { return id; }
    public void    setId(Long id)                    { this.id = id; }

    public String  getName()                         { return name; }
    public void    setName(String name)              { this.name = name; }

    public String  getContact()                      { return contact; }
    public void    setContact(String contact)        { this.contact = contact; }

    public String  getAltContact()                   { return altContact; }
    public void    setAltContact(String altContact)  { this.altContact = altContact; }

    public Boolean getActive()                       { return active; }
    public void    setActive(Boolean active)         { this.active = active; }

    public Integer getBuId()                         { return buId; }
    public void    setBuId(Integer buId)             { this.buId = buId; }

    public LocalDate getUpdatedDate()                      { return updatedDate; }
    public void      setUpdatedDate(LocalDate updatedDate) { this.updatedDate = updatedDate; }

    public String  getAddress1()                     { return address1; }
    public void    setAddress1(String address1)      { this.address1 = address1; }

    public String  getAddress2()                     { return address2; }
    public void    setAddress2(String address2)      { this.address2 = address2; }

    public String  getAddress3()                     { return address3; }
    public void    setAddress3(String address3)      { this.address3 = address3; }

    public String  getCity()                         { return city; }
    public void    setCity(String city)              { this.city = city; }

    public String  getPinCode()                      { return pinCode; }
    public void    setPinCode(String pinCode)        { this.pinCode = pinCode; }

    public String  getFatherName()                   { return fatherName; }
    public void    setFatherName(String fatherName)  { this.fatherName = fatherName; }

    public String  getAadharNo()                     { return aadharNo; }
    public void    setAadharNo(String aadharNo)      { this.aadharNo = aadharNo; }

    public String  getPanCard()                      { return panCard; }
    public void    setPanCard(String panCard)        { this.panCard = panCard; }

    public String  getBankAccount()                  { return bankAccount; }
    public void    setBankAccount(String bankAccount){ this.bankAccount = bankAccount; }

    public LocalDate getDateOfJoining()                        { return dateOfJoining; }
    public void      setDateOfJoining(LocalDate dateOfJoining) { this.dateOfJoining = dateOfJoining; }
}
