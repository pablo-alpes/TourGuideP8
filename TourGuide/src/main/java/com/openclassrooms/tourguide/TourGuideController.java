package com.openclassrooms.tourguide;

import java.util.List;

import com.openclassrooms.tourguide.DTO.JsonReponse;
import gpsUtil.GpsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @Autowired
    GpsUtil gpsUtil;

    @Autowired
    JsonReponse jsonReponse;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) throws Exception {
    	return tourGuideService.getUserLocation(getUser(userName));
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
    @RequestMapping("/getNearbyAttractions") 
    public String getNearbyAttractions(@RequestParam String userName) throws Exception {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        User user = tourGuideService.getUser(userName);
        List<Attraction> allAttractions = gpsUtil.getAttractions();
        return jsonReponse.replyJson(tourGuideService.getNearByAttractions(visitedLocation, allAttractions, user));
    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }

    @GetMapping("/getUser")
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}