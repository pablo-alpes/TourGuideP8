package com.openclassrooms.tourguide;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.openclassrooms.tourguide.DTO.JsonReponse;
import com.openclassrooms.tourguide.user.UserExtraInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import static org.junit.jupiter.api.Assertions.*;

public class TestRewardsService {

	@Test
	public void userGetRewards() throws Exception {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		Attraction attraction = gpsUtil.getAttractions().get(0);
		System.out.println(attraction.attractionName);
		user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));


		// Track user location
		CompletableFuture<VisitedLocation> future = tourGuideService.trackUserLocation(user);
		future.get(); // Ensure the future completes

		// Calculate rewards after tracking location
		CompletableFuture<Void> rewardsFuture = rewardsService.calculateRewards(user);
		rewardsFuture.join(); // Ensure reward calculation completes

		// Now check the results
		List<UserReward> userRewards = user.getUserRewards();

		System.out.println("future is done");
		assertTrue(rewardsFuture.isDone());
		System.out.println("size");
		assertTrue(userRewards.size() > 0); // Assert that at least one reward was added
		System.out.println(userRewards.size());

		tourGuideService.tracker.stopTracking(); // Clean up
	}

	@Test
	public void isWithinAttractionProximity() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		Attraction attraction = gpsUtil.getAttractions().get(0);
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}

	//TODO -- Do we need this after implementing the top 5 nearby attractions??
	@Disabled
	@Test
	public void nearAllAttractions() throws Exception {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		InternalTestHelper.setInternalUserNumber(1);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = tourGuideService.getAllUsers().get(0);
		rewardsService.calculateRewards(user).join(); //needs to wait for the futures to complete
		tourGuideService.getUserLocation(user);

		List<UserReward> userRewards = tourGuideService.getUserRewards(tourGuideService.getAllUsers().get(0));
		tourGuideService.tracker.stopTracking();

		assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
	}

	@Test
	@DisplayName("Sends the top 5 closest destinations to the user based on its last location (new feature)")
	public void Top5ClosestDestinations() throws Exception {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).get();
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
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).get();
		Map<Attraction, UserExtraInfo> attractions = tourGuideService.getNearByAttractions(visitedLocation, allAttractions, user);

		//ACT
		String json = jsonReponse.replyJson(attractions);

		//ASSERT
		assertFalse(json.isEmpty());
	}

	@Test
	public void testAddUserReward() {
		User user = new User(UUID.randomUUID(), "testUser", "000", "test@example.com");
		GpsUtil gpsUtil = new GpsUtil();
		Attraction attraction = gpsUtil.getAttractions().get(0);
		VisitedLocation visitedLocation = new VisitedLocation(user.getUserId(), attraction, new Date());
		user.addToVisitedLocations(visitedLocation);
		UserReward reward = new UserReward(visitedLocation, attraction, 10);

		user.addUserReward(reward);

		List<UserReward> userRewards = user.getUserRewards();
		assertEquals(1, userRewards.size());
		assertTrue(userRewards.contains(reward));
	}


}
