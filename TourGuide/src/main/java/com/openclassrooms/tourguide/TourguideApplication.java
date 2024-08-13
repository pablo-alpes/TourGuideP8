package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.DTO.InitUsers;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class TourguideApplication {

	public static void main(String[] args) {
		SpringApplication.run(TourguideApplication.class, args);
		InitUsers.initializeMockUsers();
	}

}
