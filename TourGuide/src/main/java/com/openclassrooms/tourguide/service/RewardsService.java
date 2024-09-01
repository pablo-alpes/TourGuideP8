package com.openclassrooms.tourguide.service;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

@Service
public class RewardsService {


    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;


    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    private final ExecutorService virtualPool = newVirtualThreadPerTaskExecutor();
    //https://openjdk.org/jeps/444: Virtual threads are lightweight threads that dramatically
    // reduce the effort of writing, maintaining, and observing high-throughput concurrent applications.

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

    /**
     * Perfoms the calculation of rewards based on business logic (locations to be rewarded shall be attractions) and not repeated
     * It uses virtual threads to enhance performance and it does that by combining steps since not all steps can be asynchrous for
     * the process
     * We kept the prints for debugging
     *
     * @param user
     * @return
     */
    public CompletableFuture<Void> calculateRewards(User user) {
        //System.out.println("Starting reward calculation");
        final List<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());

        CompletableFuture<List<Attraction>> attractionsFuture = CompletableFuture.supplyAsync(gpsUtil::getAttractions, virtualPool);
        CompletableFuture<VisitedLocation> locationFuture = CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()), virtualPool);

        return attractionsFuture.thenCombine(locationFuture, (attractions, location) -> {
            //System.out.println("Processing attractions and location");

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (VisitedLocation visitedLocation : userLocations) {
                for (Attraction attraction : attractions) {

                    //System.out.println("Processing attraction: " + attraction);

                    CompletableFuture<Void> future = getRewardPointsAsync(attraction, user).thenAccept(rewardPoints -> {
                        try {
                            if (isLocationMatch(visitedLocation, attraction)) { // Business rule: checks if visited location is an attraction
                                //TODO -- Do we need also to add back the nearlocation??
                                //System.out.println("Calculating reward for: " + attraction);
                                //System.out.println("rewardPoints: " + rewardPoints);
                                UserReward userReward = new UserReward(visitedLocation, attraction, rewardPoints);
                                user.addUserReward(userReward);
                            }
                        } catch (Exception e) {
                            //System.err.println("Error adding user reward: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });

                    futures.add(future);
                }
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }).thenRun(() -> {
            //System.out.println("Completed reward calculation.");
        });
    }

    //TODO-- Do we need to apply this to the calculate reward or not?
    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    private boolean isLocationMatch(VisitedLocation visitedLocation, Attraction attraction) {
        double tolerance = 0.0001;
        // Define a small tolerance floating-point precision issues

        boolean latMatch = Math.abs((visitedLocation.location.latitude - attraction.latitude)) < tolerance;
        boolean lonMatch = Math.abs(visitedLocation.location.longitude - attraction.longitude) < tolerance;

        return latMatch && lonMatch;
    }

    //deprecated
    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }


    public CompletableFuture<Integer> getRewardPointsAsync(Attraction attraction, User user) {
        //for use in the calculate rewards
        return CompletableFuture.supplyAsync(() -> {
            try {
                return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
            } catch (Exception e) {
                return 0;
            }
        }, virtualPool);
    }


    /**
     * public CompletableFuture<Integer> attemptFetchRewardPoints(Attraction attraction, User user, int remainingRetries) {
     * <p>
     * final long INITIAL_RETRY_DELAY_MS = 10; // Initial delay in milliseconds
     * final long MAX_RETRY_DELAY_MS = 500;    // Maximum delay in milliseconds
     * final int MAX_RETRIES = 6;               // Maximum number of retries
     * <p>
     * if (remainingRetries <= 0) {
     * // Base case: No retries left
     * return CompletableFuture.completedFuture(0);
     * }
     * <p>
     * return CompletableFuture.supplyAsync(() -> {
     * try {
     * return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
     * } catch (Exception e) {
     * System.err.println("Error fetching reward points for " + attraction.attractionName + ": " + e.getMessage());
     * e.printStackTrace();
     * return 0;
     * }
     * }, virtualPool).handle((result, ex) -> {
     * if (ex != null || result == 0) {
     * // If there was an exception or result is 0, retry
     * long delay = Math.min(INITIAL_RETRY_DELAY_MS * (1 << (MAX_RETRIES - remainingRetries)), MAX_RETRY_DELAY_MS);
     * //System.out.println("Retrying fetch reward points for " + attraction.attractionName + " after " + delay + " ms delay");
     * <p>
     * return CompletableFuture.supplyAsync(() -> {
     * try {
     * Thread.sleep(delay); // Sleep for the delay
     * } catch (InterruptedException interruptedException) {
     * Thread.currentThread().interrupt();
     * }
     * return attemptFetchRewardPoints(attraction, user, remainingRetries - 1).join();
     * }, virtualPool).join();
     * } else {
     * return result;
     * }
     * });
     * }
     */


    //deprecated
    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
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
