package com.openclassrooms.tourguide;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.openclassrooms.tourguide.DTO.JsonReponse;
import com.openclassrooms.tourguide.user.UserExtraInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.beans.factory.annotation.Autowired;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import tripPricer.Provider;

import static org.junit.jupiter.api.Assertions.*;

public class TestTourGuideService {

	@Test
	public void getUserLocation() throws Exception {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);
		tourGuideService.tracker.stopTracking();
		assertTrue(visitedLocation.userId.equals(user.getUserId()));
	}

	@Test
	public void addUser() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);

		User retrivedUser = tourGuideService.getUser(user.getUserName());
		User retrivedUser2 = tourGuideService.getUser(user2.getUserName());

		tourGuideService.tracker.stopTracking();

		assertEquals(user, retrivedUser);
		assertEquals(user2, retrivedUser2);
	}

	@Test
	public void getAllUsers() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);

		List<User> allUsers = tourGuideService.getAllUsers();

		tourGuideService.tracker.stopTracking();

		assertTrue(allUsers.contains(user));
		assertTrue(allUsers.contains(user2));
	}

	@Test
	public void trackUser() throws Exception {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

		tourGuideService.tracker.stopTracking();

		assertEquals(user.getUserId(), visitedLocation.userId);
	}

	/**
	 * Test is deprecated since is substituted by the new method closest 5 locations
	@Disabled
	@Test
	public void getNearbyAttractions() throws Exception {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		List<Attraction> allAttractions = gpsUtil.getAttractions();

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

		List<Attraction> attractions = tourGuideService.getNearByAttractions(visitedLocation, allAttractions, true).keySet().stream().toList();

		tourGuideService.tracker.stopTracking();

		assertEquals(5, attractions.size());
	}
	*/

	@Test
	@DisplayName("Sends the top 5 closest destinations to the user based on its last location (new feature)")
	public void Top5ClosestDestinations() throws Exception {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);
		List<Attraction> allAttractions = gpsUtil.getAttractions();
		tourGuideService.tracker.stopTracking();

		int top5 = tourGuideService.getNearByAttractions(visitedLocation, allAttractions, true).size();

		assertEquals(5, top5);
	}

	@Test
	@DisplayName("Tests whether the reply is in json or not for one user")
	public void jsonOutputForTop5Closest() throws Exception {
		JsonReponse jsonReponse = new JsonReponse();

		//ARRANGE for the attractions MAP reply
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		List<Attraction> allAttractions = gpsUtil.getAttractions();

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);
		Map<Attraction, UserExtraInfo> attractions = tourGuideService.getNearByAttractions(visitedLocation, allAttractions, user);

		//ACT
		String json = jsonReponse.replyJson(attractions);

		//ASSERT
        assertFalse(json.isEmpty());
	}

	@Test
	@DisplayName("Fixes trips deals to deliver 10 max options to the user")
	public void getTripDeals() {

		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		List<Provider> providers = tourGuideService.getTripDeals(user);

		tourGuideService.tracker.stopTracking();

		assertEquals(10, providers.size());
	}

	@Test
	@DisplayName("Adds 100 random users to the system")
	public void addMockUsers() {
		InternalTestHelper.setInternalUserNumber(100);
		Random random = new Random();
		int randuser = random.nextInt(0,99);

		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		tourGuideService.initializeInternalUsers();

		List<User> allUsers = tourGuideService.getAllUsers();
		assertFalse(allUsers.isEmpty());
		assertFalse(tourGuideService.getAllUsers().get(randuser).getUserName().isEmpty());
	}


}
