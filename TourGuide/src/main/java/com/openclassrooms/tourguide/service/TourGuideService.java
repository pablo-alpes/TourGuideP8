package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserExtraInfo;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import rewardCentral.RewardCentral;

import tripPricer.Provider;
import tripPricer.TripPricer;


@Service
public class TourGuideService {
    private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    public final Tracker tracker;
    boolean testMode = true;
    private RewardCentral rewardsCentral;

    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;

        Locale.setDefault(Locale.US);

        if (testMode) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        tracker = new Tracker(this);
        addShutDownHook();
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    public VisitedLocation getUserLocation(User user) throws ExecutionException, InterruptedException {
        VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
                : trackUserLocation(user);
        return visitedLocation;
    }

    public User getUser(String userName) {
        return internalUserMap.get(userName);
    }

    public List<User> getAllUsers() {
        return internalUserMap.values().stream().collect(Collectors.toList());
    }

    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    public List<Provider> getTripDeals(User user) {
        //It suggests trips based on price, even for the same providers if different prices are offered
        //The times to obtain different providers make the tool make very poorly
        int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();

        List<Provider> providers = new ArrayList<>();

        while (true) {
            providers.addAll(
                        tripPricer.getPrice(
                                tripPricerApiKey,
                                user.getUserId(),
                                user.getUserPreferences().getNumberOfAdults(),
                                user.getUserPreferences().getNumberOfChildren(),
                                user.getUserPreferences().getTripDuration(),
                                cumulativeRewardPoints));

            providers = providers.stream()
                       .filter(distinctByKey(provider -> provider.price)) //add to filter by elements of different price
                       .collect(Collectors.toList());

            if (providers.size() > 9) break;

        }
        return providers.subList(0,10); //gets the first 10, no order necesarily done
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        //snippet obtain from https://howtodoinjava.com/java8/java-stream-distinct-examples/
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public VisitedLocation trackUserLocation(User user) throws ExecutionException, InterruptedException {
        VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    //technical doc of different approaches :
    // from https://krishaniindrachapa.medium.com/parallel-processing-for-optimisation-in-java-8f68077d3605
    public List<VisitedLocation> trackUserLocations(List<User> users) throws ExecutionException, InterruptedException {
        List<CompletableFuture<VisitedLocation>> allFutures = new ArrayList<>();

        users.stream().parallel().forEach(user -> {
            CompletableFuture<VisitedLocation> future = CompletableFuture.supplyAsync(() -> {
                VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
                user.addToVisitedLocations(visitedLocation);
                rewardsService.calculateRewards(user);
                return visitedLocation;
            });
            allFutures.add(future);
        });

        CompletableFuture<List<VisitedLocation>> listFuture = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> allFutures.stream().parallel()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
        return listFuture.join();
    }

    /**
     * public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
     * List<Attraction> nearbyAttractions = new ArrayList<>();
     * List<Attraction> allAttractions = new ArrayList<>(gpsUtil.getAttractions());
     * for (Attraction attraction : allAttractions) {
     * if (rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
     * nearbyAttractions.add(attraction);
     * }
     * }
     * <p>
     * return nearbyAttractions;
     * }
     */

    // Original method (kept for backward compatibility)
    public Map<Attraction, Double> getNearByAttractions(VisitedLocation visitedLocation, List<Attraction> allAttractions, boolean flag) {
        List<Double> distances = allAttractions.stream()
                .map(attraction -> rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude), visitedLocation.location))
                .toList();

        Map<Attraction, Double> consolidated = new HashMap<>();

        for (int i = 0; i < allAttractions.size(); i++) {
            consolidated.put(allAttractions.get(i), distances.get(i));
        }

        return consolidated.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    public Map<Attraction, UserExtraInfo> getNearByAttractions(VisitedLocation visitedLocation, List<Attraction> allAttractions, User user) throws NullPointerException {
        List<Double> distances = allAttractions.stream()
                .map(attraction -> rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude), visitedLocation.location))
                .toList();

        Map<Attraction, UserExtraInfo> consolidated = new HashMap<>();
        for (int i = 0; i < distances.size(); i++) {
            int reward;
            double userLongitude = i < allAttractions.size() ? user.getLastVisitedLocation().location.latitude : null;
            double userLatitude = i < allAttractions.size() ? user.getLastVisitedLocation().location.longitude : null;
            try {
                reward = rewardsCentral.getAttractionRewardPoints(allAttractions.get(i).attractionId, visitedLocation.userId);
            }
            catch (Exception e) {
                reward = 0;
            }
            consolidated.put(allAttractions.get(i), new UserExtraInfo(
                                distances.get(i),
                    reward,
                    userLongitude,
                    userLatitude
            ));
        }

        return consolidated.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingDouble(UserExtraInfo::getDistance)))
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                tracker.stopTracking();
            }
        });
    }

    /**********************************************************************************
     *
     * Methods Below: For Internal Testing
     *
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes
// internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
    }

    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                    new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
