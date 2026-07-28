package com.msc.memories.dto;

import java.util.List;

public class UserResponseDto {
    private String email;
    private String name;
    private String registrationNumber;
    private String phoneNumber;
    private boolean enabled;
    private List<ImageDto> images;
    
    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRegistrationNumber() {
		return registrationNumber;
	}
	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public List<ImageDto> getImages() {
		return images;
	}
	public void setImages(List<ImageDto> images) {
		this.images = images;
	}
    
    // Getters and Setters
}