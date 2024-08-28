package com.openclassrooms.tourguide;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import com.openclassrooms.tourguide.DTO.JsonReponse;
import com.openclassrooms.tourguide.service.RewardsService;
import gpsUtil.GpsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import rewardCentral.RewardCentral;
import tripPricer.Provider;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@RestController
public class TourGuideController {

    private final GpsUtil gpsUtil = new GpsUtil();

    private final RewardCentral rewardsCentral = new RewardCentral();

    @Autowired
    private RewardsService rewardsService;

    @Autowired
    private TourGuideService tourGuideService;

    @Autowired
    JsonReponse jsonReponse;

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping("/getLocation")
    public VisitedLocation getLocation(@RequestParam String userName) throws Exception { //OK
    	return (VisitedLocation) tourGuideService.getUserLocation(getUser(userName));
    }

    //  DONE: Change this method to no longer return a List of Attractions.
 	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are. -- DONE
 	//  Return a new JSON object that contains: -- All data is contained, needs to wrap into a JSON now
    	// Name of Tourist attraction,
        // Tourist attractions lat/long,
        // The user's location lat/long,
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral
    //Needs optimization -- 1000 ms
    @RequestMapping("/getNearbyAttractions")
    public CompletableFuture<String> getNearbyAttractions(@RequestParam String userName) throws Exception {
        final User user = tourGuideService.getUser(userName);
        final VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);

        //Optimization of the call to GpsUtil
        //final List<Attraction> allAttractions = new ArrayList<>();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return jsonReponse.replyJson(tourGuideService.getNearByAttractions(visitedLocation, gpsUtil.getAttractions(), user));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());

    }

    //Optimized - Needs add real data to check perf
    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }

    //Optimized
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }

    @RequestMapping("/getUser")
    private User getUser(@RequestParam String userName) {
    	return tourGuideService.getUser(userName);
    }


}