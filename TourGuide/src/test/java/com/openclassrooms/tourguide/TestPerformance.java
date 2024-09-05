package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.openclassrooms.tourguide.DTO.JsonReponse;
import com.openclassrooms.tourguide.user.UserExtraInfo;
import org.apache.commons.lang3.time.StopWatch;
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

public class TestPerformance {

    /*
     * A note on performance improvements:
     *
     * The number of users generated for the high volume tests can be easily
     * adjusted via this method:
     *
     * InternalTestHelper.setInternalUserNumber(100000);
     *
     *
     * These tests can be modified to suit new solutions, just as long as the
     * performance metrics at the end of the tests remains consistent.
     *
     * These are performance metrics that we are trying to hit:
     *
     * highVolumeTrackLocation: 100,000 users within 15 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     * highVolumeGetRewards: 100,000 users within 20 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     */

    @Disabled
    @Test
    public void highVolumeTrackLocation() throws Exception {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        // Users should be incremented up to 100,000, and test finishes within 15
        // minutes
        InternalTestHelper.setInternalUserNumber(50000);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        List<User> allUsers = new ArrayList<>(tourGuideService.getAllUsers());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<CompletableFuture<VisitedLocation>> futureRewards = allUsers.stream()
                .map(tourGuideService::trackUserLocation)
                .toList();

        CompletableFuture.allOf(futureRewards.toArray(new CompletableFuture[0])).join();

        //ACT
        //The test is simplified as solution will get the whole list instead to run all futures in parallel
        for (User user : allUsers) {
            tourGuideService.trackUserLocation(user);
        }

        //ASSERT
        stopWatch.stop();
        tourGuideService.tracker.stopTracking();

        System.out.println("highVolumeTrackLocation: Time Elapsed: "
                + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
    }

    @Disabled
    @Test
    public void highVolumeGetRewards() throws InterruptedException {

        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

        // Users should be incremented up to 100,000, and test finishes within 20
        // minutes
        InternalTestHelper.setInternalUserNumber(50000);
        StopWatch stopWatch = new StopWatch();

        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        Attraction attraction = gpsUtil.getAttractions().get(0);
        List<User> allUsers = tourGuideService.getAllUsers();
        stopWatch.start();
        allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

        //technical source: https://www.baeldung.com/java-completablefuture-unit-test
        ////https://github.com/bbejeck/Java-8/blob/master/src/test/java/bbejeck/concurrent/CompletableFutureTest.java
        //until completion of all futures
        CompletableFuture<?>[] completableFutures = allUsers.stream()
                .map(rewardsService::calculateRewards)
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutures).join();

        for (User user : allUsers) {
            assertTrue(!user.getUserRewards().isEmpty());
        }

        stopWatch.stop();
        tourGuideService.tracker.stopTracking();

        System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
                + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
    }

    @Disabled
    @Test
    @DisplayName("Tests response time for nearby request")
    public void nearbySpeed() throws Exception {
        JsonReponse jsonReponse = new JsonReponse();

        //ARRANGE for the attractions MAP reply
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        InternalTestHelper.setInternalUserNumber(100);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<Attraction> allAttractions = new ArrayList<>(gpsUtil.getAttractions());
        //List<Attraction> allAttractions = gpsUtil.getAttractions();

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).get();
        List<UserExtraInfo> attractions = tourGuideService.getNearByAttractions(visitedLocation, allAttractions, user);

        //ACT
        String json = String.valueOf(jsonReponse.replyJson(attractions));

        //ASSERT
        assertFalse(json.isEmpty());
        stopWatch.stop();
        tourGuideService.tracker.stopTracking();

        System.out.println("nearby duration: Time Elapsed: "
                + TimeUnit.MILLISECONDS.toMillis(stopWatch.getTime()) + " ms");
        assertTrue(TimeUnit.SECONDS.toSeconds(2) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));

    }

}
