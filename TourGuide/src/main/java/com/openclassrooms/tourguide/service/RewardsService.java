package com.openclassrooms.tourguide.service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {


    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;


    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    private final Executor executor = Executors.newScheduledThreadPool(100);
    //https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executors.html#newFixedThreadPool-int-

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    public CompletableFuture<Void> calculateRewards(User user) {
        final List<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());

        // Fetch attractions asynchronously since gpsutil is a bottelneck
        CompletableFuture<List<Attraction>> attractionsFuture = CompletableFuture.supplyAsync(
                () -> gpsUtil.getAttractions(), Executors.newVirtualThreadPerTaskExecutor()); //virtuals to enhance performance for i/o transactions

        // Composing with rewards once attractions are fetched
        return attractionsFuture.thenCompose(attractions -> {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (VisitedLocation visitedLocation : userLocations) { //we keep loops as there's loss of performance with parallel streams in the thread management
                for (Attraction attraction : attractions) {
                    if (nearAttraction(visitedLocation, attraction)) {
                        // Submit each reward calculation as a separate task in a virtual thread
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            int rewardPoints = getRewardPoints(attraction, user);
                            user.addUserReward(new UserReward(visitedLocation, attraction, rewardPoints));
                        }, Executors.newVirtualThreadPerTaskExecutor());
                        futures.add(future);
                    }
                }
            }
            // We wait for all futures to be done and merging in it into a CompletableFuture
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        });
    }

    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
    }

}
