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
import java.util.concurrent.atomic.AtomicInteger;
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
    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

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

    public VisitedLocation getUserLocation(User user) {
        Object visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
                :  trackUserLocation(user);
        return (VisitedLocation) visitedLocation;
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
        return providers.subList(0, 10); //gets the first 10, no order necesarily done
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        //snippet obtain from https://howtodoinjava.com/java8/java-stream-distinct-examples/
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * It gathers the user location. It applies then compose and then apply to ensure it awaits for results of calcualte rewards
     * @param user
     * @return visited location
     * //technical doc of different approaches :
     * from https://krishaniindrachapa.medium.com/parallel-processing-for-optimisation-in-java-8f68077d3605
     */

    public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
        //System.out.println("Tracking user location for user: " + user.getUserId());
        return CompletableFuture.supplyAsync(() -> {
            VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
            user.addToVisitedLocations(visitedLocation);
            return visitedLocation;
        }, executorService).thenCompose(visitedLocation -> {
          //  System.out.println("Calculating rewards after tracking location.");
            return rewardsService.calculateRewards(user).thenApply(v -> {
               // System.out.println("Completed reward calculation.");
                return visitedLocation;
            });
        });
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
        List<Double> distances = allAttractions.parallelStream()
                .map(attraction -> rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude), visitedLocation.location))
                .toList();

        Map<Attraction, Double> consolidated = new HashMap<>();

        for (int i = 0; i < allAttractions.size(); i++) {
            consolidated.put(allAttractions.get(i), distances.get(i));
        }

        return consolidated.entrySet()
                .parallelStream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    /**
     * This method sents the whole information for the json including the new values required (
     *
     * @param visitedLocation
     * @param allAttractions
     * @param user
     * @return Map with all the attractions, reward points, closest ones
     */
    public Map<Attraction, UserExtraInfo> getNearByAttractions(VisitedLocation visitedLocation, List<Attraction> allAttractions, User user) throws NullPointerException {
        List<Double> distances = allAttractions.parallelStream()
                .map(attraction -> rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude), visitedLocation.location))
                .toList();

        Map<Attraction, UserExtraInfo> consolidated = new HashMap<>();
        //for (int i = 0; i < distances.size(); i++) {
        AtomicInteger counter = new AtomicInteger(0); //done for passing the right location - solution: https://gist.github.com/Makesh/a1defe6f1e2692aaa196
        allAttractions.parallelStream().forEach(attraction -> { //refactored for performance optimization
            double userLongitude = user.getLastVisitedLocation().location.latitude;
            double userLatitude = user.getLastVisitedLocation().location.longitude;
            int reward = rewardsService.getRewardPoints(attraction, user);

            consolidated.put(attraction, new UserExtraInfo(
                    distances.get(counter.getAndIncrement()),
                    reward,
                    userLongitude,
                    userLatitude
            ));
        });

        return consolidated.entrySet()
                .parallelStream()
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

    public void initializeInternalUsers() {
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

    /**private void generateUserLocationHistory(User user) {
     IntStream.range(0, 3).forEach(i -> {
     user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
     new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
     });
     }
     */

    /**
     * Max number locations per user
     * Method refined to assign real locations and not randomwise
     *
     * @param user
     */
    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            //    try {
            //        user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
            //               new Location(generateAttractionLocation().latitude,generateAttractionLocation().longitude), getRandomTime()));
            //                   } catch (Exception e) {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                    new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
            //    }
        });
    }

    //This code is in case to generale real attraction instead of random
    //However performance is hugely impacted
    //private Attraction generateAttractionLocation() {
    //  Random rand = new Random();
    //  List<Attraction> allAttractions = new CopyOnWriteArrayList<>(gpsUtil.getAttractions());
    //  int attractionRand = rand.nextInt(0, allAttractions.size());
    //  return allAttractions.get(attractionRand);


    //user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
    // TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
    // tourGuideService.trackUserLocation(user);
    //}

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

    public List<Attraction> getAllAttractions() {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CompletableFuture<List<Attraction>> attractionsList = CompletableFuture.supplyAsync(() -> gpsUtil.getAttractions(), executor);
        return attractionsList.join();
    }
}
